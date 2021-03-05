import json
from model.java.java_preprocess import java_tokenize_take_last, java_tokenize_sentences
from model.excode.excode_preprocess import excode_tokenize, excode_tokenize_candidates
from model.method_call.preprocessing import extract_method_call_from_cfg_string
from name_stat.name_tokenizer import tokenize
from model.predictor import prepare_sentences, predict, evaluate
from time import perf_counter
import numpy as np
from model.manager.model_manager import ModelManager


class RNNManager(ModelManager):
    def __init__(self, top_k, project, train_len,
                 excode_model_path, java_model_path, method_call_model_path):
        super().__init__(top_k, project, train_len,
                         excode_model_path, java_model_path, method_call_model_path)

    def process(self, data, service):
        response = "rnn:{"
        if service == "param":
            response += self.predict_param(data)
        elif service == "method_name":
            response += self.predict_method_name_using_cfg(data)
        return response + "}"

    def predict_param(self, data):
        start_time = perf_counter()
        if data['method_name'] != "":
            method_name = tokenize(data['method_name'])
        else:
            method_name = "<UNK>"
        class_name = tokenize(data['class_name'])
        method_name_tokens_excode = self.excode_tokenizer.texts_to_sequences([method_name])[0]
        class_name_tokens_excode = self.excode_tokenizer.texts_to_sequences([class_name])[0]
        method_name_tokens_java = self.java_tokenizer.texts_to_sequences([method_name])[0]
        class_name_tokens_java = self.java_tokenizer.texts_to_sequences([class_name])[0]
        excode_origin_context = excode_tokenize(data['excode_context'],
                                                tokenizer=self.excode_tokenizer,
                                                train_len=self.train_len,
                                                tokens=self.excode_tokens,
                                                method_content_only=False)[0]
        excode_context = [([[]], [])]
        excode_comma_id = self.excode_tokenizer.texts_to_sequences([["SEPA(,)"]])[0]
        n_param = len(data['next_excode'])
        for p_id in range(n_param):
            excode_suggestions = excode_tokenize_candidates(data['next_excode'][p_id],
                                                            tokenizer=self.excode_tokenizer,
                                                            tokens=self.excode_tokens)
            excode_suggestion_scores = []
            x_test_all = []
            y_test_all = []
            sentence_len_all = []
            for ex_suggest_id in range(len(excode_context)):
                curr_context = excode_origin_context + excode_context[ex_suggest_id][0][0]
                x_test, y_test, sentence_len = prepare_sentences(curr_context,
                                                                 excode_suggestions,
                                                                 self.train_len,
                                                                 len(curr_context))
                x_test_all += x_test.tolist()
                y_test_all += y_test.tolist()
                sentence_len_all += sentence_len
            x_test_all = np.array(x_test_all)
            y_test_all = np.array(y_test_all)
            p_pred = predict(self.excode_model, x_test_all,
                             method_name_tokens=method_name_tokens_excode,
                             class_name_tokens=class_name_tokens_excode)
            log_p_sentence = evaluate(p_pred, y_test_all, sentence_len_all)
            counter = 0
            for ex_suggest_id in range(len(excode_context)):
                for i, excode_suggestion in enumerate(excode_suggestions):
                    new_context = excode_context[ex_suggest_id][0][0] + excode_suggestion
                    if p_id < n_param - 1:
                        new_context += excode_comma_id
                    excode_suggestion_scores.append(([new_context],
                                                     excode_context[ex_suggest_id][1] + [i],
                                                     log_p_sentence[counter]))
                    counter += 1
            sorted_scores = sorted(excode_suggestion_scores, key=lambda x: -x[2])[:self.max_keep_step[p_id]]
            excode_context = [(x[0], x[1]) for x in sorted_scores]
            # logger.debug(sorted_scores)
            # logger.debug('-----------------------------\n-----------------------------\n-----------------------------')
            # logger.debug("Best excode suggestion(s):")

        # for i in range(min(self.top_k, len(excode_context))):
        #     if expected_excode == excode_context[i][0][0]:
        #         self.rnn_excode_correct[i] += 1
        java_origin_context = java_tokenize_take_last(data['lex_context'],
                                                      tokenizer=self.java_tokenizer,
                                                      train_len=self.train_len)
        java_comma_id = java_tokenize_take_last([","],
                                                tokenizer=self.java_tokenizer,
                                                train_len=self.train_len)
        java_suggestions_all = []
        for i in range(n_param):
            java_suggestions_all.append([])
            for j in range(len(data['next_lex'][i])):
                java_suggestions_all[i].append(java_tokenize_sentences(data['next_lex'][i][j],
                                                                       tokenizer=self.java_tokenizer))
        java_context = [([], 0, x, []) for x in range(0, len(excode_context))]
        for j in range(n_param):
            x_test_all = []
            y_test_all = []
            sentence_len_all = []
            for k in range(len(java_context)):
                i = java_context[k][2]
                java_suggestions = java_suggestions_all[j][excode_context[i][1][j]]
                curr_context = java_origin_context + java_context[k][0]
                x_test, y_test, sentence_len = prepare_sentences(curr_context,
                                                                 java_suggestions, self.train_len,
                                                                 len(curr_context))
                x_test_all += x_test.tolist()
                y_test_all += y_test.tolist()
                sentence_len_all += sentence_len
            x_test_all = np.array(x_test_all)
            y_test_all = np.array(y_test_all)
            p_pred = predict(self.java_model, x_test_all,
                             method_name_tokens=method_name_tokens_java,
                             class_name_tokens=class_name_tokens_java)
            log_p_sentence = evaluate(p_pred, y_test_all, sentence_len_all)
            counter = 0
            java_suggestion_scores = []
            for k in range(len(java_context)):
                i = java_context[k][2]
                java_suggestions = java_suggestions_all[j][excode_context[i][1][j]]
                for ii, java_suggestion in enumerate(java_suggestions):
                    new_context = java_context[k][0] + java_suggestion
                    if j < n_param - 1:
                        new_context += java_comma_id
                    java_suggestion_scores.append((new_context,
                                                   log_p_sentence[counter],
                                                   i,
                                                   java_context[k][3] + [(excode_context[i][1][j], ii)]))
                    counter += 1
            sorted_scores = sorted(java_suggestion_scores, key=lambda x: -x[1])[:self.max_keep_step[j]]
            java_context = sorted_scores
        return self.select_top_candidates(java_context, data, start_time)

    def select_top_candidates(self, java_context, data, start_time):
        sorted_scores = sorted(java_context, key=lambda x: -x[1])[:self.top_k]
        result_rnn = []
        for i in range(min(self.top_k, len(sorted_scores))):
            result_rnn.append(sorted_scores[i][3])
        runtime_rnn = perf_counter() - start_time
        self.logger.debug("Total rnn runtime: " + str(runtime_rnn))
        result_rnn = self.recreate(result_rnn, data)
        self.logger.debug("Result rnn:\n", result_rnn)
        response = 'result:' + json.dumps(result_rnn) + ',runtime:' + str(runtime_rnn)
        return response

    def predict_method_name_using_lex(self, data):
        # using lex
        start_time = perf_counter()
        method_candidate_lex, java_context = self.prepare_method_name_prediction(data)
        if len(method_candidate_lex) == 0:
            return 'result:[],runtime:"0"'
        if data['method_name'] != "":
            method_name = tokenize(data['method_name'])
        else:
            method_name = "<UNK>"
        class_name = tokenize(data['class_name'])
        method_name_tokens_java = self.java_tokenizer.texts_to_sequences([method_name])[0]
        class_name_tokens_java = self.java_tokenizer.texts_to_sequences([class_name])[0]
        java_suggestions = java_tokenize_sentences(method_candidate_lex,
                                                   tokenizer=self.java_tokenizer,
                                                   to_sequence=True)
        x_test, y_test, sentence_len = prepare_sentences(java_context,
                                                         java_suggestions, self.train_len,
                                                         len(java_context))
        p_pred = predict(self.java_model, x_test,
                         method_name_tokens=method_name_tokens_java,
                         class_name_tokens=class_name_tokens_java)
        log_p_sentence = evaluate(p_pred, y_test, sentence_len)
        java_suggestion_scores = []
        for i in range(len(log_p_sentence)):
            java_suggestion_scores.append((i, log_p_sentence[i]))
        return self.select_top_method_name_candidates(java_suggestion_scores, method_candidate_lex, start_time)

    def predict_method_name_using_cfg(self, data):
        start_time = perf_counter()
        contexts = []
        candidates = []
        for method_context in data['method_context']:
            context = extract_method_call_from_cfg_string(method_context)
            contexts += self.method_call_tokenizer.texts_to_sequences([context])
        for candidate in data['next_lex']:
            candidates += self.method_call_tokenizer.texts_to_sequences([[candidate]])

        x_test_all = []
        y_test_all = []
        sentence_len_all = []
        for i in range(len(candidates)):
            accumulate_len = 0
            for method_context in contexts:
                x_test, y_test, sentence_len = prepare_sentences(method_context,
                                                                 [candidates[i]],
                                                                 self.train_len,
                                                                 len(method_context))
                x_test_all += x_test.tolist()
                y_test_all += y_test.tolist()
                accumulate_len += sum(sentence_len)
            sentence_len_all.append(accumulate_len)
        x_test_all = np.array(x_test_all)
        y_test_all = np.array(y_test_all)
        p_pred = predict(self.method_call_model, x_test_all)
        log_p_sentence = evaluate(p_pred, y_test_all, sentence_len_all)
        for i in range(len(log_p_sentence)):
            log_p_sentence[i] = (i, log_p_sentence[i])
        return self.select_top_method_name_candidates(log_p_sentence, data['next_lex'], start_time)
