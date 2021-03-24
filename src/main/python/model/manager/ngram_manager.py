from model.java.java_preprocess import java_tokenize_sentences
from model.excode.excode_preprocess import excode_tokenize, excode_tokenize_candidates
from model.method_call.preprocessing import extract_method_call_from_cfg_string
import numpy as np
from model.ngram_predictor import score_ngram
from model.manager.model_manager import *
from model.config import *
from name_stat.similarly import lexSim
from name_stat.name_tokenizer import tokenize
import copy


class NgramManager(ModelManager):
    def __init__(self, top_k, project, train_len,
                 excode_model_path, java_model_path, method_call_model_path):
        super().__init__(top_k, project, train_len,
                         excode_model_path, java_model_path, method_call_model_path)

    def process(self, data, service):
        response = "ngram:{"
        if service == "param":
            response += self.predict_param(data)
        elif service == "method_name":
            response += self.predict_method_name_using_cfg(data)
        return response + "}"

    def predict_param(self, data):
        if PARAM_LEXICAL_ONLY:
            return self.predict_param_using_lex(data)
        else:
            return self.predict_param_all_features(data)

    def tokenize_from_str(self, string):
        tokens = []
        for candidate_word in string.split():
            tokens += tokenize(candidate_word)
        return tokens

    def tokenize_context_str(self, context):
        tokens = []
        for word in context:
            tokens += self.tokenize_from_str(word)
        return tokens

    def predict_param_all_features(self, data):
        start_time = perf_counter()
        # excode_context = excode_tokenize(data['excode_context'],
        #                                  tokenizer=self.excode_tokenizer,
        #                                  train_len=self.train_len,
        #                                  tokens=self.excode_tokens,
        #                                  method_content_only=False)[0]
        # excode_context_textform = self.excode_tokenizer.sequences_to_texts([excode_context])[0].split()[-self.ngram:]
        # excode_context_textform = [(excode_context_textform, [])]
        excode_context_textform = [(data['excode_context'].split(' ')[-NGRAM_EXCODE_PARAM:], [])]
        n_param = len(data['next_excode'])
        for p_id in range(n_param):
            # excode_suggestions = excode_tokenize_candidates(data['next_excode'][p_id],
            #                                                 tokenizer=self.excode_tokenizer,
            #                                                 tokens=self.excode_tokens)
            # excode_suggestions_textforms = self.excode_tokenizer.sequences_to_texts(excode_suggestions)
            excode_suggestions_textforms = [x for x in data['next_excode'][p_id]]
            excode_suggestion_scores = []
            for ex_suggest_id in range(len(excode_context_textform)):
                for i, excode_suggestions_textform in enumerate(excode_suggestions_textforms):
                    sentence = excode_context_textform[ex_suggest_id][0] + excode_suggestions_textform.split()
                    if p_id < n_param - 1:
                        sentence += ['SEPA(,)']
                        # be careful if lower case in training data
                        # sentence += ['sepa(,)']
                    model_score = score_ngram(model=self.excode_model,
                                              sentence=sentence,
                                              n=NGRAM_EXCODE_PARAM,
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
            if p_id > 0:
                sorted_scores = sorted(excode_suggestion_scores, key=lambda x: -x[2])[:self.max_keep_step[p_id]]
            else:
                sorted_scores = sorted(excode_suggestion_scores, key=lambda x: -x[2])
            # excode_context_textform = [(x[0], x[1]) for x in sorted_scores]
            excode_context_textform = sorted_scores
        # logger.debug(sorted_scores)
        # logger.debug('-----------------------------\n-----------------------------\n-----------------------------')
        # logger.debug("Best excode suggestion(s):")

        # java_context = java_tokenize_take_last(data['lex_context'],
        #                                        tokenizer=self.java_tokenizer,
        #                                        train_len=self.train_len)
        # java_suggestions_all = np.array(copy.deepcopy(data['next_lex']), dtype=object)
        # for i in range(n_param):
        #     for j in range(len(java_suggestions_all[i])):
        #         java_suggestions_all[i][j] = java_tokenize_sentences(data['next_lex'][i][j],
        #                                                              tokenizer=self.java_tokenizer,
        #                                                              to_sequence=False)

        java_context = self.tokenize_context_str(data['lex_context'])
        java_suggestions_all = []
        for i in range(n_param):
            java_suggestions_all.append([])
            for j in range(len(data['next_lex'][i])):
                java_suggestions_all[i].append([])
                for k in range(len(data['next_lex'][i][j])):
                    candidate_tokens = self.tokenize_from_str(data['next_lex'][i][j][k])
                    java_suggestions_all[i][j].append(candidate_tokens)
        all_candidate_lex = []
        for i in range(len(excode_context_textform)):
            # java_context_list = self.java_tokenizer.sequences_to_texts([java_context])[0].split()[-NGRAM_LEXICAL_PARAM:]
            java_context_list = [(java_context, [])]
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
                                                  n=NGRAM_LEXICAL_PARAM,
                                                  start_pos=len(java_context_list[k][0])) \
                                    + excode_context_textform[i][2]
                        # if USE_LEXSIM and is_not_empty_list(data['param_list']) \
                        #         and self.is_valid_param(data['param_list'][j]):
                        if USE_LEXSIM and 'param_name' in data.keys() \
                                and self.is_valid_param(data['param_name']):
                            # self.logger.log(java_suggestion, data['next_lex'][j][excode_context_textform[i][1][j]][
                            # ii])
                            # lexsim = lexSim(data['param_list'][j],
                            #                 data['next_lex'][j][excode_context_textform[i][1][j]][ii])
                            lexsim = lexSim(data['param_name'],
                                            data['next_lex'][j][excode_context_textform[i][1][j]][ii])
                            model_score = self.score_lexsim(lexsim) + model_score * NGRAM_SCORE_WEIGHT
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

    def predict_param_using_lex(self, data):
        start_time = perf_counter()
        n_param = len(data['next_lex'])
        # java_context = java_tokenize_take_last(data['lex_context'],
        #                                        tokenizer=self.java_tokenizer,
        #                                        train_len=self.train_len)
        # java_suggestions_all = np.array(copy.deepcopy(data['next_lex']), dtype=object)
        # for i in range(n_param):
        #     for j in range(len(java_suggestions_all[i])):
        #         java_suggestions_all[i][j] = java_tokenize_sentences(data['next_lex'][i][j],
        #                                                              tokenizer=self.java_tokenizer,
        #                                                              to_sequence=False)
        # all_candidate_lex = []
        # java_context_list = self.java_tokenizer.sequences_to_texts([java_context])[0].split()
        # java_context_list = [(java_context_list, [])]
        java_context = self.tokenize_context_str(data['lex_context'])
        java_suggestions_all = []
        for i in range(n_param):
            java_suggestions_all.append([])
            for j in range(len(data['next_lex'][i])):
                java_suggestions_all[i].append([])
                for k in range(len(data['next_lex'][i][j])):
                    candidate_tokens = self.tokenize_from_str(data['next_lex'][i][j][k])
                    java_suggestions_all[i][j].append(candidate_tokens)
        java_context_list = [(java_context, [])]
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
                        model_score = score_ngram(model=self.java_model,
                                                  sentence=new_context,
                                                  n=NGRAM_LEXICAL_PARAM,
                                                  start_pos=len(java_context_list[k][0]))
                        if USE_LEXSIM and 'param_name' in data.keys() \
                                and self.is_valid_param(data['param_name']):
                            lexsim = lexSim(data['param_name'],
                                            data['next_lex'][j][jj][ii])
                            model_score = self.score_lexsim(lexsim) + model_score * NGRAM_SCORE_WEIGHT
                            if USE_LOCAL_VAR and data['is_local_var'][j][jj][ii]:
                                model_score = model_score + LOCAL_VAR_BONUS
                        java_suggestion_scores.append((new_context, java_context_list[k][1]
                                                       + [(jj, ii)], model_score))
            sorted_scores = sorted(java_suggestion_scores, key=lambda x: -x[2])
            if j < n_param - 1:
                java_context_list = [(x[0], x[1]) for x in sorted_scores]
            else:
                java_context_list = sorted_scores
        all_candidate_lex += java_context_list
        return self.select_top_param_candidates(all_candidate_lex, data, start_time)

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

    def predict_method_name_using_lex(self, data):
        start_time = perf_counter()
        method_candidate_lex, java_context = self.prepare_method_name_prediction(data)
        java_context = self.java_tokenizer.sequences_to_texts([java_context])[0].split()[-NGRAM_LEXICAL_METHODCALL:]
        java_suggestions = java_tokenize_sentences(method_candidate_lex,
                                                   tokenizer=self.java_tokenizer,
                                                   to_sequence=False)
        java_suggestion_scores = []
        for i in range(len(java_suggestions)):
            model_score = score_ngram(model=self.java_model,
                                      sentence=java_context + java_suggestions[i],
                                      n=NGRAM_LEXICAL_METHODCALL,
                                      start_pos=len(java_context))
            java_suggestion_scores.append((i, model_score))
        return self.select_top_method_name_candidates(java_suggestion_scores, method_candidate_lex, start_time)

    def predict_method_name_using_excode(self, data):
        start_time = perf_counter()
        excode_context = excode_tokenize(data['excode_context'],
                                         tokenizer=self.excode_tokenizer,
                                         train_len=self.train_len,
                                         tokens=self.excode_tokens,
                                         method_content_only=False)[0]

        excode_context_textform = self.excode_tokenizer.sequences_to_texts([excode_context])[0].split()[
                                  -NGRAM_EXCODE_METHODCALL:]
        excode_suggestions = excode_tokenize_candidates(data['method_candidate_excode'],
                                                        tokenizer=self.excode_tokenizer,
                                                        tokens=self.excode_tokens)
        excode_suggestions_textforms = self.excode_tokenizer.sequences_to_texts(excode_suggestions)
        excode_suggestion_scores = []
        for i, excode_suggestions_textform in enumerate(excode_suggestions_textforms):
            sentence = excode_context_textform + excode_suggestions_textform.split()
            model_score = score_ngram(model=self.excode_model,
                                      sentence=sentence,
                                      n=NGRAM_EXCODE_METHODCALL,
                                      start_pos=len(excode_context_textform))
            excode_suggestion_scores.append((i, model_score))
        sorted_scores = sorted(excode_suggestion_scores, key=lambda x: -x[1])[:self.top_k]
        best_candidates_index = [x[0] for x in sorted_scores]
        best_candidates = []
        for x in best_candidates_index:
            best_candidates.append(data['method_candidate_excode'][x])
        print(best_candidates)
        return 'result:' + json.dumps(best_candidates) \
               + ',runtime:' + str(perf_counter() - start_time)

    def predict_method_name_using_cfg(self, data):
        start_time = perf_counter()
        method_suggestion_scores = []
        for i in range(len(data['next_lex'])):
            model_score = 0
            for method_context in data['method_context']:
                sentence = method_context + ' ' + data['next_lex'][i]
                sentence = extract_method_call_from_cfg_string(sentence)
                power = score_ngram(model=self.method_call_model,
                                    sentence=sentence,
                                    n=NGRAM_LEXICAL_METHODCALL,
                                    start_pos=0)
                model_score += math.pow(2, power)
            method_suggestion_scores.append((i, model_score))
        return self.select_top_method_name_candidates(method_suggestion_scores, data['next_lex'], start_time)
