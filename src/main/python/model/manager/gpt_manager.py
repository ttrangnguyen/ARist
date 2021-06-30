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

        if self.encoder is None:
            self.encoder = encoder.get_encoder(model_name, models_dir)
        hparams = model.default_hparams()
        with open(os.path.join(models_dir, model_name, 'hparams.json')) as f:
            hparams.override_from_dict(json.load(f))
        self.list_end_tokens(hparams)

        self.context = tf.compat.v1.placeholder(tf.int32, [GPT_BATCH_SIZE, None])
        self.context_shape = model.past_shape(hparams=hparams, batch_size=GPT_BATCH_SIZE)
        self.context_output = tf.compat.v1.placeholder(tf.float32, self.context_shape)
        self.suggestion = tf.compat.v1.placeholder(tf.int32, [GPT_BATCH_SIZE, None])
        self.end_tokens = tf.compat.v1.placeholder(tf.int32, [None])
        output = model.model(hparams=hparams, X=self.context)

        saver = tf.compat.v1.train.Saver(allow_empty=True)
        self.sess.run(tf.compat.v1.global_variables_initializer())

        ckpt = tf.train.latest_checkpoint(os.path.join(models_dir, model_name))
        print('Loading model', ckpt)
        saver.restore(self.sess, ckpt)

        max_length = 20
        if max_length is None:
            max_length = hparams.n_ctx // 2
        elif max_length > hparams.n_ctx:
            raise ValueError("Can't get samples longer than window size: %s" % self.hparams.n_ctx)
        output_autogen = sample.sample_sequence(
            hparams=hparams,
            max_length=max_length,
            context=self.context,
            context_output=self.context_output,
            end_tokens=self.end_tokens,
            batch_size=GPT_BATCH_SIZE,
            temperature=GPT_TEMPERATURE,
            top_k=GPT_TOP_K,
            top_p=GPT_TOP_P
        )

        output_pa = sample.probability(
            hparams=hparams,
            context=self.context,
            context_output=self.context_output,
            suggestion=self.suggestion,
            end_tokens=self.end_tokens,
            batch_size=GPT_BATCH_SIZE,
        )
        return {
            "autogen": output_autogen,
            "pa": output_pa
        }

    def list_end_tokens(self, hparams):
        self.end_token_list = []
        self.method_invoc_token_list = []
        self.string_lit_token_list = []
        self.name_suffix_token_list = []
        for i in range(0, hparams.n_vocab):
            token = self.encoder.decode([i])

            for c in [";", "(", ",", ")"]:
                if c in token:
                    self.end_token_list.append(i)
                    #print(token)
                    break

            if token in ["(", "()", "(\"", "('", "((", "().", "(),", "(_", "())", "(-"]:
                self.method_invoc_token_list.append(i)

            if token in ["\"", "\",", "\".", "\",\"", "\")", "\").", "\"),", "\"))", "\"\""]:
                self.string_lit_token_list.append(i)

            if token in ["%", "&", ")", "*", "+", ",", "-", "/", "<", "=", ">", "?", "^", "|", " ", "\t", " -", ",\"",
                         ").", " =", "),", " ,", "==", " |", " /", " &", " )", " <", " +", " *", "?\"", "->", "))",
                         ",'", " %", ">>", " ->", "++", " ?", " ==", " ).", " ?'", " ||", " >>", " <<", " ^", " ),",
                         " &&", ",-", ")-", " ))", "||", ",'\"", "<<", " >=", "-'", " ++", ")/", ")))", "&&", "),\"",
                         ")*", "++)", ")).", "-(", "+(", ">(", "))))", ")),", ")[", " ,\"", ")--", " )))"]:
                self.name_suffix_token_list.append(i)

        print(len(self.end_token_list), "end tokens.")

    def process(self, data, service):
        response = "gpt:{"
        if service == "param":
            response += self.predict_param(data)
        return response + "}"

    def predict_param(self, data):
        if USE_PROGRAM_ANALYSIS:
            if PARAM_LEXICAL_ONLY:
                return self.predict_param_using_lex(data)
            else:
                return self.predict_param_all_features(data)
        else:
            return self.generate_param(data)

    def probability(self, context_tokens, suggestions_tokens, context_data=None):
        batch_context = GPT_BATCH_SIZE * [context_tokens]
        suggestion = self.encoder.decode(suggestions_tokens[0])

        if suggestion[-1] == "(":       # METHOD_INVOC
            batch_suggestion = np.empty(shape=[GPT_BATCH_SIZE, len(suggestions_tokens[0])-1])
            for i in range(len(suggestions_tokens)):
                batch_suggestion[i, :] = suggestions_tokens[i][:-1]
            end_tokens = self.method_invoc_token_list

        elif suggestion[-1] == "\"":    # STRING_LIT
            batch_suggestion = np.empty(shape=[GPT_BATCH_SIZE, len(suggestions_tokens[0])-1])
            for i in range(len(suggestions_tokens)):
                batch_suggestion[i, :] = suggestions_tokens[i][:-1]
            end_tokens = self.string_lit_token_list

        else:                           # NAME, FIELD_ACCESS, v.v..
            batch_suggestion = np.empty(shape=[GPT_BATCH_SIZE, len(suggestions_tokens[0])])
            for i in range(len(suggestions_tokens)):
                batch_suggestion[i, :] = suggestions_tokens[i]
            end_tokens = self.name_suffix_token_list

        if context_data is None:
            feed_dict = {self.context: batch_context,
                         self.context_output: np.empty(shape=[0 if v is None else v for v in self.context_shape]),
                         self.suggestion: batch_suggestion,
                         self.end_tokens: end_tokens,
                         }
            context_data, out = self.sess.run(self.java_model['pa'], feed_dict=feed_dict)
        else:
            feed_dict = {self.context: GPT_BATCH_SIZE * [context_tokens[-1:]],
                         self.context_output: context_data,
                         self.suggestion: batch_suggestion,
                         self.end_tokens: end_tokens,
                         }
            context_data, out = self.sess.run(self.java_model['pa'], feed_dict=feed_dict)

        suggestions_result = []
        for i in range(len(suggestions_tokens)):
            prob = out[i]
            suggestion = self.encoder.decode(suggestions_tokens[i])
            if suggestion[-1] == "(":       # METHOD_INVOC
                suggestion_tokens = self.encoder.encode(suggestion+"),")
            elif suggestion[-1] == "\"":    # STRING_LIT
                suggestion_tokens = self.encoder.encode(suggestion+",")
            else:                           # NAME, FIELD_ACCESS, v.v..
                suggestion_tokens = self.encoder.encode(suggestion+",")

            log_prob = np.maximum(np.log(prob), LOG_ZERO)
            score = np.sum(log_prob)
            new_context_tokens = context_tokens + suggestion_tokens

            # Debug
            # for token in suggestions_tokens[i]:
            #     print(self.encoder.decode([token]), end=' ')
            # print()
            # print(score, log_prob)

            suggestions_result.append((score, new_context_tokens))
        return suggestions_result, context_data

    def predict_param_using_lex(self, data):
        start_time = perf_counter()
        n_param = len(data['next_lex'])

        candidates_all = []
        for i in range(n_param):
            candidates_param = []
            for j in range(len(data['next_lex'][i])):
                for k in range(len(data['next_lex'][i][j])):
                    if data['next_lex'][i][j][k] not in candidates_param:
                        candidates_param.append(data['next_lex'][i][j][k])
            candidates_all.append(candidates_param)
        context_tokens = self.encoder.encode(data['lex_context'][0])

        context_list = [(context_tokens, [], 0)]
        for i in range(n_param):
            suggestion_scores = []
            for j in range(len(context_list)):
                suggestions_data = []
                for candidate_id, candidate in enumerate(candidates_all[i]):
                    suggestion = self.encoder.decode([context_list[i][0][-1]]) + candidate
                    suggestions_data.append((candidate_id, self.encoder.encode(suggestion)))

                suggestions_data = sorted(suggestions_data, key=lambda x: -len(x[1]))

                suggestions_batches = []
                suggestions_batch = []
                # NAME
                for candidate_id, suggestion_tokens in suggestions_data:
                    candidate = candidates_all[i][candidate_id]
                    if candidate[-1] != "(" and candidate[-1] != "\"":
                        if len(suggestions_batch) > 0 and len(suggestions_batch[-1][1]) != len(suggestion_tokens):
                            suggestions_batches.append(suggestions_batch)
                            suggestions_batch = []
                        suggestions_batch.append((candidate_id, suggestion_tokens))
                        if len(suggestions_batch) == GPT_BATCH_SIZE:
                            suggestions_batches.append(suggestions_batch)
                            suggestions_batch = []
                if len(suggestions_batch) > 0:
                    suggestions_batches.append(suggestions_batch)
                    suggestions_batch = []

                # METHOD_INVOC
                for candidate_id, suggestion_tokens in suggestions_data:
                    candidate = candidates_all[i][candidate_id]
                    if candidate[-1] == "(":
                        if len(suggestions_batch) > 0 and len(suggestions_batch[-1][1]) != len(suggestion_tokens):
                            suggestions_batches.append(suggestions_batch)
                            suggestions_batch = []
                        suggestions_batch.append((candidate_id, suggestion_tokens))
                        if len(suggestions_batch) == GPT_BATCH_SIZE:
                            suggestions_batches.append(suggestions_batch)
                            suggestions_batch = []
                if len(suggestions_batch) > 0:
                    suggestions_batches.append(suggestions_batch)
                    suggestions_batch = []

                # STRING_LIT
                for candidate_id, suggestion_tokens in suggestions_data:
                    candidate = candidates_all[i][candidate_id]
                    if candidate[-1] == "\"":
                        if len(suggestions_batch) > 0 and len(suggestions_batch[-1][1]) != len(suggestion_tokens):
                            suggestions_batches.append(suggestions_batch)
                            suggestions_batch = []
                        suggestions_batch.append((candidate_id, suggestion_tokens))
                        if len(suggestions_batch) == GPT_BATCH_SIZE:
                            suggestions_batches.append(suggestions_batch)
                            suggestions_batch = []
                if len(suggestions_batch) > 0:
                    suggestions_batches.append(suggestions_batch)
                    suggestions_batch = []

                context_data = None
                for suggestions_batch in suggestions_batches:
                    suggestions_tokens = [v for _, v in suggestions_batch]
                    suggestions_result, context_data = self.probability(context_list[j][0][:-1], suggestions_tokens, context_data)
                    for k in range(len(suggestions_batch)):
                        score, new_context_tokens = suggestions_result[k]
                        suggestion_scores.append((
                            new_context_tokens,
                            context_list[j][1] + [suggestions_batch[k][0]],
                            context_list[j][2] + score,
                        ))

            context_list = sorted(suggestion_scores, key=lambda x: -x[2])
        sorted_scores = context_list
        result_gpt = []
        for i in range(min(self.top_k, len(sorted_scores))):
            result_gpt.append(sorted_scores[i][1])
        runtime_gpt = perf_counter() - start_time
        self.logger.debug("Total gpt runtime: " + str(runtime_gpt))
        result_gpt = self.trace(result_gpt, candidates_all)
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
        quote_first_param_tokens = self.encoder.encode("(\"")

        java_context_list = [(java_context_tokens, [], 0)]
        all_candidate_lex = []
        for j in range(n_param):
            java_suggestion_scores = []
            for k in range(len(java_context_list)):
                context_data = None
                batch_context = GPT_BATCH_SIZE * [java_context_list[k][0]]

                string_lit_data = None

                for jj in range(len(java_suggestions_all[j])):
                    java_suggestions = java_suggestions_all[j][jj]
                    for ii, java_candidate in enumerate(java_suggestions):
                        if java_candidate[-1] == open_paren_tokens[-1]:   # METHOD_INVOC
                            java_suggestion = java_candidate[:-1]
                        else:
                            java_suggestion = java_candidate

                        if j > 0:
                            java_suggestion = comma_tokens + java_suggestion

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
                            #java_suggestion = java_suggestion + comma_tokens + close_paren_tokens + space_tokens

                            # NOT (METHOD_INVOC + FIELD_ACCESS)
                            java_suggestion = java_suggestion + dot_tokens


                        batch_suggestion = GPT_BATCH_SIZE * [java_suggestion]

                        if context_data is None:
                            feed_dict = {self.context: batch_context,
                                         self.context_output: np.empty(shape=[0 if v is None else v for v in self.context_shape]),
                                         self.suggestion: batch_suggestion,
                                         self.end_index: end_index,
                                         }
                            context_data, out = self.sess.run(self.java_model['pa'], feed_dict=feed_dict)
                        else:
                            feed_dict = {self.context: GPT_BATCH_SIZE * [java_context_list[k][0][-1:]],
                                         self.context_output: context_data,
                                         self.suggestion: batch_suggestion,
                                         self.end_index: end_index,
                                         }
                            context_data, out = self.sess.run(self.java_model['pa'], feed_dict=feed_dict)

                        smoothing_prob = np.where(out > 0, out, 1e-7)
                        if end_index < len(java_suggestion) - 1:
                            # NAME
                            # smoothing_prob = np.hstack((smoothing_prob[:, :end_index+1],
                            #                             np.sum(smoothing_prob[:, end_index+1:],axis=1,keepdims=True),
                            #                             ))

                            # NOT (METHOD_INVOC + FIELD_ACCESS)
                            smoothing_prob = np.hstack((smoothing_prob[:,:-1],1-smoothing_prob[:,-1:]))


                        log_prob = np.log(smoothing_prob)
                        model_score = np.sum(log_prob, axis=1)[0]

                        # Debug
                        # for suggestion in java_candidate:
                        #     print(self.encoder.decode([suggestion]), end=' ')
                        # print()
                        # print(model_score, " ", log_prob)

                        # Special case where string lit is the first param
                        if java_candidate[-1] == quote_tokens[-1] and java_context_list[k][0][-1] == open_paren_tokens[-1]:
                            string_lit_data = (new_context,
                                               (jj, ii),
                                               model_score)
                        else:
                            java_suggestion_scores.append((new_context,
                                                           java_context_list[k][1] + [(jj, ii)],
                                                           java_context_list[k][2] + model_score))

                # Special case where string lit is the first param
                if java_context_list[k][0][-1] == open_paren_tokens[-1] and string_lit_data is not None:
                    feed_dict = {self.context: GPT_BATCH_SIZE * [java_context_list[k][0][:-1]],
                                 self.context_output: np.empty(shape=[0 if v is None else v for v in self.context_shape]),
                                 self.suggestion: GPT_BATCH_SIZE * [quote_first_param_tokens],
                                 self.end_index: len(quote_first_param_tokens),
                                 }
                    _, out = self.sess.run(self.java_model['pa'], feed_dict=feed_dict)
                    log_prob = np.log(out[0])
                    model_score = np.sum(log_prob)

                    # Debug
                    # print("string lit at first param")
                    # print(model_score, " ", log_prob)

                    java_suggestion_scores.append((string_lit_data[0],
                                                   java_context_list[k][1] + [string_lit_data[1]],
                                                   java_context_list[k][2] + max(string_lit_data[2], model_score)))
            java_context_list = sorted(java_suggestion_scores, key=lambda x: -x[2])
        all_candidate_lex += java_context_list
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

    def generate_param(self, data):
        start_time = perf_counter()
        n_param = len(data['next_lex'])

        java_context_tokens = self.encoder.encode(data['lex_context'][0])
        java_suggestions_all = []
        for i in range(n_param):
            java_suggestions_param = set()
            for j in range(len(data['next_lex'][i])):
                for k in range(len(data['next_lex'][i][j])):
                    java_suggestions_param.add(data['next_lex'][i][j][k])
            java_suggestions_all.append(java_suggestions_param)
        comma_tokens = self.encoder.encode(",")

        java_context_list = [(java_context_tokens, [], 0)]
        for j in range(n_param):
            java_suggestion_scores = []
            for k in range(len(java_context_list)):
                context_data = None
                batch_context = GPT_BATCH_SIZE * [java_context_list[k][0]]

                suggestion_set = set()
                suggestion_retry_set = set()
                retry_count = 0
                break_while_flag = False
                while True:
                    if context_data is None:
                        feed_dict = {self.context: batch_context,
                                     self.context_output: np.empty(shape=[0 if v is None else v for v in self.context_shape]),
                                     self.end_tokens: self.end_token_list,
                                     }
                        context_data, out, out_prob = self.sess.run(self.java_model['autogen'], feed_dict=feed_dict)
                    else:
                        feed_dict = {self.context: GPT_BATCH_SIZE * [java_context_list[k][0][-1:]],
                                     self.context_output: context_data,
                                     self.end_tokens: self.end_token_list,
                                     }
                        context_data, out, out_prob = self.sess.run(self.java_model['autogen'], feed_dict=feed_dict)

                    for i in range(GPT_BATCH_SIZE):
                        suggestion_java = ""
                        suggestion_tokens = []
                        prob = out_prob[i]
                        break_i_flag = False
                        for t in range(len(out[i])):
                            token = self.encoder.decode([out[i][t]])
                            for c in range(len(token)):
                                if token[c] == ";":
                                    prob = prob[:t] + [LOG_ZERO]
                                    suggestion_java += token[:c]
                                    suggestion_tokens.append(comma_tokens[0])
                                    break_i_flag = True
                                if token[c] == "(":
                                    prob = prob[:t + 1]
                                    suggestion_java += token[:c + 1]
                                    suggestion_tokens.append(out[i][t])
                                    suggestion_tokens.append(comma_tokens[0])
                                    break_i_flag = True
                                if token[c] == ",":
                                    prob = prob[:t + 1]
                                    if j == n_param - 1:
                                        suggestion_java += token[:c]
                                        suggestion_tokens.append(comma_tokens[0])
                                    else:
                                        suggestion_java += token
                                        suggestion_tokens.append(out[i][t])
                                    break_i_flag = True
                                if token[c] == ")":
                                    prob = prob[:t + 1]
                                    suggestion_java += token[:c]
                                    suggestion_tokens.append(comma_tokens[0])
                                    break_i_flag = True
                                if break_i_flag:
                                    break
                            if break_i_flag:
                                break
                            suggestion_java += token
                            suggestion_tokens.append(out[i][t])
                        if not break_i_flag:
                            suggestion_tokens.append(comma_tokens[0])
                        log_prob = np.log(prob)
                        model_score = np.sum(log_prob)

                        # Debug
                        # for t in range(len(out[i])):
                        #     print(self.encoder.decode([out[i][t]]), end=' ')
                        # print()
                        # print(model_score, " ", log_prob)

                        if suggestion_java not in suggestion_set:
                            suggestion_set.add(suggestion_java)
                            java_suggestion_scores.append((java_context_list[k][0] + suggestion_tokens,
                                                           java_context_list[k][1] + [suggestion_java],
                                                           java_context_list[k][2] + model_score))
                        elif suggestion_java not in suggestion_retry_set:
                            suggestion_retry_set.add(suggestion_java)
                            retry_count = retry_count + 1
                        else:
                            retry_count = retry_count + 2
                        if len(suggestion_set) >= TOP_K or retry_count >= TOP_K:
                            break_while_flag = True
                        if break_while_flag:
                            break
                    if break_while_flag:
                        break

            java_context_list = sorted(java_suggestion_scores, key=lambda x: -x[2])
        sorted_scores = java_context_list
        result_gpt = []
        for i in range(min(self.top_k, len(sorted_scores))):
            suggestion = ""
            for j in range(len(sorted_scores[i][1]) - 1):
                suggestion += sorted_scores[i][1][j] + ", "
            suggestion += sorted_scores[i][1][-1]
            result_gpt.append(suggestion)
        runtime_gpt = perf_counter() - start_time
        self.logger.debug("Total gpt runtime: " + str(runtime_gpt))
        self.logger.debug("Result gpt:\n", result_gpt)
        response = 'result:' + json.dumps(result_gpt) \
                   + ',runtime:' + str(runtime_gpt)
        return response