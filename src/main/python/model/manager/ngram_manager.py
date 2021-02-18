import json
from model.java.java_preprocess import java_tokenize_take_last, java_tokenize_sentences
from model.excode.excode_preprocess import excode_tokenize, excode_tokenize_candidates
from time import perf_counter
import numpy as np
from model.ngram_predictor import score_ngram
from model.manager.model_manager import *
from model.config import *
from name_stat.similarly import lexSim
import copy


class NgramManager(ModelManager):
    def __init__(self, top_k, project, train_len, ngram,
                 excode_model_path, java_model_path,
                 excode_tokenizer_path, java_tokenizer_path,
                 excode_tokens_path):
        super().__init__(top_k, project, train_len,
                         excode_model_path, java_model_path,
                         excode_tokenizer_path, java_tokenizer_path,
                         excode_tokens_path)
        self.ngram = ngram

    def process(self, data, service):
        response = "ngram:{"
        if service == "param":
            response += self.predict_param(data)
        elif service == "method_name":
            response += self.predict_method_name(data)
        return response + "}"

    def predict_param(self, data):
        start_time = perf_counter()
        excode_context = excode_tokenize(data['excode_context'],
                                         tokenizer=self.excode_tokenizer,
                                         train_len=self.train_len,
                                         tokens=self.excode_tokens,
                                         method_content_only=False)[0]
        n_param = len(data['next_excode'])

        excode_context_textform = self.excode_tokenizer.sequences_to_texts([excode_context])[0].split()[-self.ngram:]
        excode_context_textform = [(excode_context_textform, [])]
        for p_id in range(n_param):
            excode_suggestions = excode_tokenize_candidates(data['next_excode'][p_id],
                                                            tokenizer=self.excode_tokenizer,
                                                            tokens=self.excode_tokens)
            excode_suggestions_textforms = self.excode_tokenizer.sequences_to_texts(excode_suggestions)
            excode_suggestion_scores = []
            for ex_suggest_id in range(len(excode_context_textform)):
                for i, excode_suggestions_textform in enumerate(excode_suggestions_textforms):
                    sentence = excode_context_textform[ex_suggest_id][0] + excode_suggestions_textform.split()
                    if p_id < n_param - 1:
                        sentence += ['sepa(,)']
                    model_score = score_ngram(model=self.excode_model,
                                              sentence=sentence,
                                              n=self.ngram,
                                              start_pos=len(excode_context_textform[ex_suggest_id][0]))
                    # if data['param_list'][p_id] == 'null':
                    #     pass
                    # else:
                    #     lexsim = lexSim(data['param_list'][p_id], data['next_excode'][p_id][ex_suggest_id])
                    # lexsim = lexSim('a', 'b')
                    # model_score = lexsim_weight * lexsim + model_weight * model_score
                    excode_suggestion_scores.append((sentence,
                                                     excode_context_textform[ex_suggest_id][1] + [i],
                                                     model_score))
            sorted_scores = sorted(excode_suggestion_scores, key=lambda x: -x[2])[:self.max_keep_step[p_id]]
            excode_context_textform = [(x[0], x[1]) for x in sorted_scores]
        # logger.debug(sorted_scores)
        # logger.debug('-----------------------------\n-----------------------------\n-----------------------------')
        # logger.debug("Best excode suggestion(s):")
        # for i in range(min(self.top_k, len(excode_context_textform))):
        #     if expected_excode_text == excode_context_textform[i][0][self.ngram:]:
        #         ngram_excode_correct[i] += 1
        java_context = java_tokenize_take_last(data['lex_context'],
                                               tokenizer=self.java_tokenizer,
                                               train_len=self.train_len)
        java_suggestions_all = np.array(copy.deepcopy(data['next_lex']), dtype=object)
        for i in range(n_param):
            for j in range(len(java_suggestions_all[i])):
                java_suggestions_all[i][j] = java_tokenize_sentences(data['next_lex'][i][j],
                                                                     tokenizer=self.java_tokenizer,
                                                                     to_sequence=False)
        all_candidate_lex = []
        for i in range(len(excode_context_textform)):
            java_context_list = self.java_tokenizer.sequences_to_texts([java_context])[0].split()[-self.ngram:]
            java_context_list = [(java_context_list, [])]
            for j in range(n_param):
                java_suggestion_scores = []
                for k in range(len(java_context_list)):
                    java_suggestions = java_suggestions_all[j][excode_context_textform[i][1][j]]
                    for ii, java_suggestion in enumerate(java_suggestions):
                        new_context = java_context_list[k][0] + java_suggestion
                        if j < n_param - 1:
                            new_context += [',']
                        model_score = score_ngram(model=self.java_model,
                                                  sentence=new_context,
                                                  n=self.ngram,
                                                  start_pos=len(java_context_list[k][0]))
                        if USE_LEXSIM and is_not_empty_list(data['param_list']) \
                                and self.is_valid_param(data['param_list'][j]):
                            # self.logger.log(java_suggestion, data['next_lex'][j][excode_context_textform[i][1][j]][
                            # ii])
                            lexsim = lexSim(data['param_list'][j],
                                            data['next_lex'][j][excode_context_textform[i][1][j]][ii])
                            model_score = self.score_lexsim(lexsim) + model_score * NGRAM_WEIGHT
                            if USE_LOCAL_VAR and data['is_local_var'][j][excode_context_textform[i][1][j]][ii]:
                                model_score = model_score + LOCAL_VAR_BONUS
                        java_suggestion_scores.append((new_context, java_context_list[k][1]
                                                       + [(excode_context_textform[i][1][j], ii)], model_score))
                sorted_scores = sorted(java_suggestion_scores, key=lambda x: -x[2])
                if j < n_param - 1:
                    java_context_list = [(x[0], x[1]) for x in sorted_scores]
                else:
                    java_context_list = sorted_scores
            all_candidate_lex += java_context_list
        return self.select_top_param_candidates(all_candidate_lex, data, start_time)
        # logger.debug(sorted_scores)
        # logger.debug('-----------------------------\n-----------------------------\n-----------------------------')
        # logger.debug("Best java suggestion(s):")

    def select_top_param_candidates(self, all_candidate_lex, data, start_time):
        sorted_scores = sorted(all_candidate_lex, key=lambda x: -x[2])
        result_ngram = []
        for i in range(min(self.top_k, len(sorted_scores))):
            result_ngram.append(sorted_scores[i][1])
        runtime_ngram = perf_counter() - start_time
        self.logger.debug("Total n-gram runtime: " + str(runtime_ngram))
        result_ngram = self.recreate(result_ngram, data)
        self.logger.debug("Result ngram: " + json.dumps(result_ngram))
        response = 'result:' + json.dumps(result_ngram) \
                   + ',runtime:' + str(runtime_ngram)
        return response

    def predict_method_name(self, data):
        # using lex only
        start_time = perf_counter()
        method_candidate_lex = []
        for method_candidate_excode in data['method_candidate_excode']:
            method_candidate_lex.append(method_candidate_excode.split(',')[1])
        method_candidate_lex = list(set(method_candidate_lex))
        java_context = java_tokenize_take_last(data['lex_context'],
                                               tokenizer=self.java_tokenizer,
                                               train_len=self.train_len)
        java_context = self.java_tokenizer.sequences_to_texts([java_context])[0].split()[-self.ngram:]
        java_suggestions = java_tokenize_sentences(method_candidate_lex,
                                                   tokenizer=self.java_tokenizer,
                                                   to_sequence=False)
        java_suggestion_scores = []
        for i in range(len(java_suggestions)):
            model_score = score_ngram(model=self.java_model,
                                      sentence=java_context + java_suggestions[i],
                                      n=self.ngram,
                                      start_pos=len(java_context))
            java_suggestion_scores.append((i, model_score))
        sorted_scores = sorted(java_suggestion_scores, key=lambda x: -x[1])[:self.top_k]
        best_candidates_index = [x[0] for x in sorted_scores]
        best_candidates = []
        for i in best_candidates_index:
            best_candidates.append(method_candidate_lex[i])
        print(best_candidates)
        return 'result:' + json.dumps(best_candidates) \
               + ',runtime:' + str(perf_counter() - start_time)

        # using excode only
        # start_time = perf_counter()
        # excode_context = excode_tokenize(data['excode_context'],
        #                                  tokenizer=self.excode_tokenizer,
        #                                  train_len=self.train_len,
        #                                  tokens=self.excode_tokens,
        #                                  method_content_only=False)[0]
        #
        # excode_context_textform = self.excode_tokenizer.sequences_to_texts([excode_context])[0].split()[-self.ngram:]
        # excode_suggestions = excode_tokenize_candidates(data['method_candidate_excode'],
        #                                                 tokenizer=self.excode_tokenizer,
        #                                                 tokens=self.excode_tokens)
        # excode_suggestions_textforms = self.excode_tokenizer.sequences_to_texts(excode_suggestions)
        # excode_suggestion_scores = []
        # for i, excode_suggestions_textform in enumerate(excode_suggestions_textforms):
        #     sentence = excode_context_textform + excode_suggestions_textform.split()
        #     model_score = score_ngram(model=self.excode_model,
        #                               sentence=sentence,
        #                               n=self.ngram,
        #                               start_pos=len(excode_context_textform))
        #     excode_suggestion_scores.append((i, model_score))
        # sorted_scores = sorted(excode_suggestion_scores, key=lambda x: -x[1])[:self.top_k]
        # best_candidates_index = [x[0] for x in sorted_scores]
        # best_candidates = []
        # for x in best_candidates_index:
        #     best_candidates.append(data['method_candidate_excode'][x])
        # print(best_candidates)
        # return 'result:' + json.dumps(best_candidates) \
        #        + ',runtime:' + str(perf_counter() - start_time)
