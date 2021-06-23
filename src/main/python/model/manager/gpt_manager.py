import json
import os
import numpy as np
from time import perf_counter
import tensorflow as tf
from model.manager.model_manager import ModelManager
from ..gpt import encoder, model, sample
from ..config import *

class GPTManager(ModelManager):
    def __init__(self, top_k, project, train_len,
                 excode_model_path, java_model_path, method_call_model_path):

        os.environ["KMP_BLOCKTIME"] = "1"
        os.environ["KMP_SETTINGS"] = "1"
        os.environ["KMP_AFFINITY"] = "granularity=fine,verbose,compact,1,0"

        gpu_options = tf.GPUOptions(allow_growth=True)
        config = tf.ConfigProto(intra_op_parallelism_threads=0, inter_op_parallelism_threads=0,
                                allow_soft_placement=True, gpu_options=gpu_options)

        self.sess = tf.Session(config=config)
        self.encoder = None

        super().__init__(top_k, project, train_len,
                         excode_model_path, java_model_path, method_call_model_path)

    def __del__(self):
        self.sess.close()

    def load_model(self, model_path):
        models_dir = os.path.expanduser(os.path.expandvars(model_path))
        model_name = 'latest'
        seed = 42
        length = 7

        if self.encoder is None:
            self.encoder = encoder.get_encoder(model_name, models_dir)
        hparams = model.default_hparams()
        with open(os.path.join(models_dir, model_name, 'hparams.json')) as f:
            hparams.override_from_dict(json.load(f))

        if length is None:
            length = hparams.n_ctx // 2
        elif length > hparams.n_ctx:
            raise ValueError("Can't get samples longer than window size: %s" % hparams.n_ctx)

        context = tf.placeholder(tf.int32, [GPT_BATCH_SIZE, None])
        np.random.seed(seed)
        tf.set_random_seed(seed)

        output = sample.sample_sequence(
            hparams=hparams, length=length,
            context=context,
            batch_size=GPT_BATCH_SIZE,
            temperature=GPT_TEMPERATURE, top_k=GPT_TOP_K, top_p=GPT_TOP_P
        )

        saver = tf.train.Saver(allow_empty=True)
        self.sess.run(tf.global_variables_initializer())
        ckpt = tf.train.latest_checkpoint(os.path.join(models_dir, model_name))
        print('Loading model', ckpt)
        saver.restore(self.sess, ckpt)
        return output

    def process(self, data, service):
        response = "gpt:{"
        if service == "param":
            response += self.predict_param(data)
        return response + "}"

    def predict_param(self, data):
        if PARAM_LEXICAL_ONLY:
            return self.predict_param_using_lex(data)
        else:
            return self.predict_param_using_lex(data)

    def predict_param_using_lex(self, data):
        start_time = perf_counter()
        n_param = len(data['next_lex'])

        java_context_tokens = self.encoder.encode(data['lex_context'][0])
        java_suggestions_all = []
        for i in range(n_param):
            java_suggestions_all.append([])
            for j in range(len(data['next_lex'][i])):
                java_suggestions_all[i].append([])
                for k in range(len(data['next_lex'][i][j])):
                    candidate_tokens = self.encoder.encode(data['next_lex'][i][j][k])
                    java_suggestions_all[i][j].append(candidate_tokens)
        java_context_list = [(java_context_tokens, [])]
        all_candidate_lex = []
        for j in range(n_param):
            java_suggestion_scores = []
            for k in range(len(java_context_list)):
                for jj in range(len(java_suggestions_all[j])):
                    java_suggestions = java_suggestions_all[j][jj]
                    for ii, java_suggestion in enumerate(java_suggestions):
                        new_context = java_context_list[k][0] + java_suggestion
                        if j < n_param - 1:
                            new_context += [',']
                        model_score = 0
                        java_suggestion_scores.append((new_context, java_context_list[k][1]
                                                       + [(jj, ii)], model_score))
            sorted_scores = sorted(java_suggestion_scores, key=lambda x: -x[2])
            if j < n_param - 1:
                java_context_list = [(x[0], x[1]) for x in sorted_scores]
            else:
                java_context_list = sorted_scores
        all_candidate_lex += java_context_list
        if all_candidate_lex is not None:
            return self.select_top_param_candidates(all_candidate_lex, data, start_time)

        generated = 0
        predictions = []
        for _ in range(self.top_k // GPT_BATCH_SIZE):
            context = tf.placeholder(tf.int32, [GPT_BATCH_SIZE, None])
            feed_dict = {context: [java_context_tokens for _ in range(GPT_BATCH_SIZE)]}
            out = self.sess.run(self.java_model, feed_dict=feed_dict)[:, len(java_context_tokens):]

            for i in range(GPT_BATCH_SIZE):
                generated += 1
                text = self.encoder.decode(out[i])
                predictions.append(str(text))

        response = 'result:' + json.dumps(predictions) \
                   + ',runtime:' + str(0)
        return response


    def select_top_param_candidates(self, all_candidate_lex, data, start_time):
        sorted_scores = sorted(all_candidate_lex, key=lambda x: -x[2])[:self.top_k]
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