import dill
import logging

import sys
from pickle import load
from model.utility import *
from model.config import *


class ModelManager:
    def __init__(self, top_k, project, train_len,
                 excode_model_path, java_model_path,
                 excode_tokenizer_path, java_tokenizer_path,
                 excode_tokens_path):
        with open(excode_model_path, 'rb') as fin:
            self.excode_model = dill.load(fin)
        with open(java_model_path, 'rb') as fin:
            self.java_model = dill.load(fin)
        self.project = project
        self.top_k = top_k
        self.train_len = train_len
        self.logger = logging.getLogger()
        self.logger.disabled = not PRINT_LOG
        self.logger.setLevel(logging.DEBUG)

        # output_file_handler = logging.FileHandler("output.log")
        # logger.addHandler(output_file_handler)
        stdout_handler = logging.StreamHandler(sys.stdout)
        self.logger.addHandler(stdout_handler)
        self.excode_tokenizer = load(open(excode_tokenizer_path, 'rb'))
        self.java_tokenizer = load(open(java_tokenizer_path, 'rb'))
        self.excode_tokens = read_file(excode_tokens_path).lower().split("\n")
        self.max_keep_step = [10, 10, 10, 10, 10, 10, 10, 10, 10, 10]
        self.lexsim_flag = USE_LEXSIM

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

    def set_lexsim_flag(self, value):
        self.lexsim_flag = value

    def use_lexsim_flag(self):
        return self.lexsim_flag

    def score_lexsim(self, lexsim):
        # lexsim=0
        if abs(lexsim) < 1e-6:
            return -1 * LEXSIM_MULTIPLIER
        # lexsim>0, <0.4
        if lexsim < 0.4:
            return -0.8 * LEXSIM_MULTIPLIER
        # lexsim>0.4, <0.5
        if lexsim + 1e-6 < 0.5:
            return -0.6 * LEXSIM_MULTIPLIER
        if abs(lexsim - 0.5) < 1e-6:
            # lexsim=0.5
            return -0.5 * LEXSIM_MULTIPLIER
        # lexsim>0.5, <0.66
        if lexsim < 0.66:
            return -0.4 * LEXSIM_MULTIPLIER
        # lexsim>0.66, <0.67
        if lexsim < 0.67:
            return -0.3 * LEXSIM_MULTIPLIER
        # lexsim>0.67, <1
        if lexsim + 1e-6 < 1:
            return -0.2 * LEXSIM_MULTIPLIER
        else:
            # lexsim=1
            return 0 * LEXSIM_MULTIPLIER
