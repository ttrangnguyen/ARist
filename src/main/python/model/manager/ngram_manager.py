import json
from model.java.java_preprocess import java_tokenize_take_last, java_tokenize_sentences
from model.excode.excode_preprocess import excode_tokenize, excode_tokenize_candidates
from time import perf_counter
from pickle import load
import numpy as np
import dill
from model.ngram_predictor import score_ngram
import logging
import sys
import copy
from model.utility import *
from model.manager.model_manager import *


class NgramManager(ModelManager):
    def __init__(self, top_k, project, train_len, ngram,
                 excode_model_ngram_path, java_model_ngram_path,
                 excode_tokenizer_path, java_tokenizer_path,
                 excode_tokens_path):
        with open(excode_model_ngram_path, 'rb') as fin:
            self.excode_model_ngram = dill.load(fin)
        with open(java_model_ngram_path, 'rb') as fin:
            self.java_model_ngram = dill.load(fin)
        self.top_k = top_k
        self.train_len = train_len
        self.ngram = ngram
        self.logger = logging.getLogger()
        # logger.disabled = True
        self.logger.setLevel(logging.DEBUG)

        # output_file_handler = logging.FileHandler("output.log")
        # logger.addHandler(output_file_handler)
        stdout_handler = logging.StreamHandler(sys.stdout)
        self.logger.addHandler(stdout_handler)
        self.excode_tokenizer = load(open(excode_tokenizer_path, 'rb'))
        self.java_tokenizer = load(open(java_tokenizer_path, 'rb'))
        self.excode_tokens = read_file(excode_tokens_path).lower().split("\n")
        self.max_keep_step = [10, 10, 10, 10, 10, 10, 10, 10, 10, 10]

    def work(self, data):
        start_time = perf_counter()
        origin_data = copy.deepcopy(data)
        excode_context = excode_tokenize(data['excode_context'],
                                         tokenizer=self.excode_tokenizer,
                                         train_len=self.train_len,
                                         tokens=self.excode_tokens,
                                         method_content_only=False)[0]
        java_context = java_tokenize_take_last(data['lex_context'],
                                               tokenizer=self.java_tokenizer,
                                               train_len=self.train_len)
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
                    score = score_ngram(model=self.excode_model_ngram,
                                        sentence=sentence,
                                        n=self.ngram,
                                        start_pos=len(excode_context_textform[ex_suggest_id][0]))
                    excode_suggestion_scores.append((sentence,
                                                     excode_context_textform[ex_suggest_id][1] + [i],
                                                     score))
            sorted_scores = sorted(excode_suggestion_scores, key=lambda x: -x[2])[:self.max_keep_step[p_id]]
            excode_context_textform = [(x[0], x[1]) for x in sorted_scores]

        # logger.debug(sorted_scores)
        # logger.debug('-----------------------------\n-----------------------------\n-----------------------------')
        # logger.debug("Best excode suggestion(s):")
        # for i in range(min(self.top_k, len(excode_context_textform))):
        #     if expected_excode_text == excode_context_textform[i][0][self.ngram:]:
        #         ngram_excode_correct[i] += 1
        java_suggestions_all = np.array(data['next_lex'], dtype=object)
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
                        score = score_ngram(model=self.java_model_ngram,
                                            sentence=new_context,
                                            n=self.ngram,
                                            start_pos=len(java_context_list[k][0]))
                        java_suggestion_scores.append((new_context, java_context_list[k][1]
                                                       + [(excode_context_textform[i][1][j], ii)], score))
                sorted_scores = sorted(java_suggestion_scores, key=lambda x: -x[2])
                if j < n_param - 1:
                    java_context_list = [(x[0], x[1]) for x in sorted_scores]
                else:
                    java_context_list = sorted_scores
            all_candidate_lex += java_context_list
        return self.select_top_candidates(all_candidate_lex, origin_data, start_time)
        # logger.debug(sorted_scores)
        # logger.debug('-----------------------------\n-----------------------------\n-----------------------------')
        # logger.debug("Best java suggestion(s):")

    def select_top_candidates(self, all_candidate_lex, origin_data, start_time):
        sorted_scores = sorted(all_candidate_lex, key=lambda x: -x[2])
        result_ngram = []
        for i in range(min(self.top_k, len(sorted_scores))):
            result_ngram.append(sorted_scores[i][1])
        runtime_ngram = perf_counter() - start_time
        self.logger.debug("Total n-gram runtime: " + str(runtime_ngram))
        result_ngram = self.recreate(result_ngram, origin_data)
        print("Result ngram:\n", result_ngram)

        response = 'ngram:{' \
                   + 'result:' + json.dumps(result_ngram) \
                   + ',runtime:' + str(runtime_ngram) \
                   + '}'
        return response
