import json

from time import perf_counter

import dill
import logging

import sys
from pickle import load

from model.java.java_preprocess import java_tokenize_take_last
from model.utility import *
from model.config import USE_JAVA_MODEL, USE_EXCODE_MODEL, USE_METHOD_CALL_MODEL, \
    PRINT_LOG, USE_LEXSIM, LEXSIM_MULTIPLIER, LEXSIM_SMALL_PENALTY,\
    excode_tokenizer_path, excode_tokens_path, java_tokenizer_path, method_call_tokenizer_path
import math
import keras

# Param score = model_score * lexsim(optional) * local_var_bonus(optional)
# Method call score = avg(model_score of (sequence[i]+candidate), for all context sequence)


class ModelManager:
    def __init__(self, top_k, project, train_len,
                 excode_model_path, java_model_path, method_call_model_path):
        if USE_EXCODE_MODEL:
            self.excode_model = self.load_model(excode_model_path)
            self.excode_tokenizer = load(open(excode_tokenizer_path, 'rb'))
            self.excode_tokens = read_file(excode_tokens_path).lower().split("\n")
        if USE_JAVA_MODEL:
            self.java_model = self.load_model(java_model_path)
            self.java_tokenizer = load(open(java_tokenizer_path, 'rb'))
        if USE_METHOD_CALL_MODEL:
            self.method_call_model = self.load_model(method_call_model_path)
            self.method_call_tokenizer = load(open(method_call_tokenizer_path, 'rb'))
        self.project = project
        self.top_k = top_k
        self.train_len = train_len
        self.logger = logging.getLogger()
        self.logger.setLevel(logging.DEBUG)
        # output_file_handler = logging.FileHandler("output.log")
        # logger.addHandler(output_file_handler)
        self.logger.disabled = not PRINT_LOG
        stdout_handler = logging.StreamHandler(sys.stdout)
        self.logger.addHandler(stdout_handler)
        self.max_keep_step = [10, 10, 10, 10, 10, 10, 10, 10, 10, 10]
        self.lexsim_flag = USE_LEXSIM

    def load_model(self, model_path):
        if model_path[-2:] == 'h5':
            model = keras.models.load_model(model_path)
            return model
        else:
            with open(model_path, 'rb') as fin:
                return dill.load(fin)

    def recreate(self, result, data):
        origin = []
        for candidate_ids in result:
            candidate_text = ""
            for i in range(len(candidate_ids)):
                candidate_text += data['next_lex'][i][candidate_ids[i][0]][candidate_ids[i][1]]
                if i < len(candidate_ids) - 1:
                    candidate_text += ", "
            origin.append(candidate_text)
        return origin

    def is_valid_param(self, param):
        invalid_phrases = ['null', '', 'true', 'false', '0']
        return param not in invalid_phrases

    def score_lexsim(self, lexsim):
        #1
        # return lexsim * LEXSIM_MULTIPLIER

        #2
        # special case: lexsim could rarely be <= 0.1
        if lexsim < 0.1:
            return LEXSIM_SMALL_PENALTY * LEXSIM_MULTIPLIER
        else:
            return math.log2(lexsim) * LEXSIM_MULTIPLIER

    def prepare_method_name_prediction(self, data):
        method_candidate_lex = []
        for method_candidate_excode in data['method_candidate_excode']:
            method_candidate_lex.append(method_candidate_excode.split(',')[1])
        method_candidate_lex = list(set(method_candidate_lex))
        java_context = java_tokenize_take_last(data['lex_context'],
                                               tokenizer=self.java_tokenizer,
                                               train_len=self.train_len)
        return method_candidate_lex, java_context

    def select_top_method_name_candidates(self, suggestion_scores, method_candidate_lex, start_time):
        sorted_scores = sorted(suggestion_scores, key=lambda x: -x[1])[:self.top_k]
        best_candidates_index = [x[0] for x in sorted_scores]
        best_candidates = []
        for i in best_candidates_index:
            best_candidates.append(method_candidate_lex[i])
        print(best_candidates)
        return 'result:' + json.dumps(best_candidates) \
               + ',runtime:' + str(perf_counter() - start_time)
