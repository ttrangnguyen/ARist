import socket
import json
from model.java.java_preprocess import java_tokenize_take_last, java_tokenize_sentences
from model.excode.excode_preprocess import excode_tokenize, excode_tokenize_candidates
from name_stat.name_tokenizer import tokenize
from keras.models import load_model
from model.predictor import prepare, predict, evaluate
from time import perf_counter
from pickle import load
import numpy as np
import dill
from model.ngram_predictor import score_ngram
from os import path
import csv
from _thread import *
import threading
import logging
import sys
import copy
import os, glob
from pathlib import Path



def read_file(filepath):
    with open(filepath) as f:
        str_text = f.read()
    return str_text


def recvall(sock):
    BUFF_SIZE = 1024
    fragments = []
    while True:
        chunk = sock.recv(BUFF_SIZE)

        fragments.append(chunk)

        if (not chunk) or chunk[-1] == 255:
            break
    arr = b''.join(fragments)
    return arr[:-1]


serv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
serv.bind(('0.0.0.0', 18007))
serv.listen(10)

project = 'all'
USE_RNN = True

excode_model_rnn = load_model("../../../../../model/excode_model_" + project + ".h5")
with open("../../../../../model/excode_model_" + project + "ngram.pkl", 'rb') as fin:
    excode_model_ngram = dill.load(fin)
java_model_rnn = load_model("../../../../../model/java_model_" + project + ".h5")
with open("../../../../../model/java_model_" + project + "ngram.pkl", 'rb') as fin:
    java_model_ngram = dill.load(fin)
excode_tokenizer = load(open('../../../../src/main/python/model/excode/excode_tokenizer', 'rb'))
java_tokenizer = load(open('../../../../src/main/python/model/java/java_tokenizer', 'rb'))
excode_tokens = read_file('../../../../data_dict/excode/excode_tokens_n_symbols.txt').lower().split("\n")
train_len = 6 + 1
ngram = 2 + 1
top_k_excode = 100
top_k = 10
max_keep_step = [10, 10, 10, 10, 10, 10, 10, 10, 10, 10]

ngram_excode_correct = [0] * top_k
ngram_lex_correct = [0] * top_k
rnn_excode_correct = [0] * top_k
rnn_lex_correct = [0] * top_k

logger = logging.getLogger()
# logger.disabled = True
logger.setLevel(logging.DEBUG)

# output_file_handler = logging.FileHandler("output.log")
# logger.addHandler(output_file_handler)
stdout_handler = logging.StreamHandler(sys.stdout)
logger.addHandler(stdout_handler)


def write_result(project, clientId, ngram_excode_correct, ngram_lex_correct, rnn_excode_correct, rnn_lex_correct,
                 total_guesses):
    for i in range(1, top_k):
        ngram_excode_correct[i] += ngram_excode_correct[i - 1]
        ngram_lex_correct[i] += ngram_lex_correct[i - 1]
        rnn_excode_correct[i] += rnn_excode_correct[i - 1]
        rnn_lex_correct[i] += rnn_lex_correct[i - 1]

    Path('../../../../storage/result/').mkdir(parents=True, exist_ok=True)
    if not path.exists('../../../../storage/result/' + project + '.' + str(clientId) + '.csv'):
        with open('../../../../storage/result/' + project + '.' + str(clientId) + '.csv', mode='w',
                  newline='') as project_result:
            writer = csv.writer(project_result)
            index = ['project']
            for i in range(top_k):
                index.append("ngram excode top{}".format(i + 1))
            for i in range(top_k):
                index.append("ngram lex top{}".format(i + 1))
            for i in range(top_k):
                index.append("RNN excode top{}".format(i + 1))
            for i in range(top_k):
                index.append("RNN lex top{}".format(i + 1))
            writer.writerow(index)
    with open('../../../../storage/result/' + project + '.' + str(clientId) + '.csv', mode='a',
              newline='') as project_result:
        writer = csv.writer(project_result)
        proj_result = [project]
        for i in range(top_k):
            proj_result.append(round(ngram_excode_correct[i] / total_guesses, 2))
        for i in range(top_k):
            proj_result.append(round(ngram_lex_correct[i] / total_guesses, 2))
        for i in range(top_k):
            proj_result.append(round(rnn_excode_correct[i] / total_guesses, 2))
        for i in range(top_k):
            proj_result.append(round(rnn_lex_correct[i] / total_guesses, 2))
        writer.writerow(proj_result)


