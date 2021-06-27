import json
import os
import numpy as np
from time import perf_counter
import tensorflow as tf
from tensorflow.core.protobuf import rewriter_config_pb2
from model.manager.model_manager import ModelManager
from ..gpt import encoder, model, sample
from ..config import *

class GPTManager(ModelManager):
    def __init__(self, top_k, project, train_len,
                 excode_model_path, java_model_path, method_call_model_path):

        os.environ["KMP_BLOCKTIME"] = "1"
        os.environ["KMP_SETTINGS"] = "1"
        os.environ["KMP_AFFINITY"] = "granularity=fine,verbose,compact,1,0"
        seed = 42

        np.random.seed(seed)
        tf.compat.v1.set_random_seed(seed)

        self.sess = self.start_tf_sess()
        self.encoder = None

        super().__init__(top_k, project, train_len,
                         excode_model_path, java_model_path, method_call_model_path)

    def __del__(self):
        self.reset_session(self.sess)

    def start_tf_sess(self, threads=-1, server=None):
        """
        Returns a tf.Session w/ config
        """
        config = tf.compat.v1.ConfigProto()
        config.gpu_options.allow_growth = True
        config.graph_options.rewrite_options.layout_optimizer = rewriter_config_pb2.RewriterConfig.OFF
        if threads > 0:
            config.intra_op_parallelism_threads = threads
            config.inter_op_parallelism_threads = threads

        if server is not None:
            return tf.compat.v1.Session(target=server.target, config=config)

        return tf.compat.v1.Session(config=config)

    def reset_session(self, sess, threads=-1, server=None):
        """Resets the current TensorFlow session, to clear memory
        or load another model.
        """

        tf.compat.v1.reset_default_graph()
        sess.close()
        sess = self.start_tf_sess(threads, server)
        return sess

    def load_model(self, model_path):
        models_dir = os.path.expanduser(os.path.expandvars(model_path))
        model_name = 'latest'
        length = 7

        if self.encoder is None:
            self.encoder = encoder.get_encoder(model_name, models_dir)
        hparams = model.default_hparams()
        with open(os.path.join(models_dir, model_name, 'hparams.json')) as f:
            hparams.override_from_dict(json.load(f))

        # if length is None:
        #     length = hparams.n_ctx // 2
        # elif length > hparams.n_ctx:
        #     raise ValueError("Can't get samples longer than window size: %s" % self.hparams.n_ctx)

        self.context = tf.compat.v1.placeholder(tf.int32, [GPT_BATCH_SIZE, None])
        self.context_shape = model.past_shape(hparams=hparams, batch_size=GPT_BATCH_SIZE)
        self.context_output = tf.compat.v1.placeholder(tf.float32, self.context_shape)
        self.suggestion = tf.compat.v1.placeholder(tf.int32, [GPT_BATCH_SIZE, None])
        self.end_index = tf.compat.v1.placeholder(tf.int32, [])
        output = model.model(hparams=hparams, X=self.context)

        saver = tf.compat.v1.train.Saver(allow_empty=True)
        self.sess.run(tf.compat.v1.global_variables_initializer())

        ckpt = tf.train.latest_checkpoint(os.path.join(models_dir, model_name))
        print('Loading model', ckpt)
        saver.restore(self.sess, ckpt)

        # output = sample.sample_sequence(
        #     hparams=hparams,
        #     length=length,
        #     context=self.context,
        #     batch_size=GPT_BATCH_SIZE,
        #     temperature=GPT_TEMPERATURE, top_k=GPT_TOP_K, top_p=GPT_TOP_P
        # )

        output = self.probability(
            hparams=hparams,
            context=self.context,
            context_output=self.context_output,
            suggestion=self.suggestion,
            end_index=self.end_index,
            batch_size=GPT_BATCH_SIZE,
        )
        return output

    def probability(self, hparams, context=None, context_output=None, suggestion=None, end_index=None, batch_size=None):
        def step(hparams, tokens, past=None):
            lm_output = model.model(hparams=hparams, X=tokens,
                                    past=past, reuse=tf.compat.v1.AUTO_REUSE)

            logits = lm_output['logits'][:, :, :hparams.n_vocab]
            presents = lm_output['present']
            presents.set_shape(model.past_shape(
                hparams=hparams, batch_size=batch_size))
            return {
                'logits': logits,
                'presents': presents,
            }

        with tf.compat.v1.name_scope('probability'):
            context_presents = tf.cond(tf.shape(context_output)[-2] > 0,
                                       lambda: context_output,
                                       lambda: step(hparams, context[:, :-1])['presents'],
                                       )

            def body(i, past, prev, output):
                next_outputs = step(hparams, prev[:, tf.newaxis], past=past)
                logits = next_outputs['logits'][:, -1, :]
                probs = tf.nn.softmax(logits)

                suggestion_token = suggestion[:, i]
                token_id = tf.stack([tf.range(tf.shape(suggestion)[0]), suggestion_token], axis=1)
                prob = tf.expand_dims(tf.gather_nd(probs, token_id), axis=1)
                return [
                    i + 1,
                    tf.cond(tf.less(end_index, i), lambda: past, lambda: tf.concat([past, next_outputs['presents']], axis=-2)),
                    tf.compat.v1.where(end_index < i, prev, suggestion_token),
                    tf.concat([output, prob], axis=1),
                ]

            def cond(*args):
                return True

            _, _, _, log_probs = tf.while_loop(
                cond=cond, body=body,
                maximum_iterations=tf.shape(suggestion)[1],
                loop_vars=[
                    tf.constant(0, dtype=tf.int32),
                    context_presents,
                    context[:, -1],
                    tf.zeros([batch_size, 0]),
                ],
                shape_invariants=[
                    tf.TensorShape([]),
                    tf.TensorShape(model.past_shape(
                        hparams=hparams, batch_size=batch_size)),
                    tf.TensorShape([batch_size]),
                    tf.TensorShape([batch_size, None]),
                ],
                back_prop=False,
            )

            return context_presents, log_probs

    def process(self, data, service):
        response = "gpt:{"
        if service == "param":
            response += self.predict_param(data)
        return response + "}"

    def predict_param(self, data):
        if PARAM_LEXICAL_ONLY:
            return self.predict_param_using_lex(data)
        else:
            return self.predict_param_all_features(data)

    def predict_param_using_lex(self, data):
        start_time = perf_counter()
        n_param = len(data['next_lex'])

        java_context_tokens = self.encoder.encode(data['lex_context'][0])
        java_suggestions_all = []
        java_suggestions_all_ori = []
        for i in range(n_param):
            java_suggestions_param = []
            java_suggestions_param_ori = []
            for j in range(len(data['next_lex'][i])):
                for k in range(len(data['next_lex'][i][j])):
                    if data['next_lex'][i][j][k] not in java_suggestions_param_ori:
                        candidate_tokens = self.encoder.encode(data['next_lex'][i][j][k])
                        java_suggestions_param.append(candidate_tokens)
                        java_suggestions_param_ori.append(data['next_lex'][i][j][k])
            java_suggestions_all.append(java_suggestions_param)
            java_suggestions_all_ori.append(java_suggestions_param_ori)
        dot_tokens = self.encoder.encode(".")
        comma_tokens = self.encoder.encode(",")
        open_paren_tokens = self.encoder.encode("(")
        close_paren_tokens = self.encoder.encode(")")
        space_tokens = self.encoder.encode(" ")
        quote_tokens = self.encoder.encode("\"\"")

        java_context_list = [(java_context_tokens, [], 0)]
        for j in range(n_param):
            java_suggestion_scores = []

            java_suggestions_param = [(ii, java_candidate) for ii, java_candidate in enumerate(java_suggestions_all[j])]
            java_suggestions_param = sorted(java_suggestions_param, key=lambda x: -len(x[1]))
            java_suggestions = []
            java_suggestions_batch = []

            # NAME
            for ii, java_candidate in java_suggestions_param:
                if java_candidate[-1] != open_paren_tokens[-1] and java_candidate[-1] != quote_tokens[-1]:
                    if len(java_suggestions_batch) > 0 and len(java_suggestions_batch[-1][1]) != len(java_candidate):
                        java_suggestions.append(java_suggestions_batch)
                        java_suggestions_batch = []
                    java_suggestions_batch.append((ii, java_candidate))
                    if len(java_suggestions_batch) == GPT_BATCH_SIZE:
                        java_suggestions.append(java_suggestions_batch)
                        java_suggestions_batch = []
            if len(java_suggestions_batch) > 0:
                java_suggestions.append(java_suggestions_batch)
                java_suggestions_batch = []

            # METHOD_INVOC + STRING_LIT
            for ii, java_candidate in java_suggestions_param:
                if java_candidate[-1] == open_paren_tokens[-1] or java_candidate[-1] == quote_tokens[-1]:
                    java_suggestions_batch.append((ii, java_candidate))
                    if len(java_suggestions_batch) == GPT_BATCH_SIZE:
                        java_suggestions.append(java_suggestions_batch)
                        java_suggestions_batch = []
            if len(java_suggestions_batch) > 0:
                java_suggestions.append(java_suggestions_batch)
                java_suggestions_batch = []

            for k in range(len(java_context_list)):
                context_data = None
                batch_context = GPT_BATCH_SIZE * [java_context_list[k][0]]

                for java_suggestions_batch in java_suggestions:
                    batch_suggestion = np.empty(shape=[GPT_BATCH_SIZE, len(java_suggestions_batch[0][1])])
                    for ii in range(len(java_suggestions_batch)):
                        java_candidate = java_suggestions_batch[ii][1]
                        batch_suggestion[ii, 0: len(java_candidate)] = java_candidate

                    if j > 0:
                        batch_suggestion = np.hstack((np.tile(comma_tokens, (GPT_BATCH_SIZE, 1)),
                                                      batch_suggestion,
                                                      ))

                    end_index = batch_suggestion.shape[1] - 1
                    if java_suggestions_batch[0][1][-1] == open_paren_tokens[-1]:   # METHOD_INVOC
                        pass
                    elif java_suggestions_batch[0][1][-1] == quote_tokens[-1]:      # STRING_LIT
                        pass
                    else:
                        # NAME
                        # batch_suggestion = np.hstack((batch_suggestion,
                        #                               np.tile(comma_tokens + close_paren_tokens, (GPT_BATCH_SIZE, 1)),
                        #                               ))

                        # NAME + COMPOUND
                        batch_suggestion = np.hstack((batch_suggestion,
                                                      np.tile(comma_tokens + close_paren_tokens + space_tokens, (GPT_BATCH_SIZE, 1)),
                                                      ))

                        # NOT (METHOD_INVOC + FIELD_ACCESS)
                        # batch_suggestion = np.hstack((batch_suggestion,
                        #                               np.tile(dot_tokens, (GPT_BATCH_SIZE, 1)),
                        #                               ))

                    if context_data is None:
                        feed_dict = {self.context: batch_context,
                                     self.context_output: np.empty(shape=[0 if v is None else v for v in self.context_shape]),
                                     self.suggestion: batch_suggestion,
                                     self.end_index: end_index,
                                     }
                        context_data, out = self.sess.run(self.java_model, feed_dict=feed_dict)
                    else:
                        feed_dict = {self.context: GPT_BATCH_SIZE * [java_context_list[k][0][-1:]],
                                     self.context_output: context_data,
                                     self.suggestion: batch_suggestion,
                                     self.end_index: end_index,
                                     }
                        context_data, out = self.sess.run(self.java_model, feed_dict=feed_dict)

                    smoothing_prob_batch = np.where(out > 0, out, 1e-7)
                    for ii in range(len(java_suggestions_batch)):
                        java_candidate = java_suggestions_batch[ii][1]
                        if j == 0:
                            java_suggestion = java_candidate
                        else:
                            java_suggestion = comma_tokens + java_candidate
                        new_context = java_context_list[k][0] + java_suggestion

                        smoothing_prob = smoothing_prob_batch[ii]
                        if end_index < len(smoothing_prob) - 1:
                            # NAME
                            smoothing_prob = np.hstack((smoothing_prob[ :end_index+1], [np.sum(smoothing_prob[end_index+1:])]))

                            # NOT (METHOD_INVOC + FIELD_ACCESS)
                            # smoothing_prob = np.hstack((smoothing_prob[:-1], [1-smoothing_prob[-1]]))
                        else:
                            smoothing_prob = smoothing_prob[:len(java_suggestion)]

                        log_prob = np.log(smoothing_prob)
                        model_score = np.sum(log_prob)

                        # Debug
                        # for suggestion in java_candidate:
                        #     print(self.encoder.decode([suggestion]), end=' ')
                        # print()
                        # print(model_score, " ", log_prob)

                        java_suggestion_scores.append((new_context,
                                                       java_context_list[k][1] + [java_suggestions_batch[ii][0]],
                                                       java_context_list[k][2] + model_score))

            java_context_list = sorted(java_suggestion_scores, key=lambda x: -x[2])
        sorted_scores = java_context_list
        result_gpt = []
        for i in range(min(self.top_k, len(sorted_scores))):
            result_gpt.append(sorted_scores[i][1])
        runtime_gpt = perf_counter() - start_time
        self.logger.debug("Total gpt runtime: " + str(runtime_gpt))
        result_gpt = self.trace(result_gpt, java_suggestions_all_ori)
        self.logger.debug("Result gpt:\n", result_gpt)
        response = 'result:' + json.dumps(result_gpt) \
                   + ',runtime:' + str(runtime_gpt)
        return response

    def trace(self, result, data):
        origin = []
        for candidate_ids in result:
            candidate_text = ""
            for i in range(len(candidate_ids)):
                candidate_text += data[i][candidate_ids[i]]
                if i < len(candidate_ids) - 1:
                    candidate_text += ", "
            origin.append(candidate_text)
        return origin

    def predict_param_all_features(self, data):
        start_time = perf_counter()
        n_param = len(data['next_lex'])

        java_context_tokens = self.encoder.encode(data['lex_context'][0])
        java_suggestions_all = []
        for i in range(n_param):
            java_suggestions_param = []
            for j in range(len(data['next_lex'][i])):
                java_suggestions_param_excode = []
                for k in range(len(data['next_lex'][i][j])):
                    candidate_tokens = self.encoder.encode(data['next_lex'][i][j][k])
                    java_suggestions_param_excode.append(candidate_tokens)
                java_suggestions_param.append(java_suggestions_param_excode)
            java_suggestions_all.append(java_suggestions_param)
        dot_tokens = self.encoder.encode(".")
        comma_tokens = self.encoder.encode(",")
        open_paren_tokens = self.encoder.encode("(")
        close_paren_tokens = self.encoder.encode(")")
        space_tokens = self.encoder.encode(" ")
        quote_tokens = self.encoder.encode("\"\"")

        java_context_list = [(java_context_tokens, [], 0)]
        all_candidate_lex = []
        for j in range(n_param):
            java_suggestion_scores = []
            for k in range(len(java_context_list)):
                context_data = None
                batch_context = GPT_BATCH_SIZE * [java_context_list[k][0]]
                for jj in range(len(java_suggestions_all[j])):
                    java_suggestions = java_suggestions_all[j][jj]
                    for ii, java_candidate in enumerate(java_suggestions):
                        if j == 0:
                            java_suggestion = java_candidate
                        else:
                            java_suggestion = comma_tokens + java_candidate
                        new_context = java_context_list[k][0] + java_suggestion

                        end_index = len(java_suggestion) - 1
                        if java_candidate[-1] == open_paren_tokens[-1]: # method call
                            pass
                        elif java_candidate[-1] == quote_tokens[-1]:    # string literal
                            pass
                        else:
                            # NAME
                            #java_suggestion = java_suggestion + comma_tokens + close_paren_tokens

                            # NAME + COMPOUND
                            java_suggestion = java_suggestion + comma_tokens + close_paren_tokens + space_tokens

                            # NOT (METHOD_INVOC + FIELD_ACCESS)
                            #java_suggestion = java_suggestion + dot_tokens


                        batch_suggestion = GPT_BATCH_SIZE * [java_suggestion]

                        if context_data is None:
                            feed_dict = {self.context: batch_context,
                                         self.context_output: np.empty(shape=[0 if v is None else v for v in self.context_shape]),
                                         self.suggestion: batch_suggestion,
                                         self.end_index: end_index,
                                         }
                            context_data, out = self.sess.run(self.java_model, feed_dict=feed_dict)
                        else:
                            feed_dict = {self.context: GPT_BATCH_SIZE * [java_context_list[k][0][-1:]],
                                         self.context_output: context_data,
                                         self.suggestion: batch_suggestion,
                                         self.end_index: end_index,
                                         }
                            context_data, out = self.sess.run(self.java_model, feed_dict=feed_dict)

                        smoothing_prob = np.where(out > 0, out, 1e-7)
                        if end_index != len(java_suggestion) - 1:
                            # NAME
                            smoothing_prob = np.hstack((smoothing_prob[:, :end_index+1],
                                                        np.sum(smoothing_prob[:, end_index+1:],axis=1,keepdims=True),
                                                        ))

                            # NOT (METHOD_INVOC + FIELD_ACCESS)
                            # smoothing_prob = np.hstack((smoothing_prob[:,:-1],1-smoothing_prob[:,-1:]))


                        log_prob = np.log(smoothing_prob)
                        model_score = np.sum(log_prob, axis=1)[0]

                        # Debug
                        # for suggestion in java_candidate:
                        #     print(self.encoder.decode([suggestion]), end=' ')
                        # print()
                        # print(model_score, " ", log_prob)

                        java_suggestion_scores.append((new_context,
                                                       java_context_list[k][1] + [(jj, ii)],
                                                       java_context_list[k][2] + model_score))
            java_context_list = sorted(java_suggestion_scores, key=lambda x: -x[2])
        all_candidate_lex += java_context_list

        """
        for _ in range(self.top_k // GPT_BATCH_SIZE):
            feed_dict = {self.context: batch_context}
            out = self.sess.run(self.java_model, feed_dict=feed_dict)[:, len(java_context_tokens):]

            for i in range(GPT_BATCH_SIZE):
                text = self.encoder.decode(out[i])
                if ';' in text:
                    text = text[:text.index(';')+1]

                if '(' in text:
                    text = text[:text.index('(')+1]
                elif ',' in text:
                    text = text[:text.index(',')]
                elif ')' in text:
                    if text.index(')') > 0:
                        text = text[:text.index(')')]
                predictions.append(str(text))
        """

        return self.select_top_param_candidates(all_candidate_lex, data, start_time)

    def select_top_param_candidates(self, all_candidate_lex, data, start_time):
        sorted_scores = sorted(all_candidate_lex, key=lambda x: -x[2])
        result_gpt = []
        for i in range(min(self.top_k, len(sorted_scores))):
            result_gpt.append(sorted_scores[i][1])
        runtime_gpt = perf_counter() - start_time
        self.logger.debug("Total gpt runtime: " + str(runtime_gpt))
        result_gpt = self.recreate(result_gpt, data)
        self.logger.debug("Result gpt:\n", result_gpt)
        response = 'result:' + json.dumps(result_gpt) \
                   + ',runtime:' + str(runtime_gpt)
        return response