def recreate(result, data):
    origin = []
    for candidate_ids in result:
        candidate_text = ""
        for i in range(len(candidate_ids)):
            # print(data['next_lex'][i][candidate_ids[i][0]][candidate_ids[i][1]])
            candidate_text += data['next_lex'][i][candidate_ids[i][0]][candidate_ids[i][1]]
            if i < len(candidate_ids) - 1:
                candidate_text += ", "
        origin.append(candidate_text)
    return origin


def flute(conn, clientId):
    total_guesses = 0
    while True:
        data = recvall(conn)
        if not data:
            break
        total_guesses += 1
        data = data.decode("utf-8")
        data = json.loads(data)
        startTime = perf_counter()
        origin_data = copy.deepcopy(data)
        # excode_context = excode_tokenize(data['excode_context'],
        #                                  tokenizer=excode_tokenizer,
        #                                  train_len=train_len,
        #                                  tokens=excode_tokens,
        #                                  method_content_only=False)[0]
        # java_context = java_tokenize_take_last(data['lex_context'],
        #                                       tokenizer=java_tokenizer,
        #                                       train_len=train_len)[0]
        if USE_RNN:
            if data['method_name'] != "":
                method_name = tokenize(data['method_name'])
            else:
                method_name = "<UNK>"
            class_name = tokenize(data['class_name'])
            method_name_tokens_excode = excode_tokenizer.texts_to_sequences([method_name])[0]
            class_name_tokens_excode = excode_tokenizer.texts_to_sequences([class_name])[0]
            method_name_tokens_java = java_tokenizer.texts_to_sequences([method_name])[0]
            class_name_tokens_java = java_tokenizer.texts_to_sequences([class_name])[0]
            excode_origin_context = excode_tokenize(data['excode_context'],
                                                    tokenizer=excode_tokenizer,
                                                    train_len=train_len,
                                                    tokens=excode_tokens,
                                                    method_content_only=False)[0]
            excode_context = [([[]], [])]
            expected_excode = excode_tokenize(data['expected_excode'],
                                              tokenizer=excode_tokenizer,
                                              train_len=train_len,
                                              tokens=excode_tokens,
                                              method_content_only=False)[0]
            excode_comma_id = excode_tokenizer.texts_to_sequences([["SEPA(,)"]])[0]
            # print(excode_comma_id)
            n_param = len(data['next_excode'])
            sorted_scores = []
            for p_id in range(n_param):
                excode_suggestions = excode_tokenize_candidates(data['next_excode'][p_id],
                                                                tokenizer=excode_tokenizer,
                                                                tokens=excode_tokens)
                scores = []
                excode_suggestion_scores = []
                # start_time = time.time()
                x_test_all = []
                y_test_all = []
                sentence_len_all = []
                for ex_suggest_id in range(len(excode_context)):
                    curr_context = excode_origin_context + excode_context[ex_suggest_id][0][0]
                    x_test, y_test, sentence_len = prepare(curr_context,
                                                           excode_suggestions,
                                                           train_len,
                                                           len(curr_context))
                    # print(x_test.shape)
                    x_test_all += x_test.tolist()
                    y_test_all += y_test.tolist()
                    sentence_len_all += sentence_len
                x_test_all = np.array(x_test_all)
                y_test_all = np.array(y_test_all)
                p_pred = predict(excode_model_rnn, x_test_all,
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
                # logger.debug("--- %s seconds ---" % (time.time() - start_time))
                sorted_scores = sorted(excode_suggestion_scores, key=lambda x: -x[2])[:max_keep_step[p_id]]
                excode_context = [(x[0], x[1]) for x in sorted_scores]
                # logger.debug(sorted_scores)
                # logger.debug('-----------------------------\n-----------------------------\n-----------------------------')
                # logger.debug("Best excode suggestion(s):")

            # print(expected_excode)
            # print(excode_context)
            for i in range(min(top_k, len(excode_context))):
                # logger.debug(data['next_lex'][sorted_scores[i][2]])
                if expected_excode == excode_context[i][0][0]:
                    rnn_excode_correct[i] += 1
            java_origin_context = java_tokenize_take_last(data['lex_context'],
                                                          tokenizer=java_tokenizer,
                                                          train_len=train_len)
            expected_lex = java_tokenize_take_last([data['expected_lex']],
                                                   tokenizer=java_tokenizer,
                                                   train_len=train_len)
            java_comma_id = java_tokenize_take_last([","],
                                                    tokenizer=java_tokenizer,
                                                    train_len=train_len)
            # print(java_comma_id)
            # print("Elex", expected_lex)
            java_suggestions_all = []
            for i in range(n_param):
                java_suggestions_all.append([])
                for j in range(len(data['next_lex'][i])):
                    java_suggestions_all[i].append(java_tokenize_sentences(data['next_lex'][i][j],
                                                                           tokenizer=java_tokenizer))
            # print("Javasug", java_suggestions_all)
            # print("EXX", excode_context)
            java_context = [([], 0, x, []) for x in range(0, len(excode_context))]
            # print(java_context)
            for j in range(n_param):
                # print(excode_context[i][1])
                x_test_all = []
                y_test_all = []
                sentence_len_all = []
                # print(excode_context)
                for k in range(len(java_context)):
                    i = java_context[k][2]
                    # start_time = time.time()
                    java_suggestions = java_suggestions_all[j][excode_context[i][1][j]]
                    # print("Sug", java_suggestions)
                    curr_context = java_origin_context + java_context[k][0]
                    x_test, y_test, sentence_len = prepare(curr_context,
                                                           java_suggestions, train_len,
                                                           len(curr_context))
                    x_test_all += x_test.tolist()
                    y_test_all += y_test.tolist()
                    sentence_len_all += sentence_len
                x_test_all = np.array(x_test_all)
                y_test_all = np.array(y_test_all)
                # print(x_test_all.shape)
                p_pred = predict(java_model_rnn, x_test_all,
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
                        # print(i, j, k, "NEWCT", new_context)
                        # print(java_context[k][0])
                        # print(java_suggestion)
                        java_suggestion_scores.append((new_context,
                                                       log_p_sentence[counter],
                                                       i,
                                                       java_context[k][3] + [(excode_context[i][1][j], ii)]))
                        counter += 1
                # logger.debug("--- %s seconds ---" % (time.time() - start_time))
                sorted_scores = sorted(java_suggestion_scores, key=lambda x: -x[1])[:max_keep_step[j]]
                # print(max_keep_step[j])
                # print("SORTED_SCC", sorted_scores)
                java_context = sorted_scores
            # print(sorted_scores)
            sorted_scores = sorted(java_context, key=lambda x: -x[1])[:top_k]
            result_rnn = []
            for i in range(min(top_k, len(sorted_scores))):
                candidate = sorted_scores[i][0]
                # logger.debug(candidate)
                if candidate == data['expected_lex']:
                    rnn_lex_correct[i] += 1
                result_rnn.append(sorted_scores[i][3])
            runtime_rnn = perf_counter() - startTime
            logger.debug("Total rnn runtime: " + str(runtime_rnn))
            result_rnn = recreate(result_rnn, origin_data)
            print("Result rnn:\n", result_rnn)

        # ----------------------------------------------------------------------------------------------------------
        # n-gram

        startTime = perf_counter()
        data = copy.deepcopy(origin_data)
        excode_context = excode_tokenize(data['excode_context'],
                                         tokenizer=excode_tokenizer,
                                         train_len=train_len,
                                         tokens=excode_tokens,
                                         method_content_only=False)[0]
        expected_excode = excode_tokenize(data['expected_excode'],
                                          tokenizer=excode_tokenizer,
                                          train_len=train_len,
                                          tokens=excode_tokens,
                                          method_content_only=False)[0]
        expected_excode_text = excode_tokenizer.sequences_to_texts([expected_excode])[0].split()
        java_context = java_tokenize_take_last(data['lex_context'],
                                               tokenizer=java_tokenizer,
                                               train_len=train_len)
        expected_lex = java_tokenize_take_last([data['expected_lex']],
                                               tokenizer=java_tokenizer,
                                               train_len=train_len)
        expected_lex_text = java_tokenizer.sequences_to_texts([expected_lex])[0].split()
        n_param = len(data['next_excode'])

        excode_context_textform = excode_tokenizer.sequences_to_texts([excode_context])[0].split()[-ngram:]
        # print(excode_context_textform)
        excode_context_textform = [(excode_context_textform, [])]
        for p_id in range(n_param):
            excode_suggestions = excode_tokenize_candidates(data['next_excode'][p_id],
                                                            tokenizer=excode_tokenizer,
                                                            tokens=excode_tokens)
            excode_suggestions_textforms = excode_tokenizer.sequences_to_texts(excode_suggestions)
            excode_suggestion_scores = []
            for ex_suggest_id in range(len(excode_context_textform)):
                for i, excode_suggestions_textform in enumerate(excode_suggestions_textforms):
                    # start_time = time.time()
                    sentence = excode_context_textform[ex_suggest_id][0] + excode_suggestions_textform.split()
                    if p_id < n_param - 1:
                        sentence += ['sepa(,)']
                    # print(sentence, "1")
                    score = score_ngram(model=excode_model_ngram,
                                        sentence=sentence,
                                        n=ngram,
                                        start_pos=len(excode_context_textform[ex_suggest_id][0]))
                    excode_suggestion_scores.append((sentence,
                                                     excode_context_textform[ex_suggest_id][1] + [i],
                                                     score))
            sorted_scores = sorted(excode_suggestion_scores, key=lambda x: -x[2])[:max_keep_step[p_id]]
            excode_context_textform = [(x[0], x[1]) for x in sorted_scores]

        # print("MI", excode_context_textform)
        # print("SUG", excode_context_textform)
        # logger.debug(sorted_scores)
        # logger.debug('-----------------------------\n-----------------------------\n-----------------------------')
        # logger.debug("Best excode suggestion(s):")
        # print(1, expected_excode_text)
        for i in range(min(top_k, len(excode_context_textform))):
            # print(excode_context_textform[i][0][ngram:])
            if expected_excode_text == excode_context_textform[i][0][ngram:]:
                ngram_excode_correct[i] += 1
        java_suggestions_all = np.array(data['next_lex'], dtype=object)
        for i in range(n_param):
            for j in range(len(java_suggestions_all[i])):
                java_suggestions_all[i][j] = java_tokenize_sentences(data['next_lex'][i][j],
                                                                     tokenizer=java_tokenizer,
                                                                     to_sequence=False)
        # print(java_suggestions_all[0])
        all_candidate_lex = []
        for i in range(len(excode_context_textform)):
            java_context_list = java_tokenizer.sequences_to_texts([java_context])[0].split()[-ngram:]
            java_context_list = [(java_context_list, [])]
            for j in range(n_param):
                java_suggestion_scores = []
                for k in range(len(java_context_list)):
                    java_suggestions = java_suggestions_all[j][excode_context_textform[i][1][j]]
                    for ii, java_suggestion in enumerate(java_suggestions):
                        # print(i, j, k, java_context_list[k][0])
                        # print(java_suggestion)
                        new_context = java_context_list[k][0] + java_suggestion
                        if j < n_param - 1:
                            new_context += [',']
                        score = score_ngram(model=java_model_ngram,
                                            sentence=new_context,
                                            n=ngram,
                                            start_pos=len(java_context_list[k][0]))
                        java_suggestion_scores.append((new_context, java_context_list[k][1]
                                                       + [(excode_context_textform[i][1][j], ii)], score))
                sorted_scores = sorted(java_suggestion_scores, key=lambda x: -x[2])
                if j < n_param - 1:
                    java_context_list = [(x[0], x[1]) for x in sorted_scores]
                else:
                    java_context_list = sorted_scores
            all_candidate_lex += java_context_list
        sorted_scores = sorted(all_candidate_lex, key=lambda x: -x[2])
        # logger.debug(sorted_scores)
        # logger.debug('-----------------------------\n-----------------------------\n-----------------------------')
        # logger.debug("Best java suggestion(s):")
        result_ngram = []
        for i in range(min(top_k, len(sorted_scores))):
            candidate = sorted_scores[i][0][ngram:]
            # logger.debug(candidate)
            if candidate == data['expected_lex']:
                ngram_lex_correct[i] += 1
            result_ngram.append(sorted_scores[i][1])
        runtime_ngram = perf_counter() - startTime
        logger.debug("Total n-gram runtime: " + str(runtime_ngram))
        result_ngram = recreate(result_ngram, origin_data)
        print("Result ngram:\n", result_ngram)

        response = '{type:"predict", data:{' \
                   + 'ngram:{' \
                   + 'result:' + json.dumps(result_ngram) \
                   + ',runtime:' + str(runtime_ngram) \
                   + '}'
        if USE_RNN:
            # print("RN", result_rnn)
            response += ',' \
                        + 'rnn:{' \
                        + 'result:' + json.dumps(result_rnn) \
                        + ',runtime:' + str(runtime_rnn) \
                        + '}'
        conn.send((response + '}}\n').encode())
    conn.close()
    print('Client disconnected')

    write_result(project, clientId, ngram_excode_correct, ngram_lex_correct, rnn_excode_correct, rnn_lex_correct,
                 total_guesses)


def main():
    clientId = 0
    for filename in glob.glob('../../../../storage/result/' + project + '.' + "*"):
        os.remove(filename)
    print('[SERVER IS STARTED]')
    while True:
        conn, addr = serv.accept()
        print("ID[" + str(clientId) + "] New client is connected!")
        print_lock = threading.Lock()
        start_new_thread(flute, (conn, clientId))
        clientId += 1


if __name__ == '__main__':
    main()
