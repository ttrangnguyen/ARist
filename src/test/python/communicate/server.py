import socket
import json
from model.java.java_preprocess import java_tokenize, java_tokenize_one_sentence
from model.excode.excode_preprocess import excode_tokenize, excode_tokenize_candidates
from keras.models import load_model
from model.predictor import predict
from time import perf_counter
import time
from pickle import load
import dill
from model.ngram_predictor import score_ngram
from os import path
import csv
from nltk.lm import MLE
import logging
import sys


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
serv.listen(1)

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
top_k = 5

total_guesses = 0
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


def write_result(project, ngram_excode_correct, ngram_lex_correct, rnn_excode_correct, rnn_lex_correct):
    for i in range(1, top_k):
        ngram_excode_correct[i] += ngram_excode_correct[i - 1]
        ngram_lex_correct[i] += ngram_lex_correct[i - 1]
        rnn_excode_correct[i] += rnn_excode_correct[i - 1]
        rnn_lex_correct[i] += rnn_lex_correct[i - 1]

    if not path.exists('../../../../storage/result/' + project + '.csv'):
        with open('../../../../storage/result/' + project + '.csv', mode='w', newline='') as project_result:
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
    with open('../../../../storage/result/' + project + '.csv', mode='a', newline='') as project_result:
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


while True:
    conn, addr = serv.accept()
    while True:
        data = recvall(conn)
        if not data:
            break
        startTime = perf_counter()
        total_guesses += 1
        data = data.decode("utf-8")
        data = json.loads(data)
        excode_context = excode_tokenize(data['excode_context'],
                                         tokenizer=excode_tokenizer,
                                         train_len=train_len,
                                         tokens=excode_tokens,
                                         method_only=False)[0]
        excode_suggestions = excode_tokenize_candidates(data['next_excode'],
                                                        tokenizer=excode_tokenizer,
                                                        tokens=excode_tokens)

        java_context = java_tokenize(data['lex_context'],
                                     tokenizer=java_tokenizer,
                                     train_len=train_len,
                                     last_only=True)[0]
        if USE_RNN:
            scores = []
            excode_suggestion_scores = []
            best_excode_score = -1e9
            i = 0

            for excode_suggestion in excode_suggestions:
                start_time = time.time()
                score = predict(excode_model_rnn, excode_context + excode_suggestion,
                                tokenizer=excode_tokenizer, train_len=train_len, start_pos=len(excode_context))
                excode_suggestion_scores.append((excode_suggestion, score, i))
                i += 1
                logger.debug("--- %s seconds ---" % (time.time() - start_time))

            sorted_scores = sorted(excode_suggestion_scores, key=lambda x: -x[1])
            logger.debug(sorted_scores)
            logger.debug('-----------------------------\n-----------------------------\n-----------------------------')
            logger.debug("Best excode suggestion(s):")

            lexemes = []
            for i in range(min(top_k, len(sorted_scores))):
                logger.debug(data['next_lex'][sorted_scores[i][2]])
                if data['expected_excode'] == data['next_excode'][sorted_scores[i][2]]:
                    rnn_excode_correct[i] += 1
                lexemes = lexemes + data['next_lex'][sorted_scores[i][2]]
            java_suggestions = java_tokenize_one_sentence(lexemes,
                                                          tokenizer=java_tokenizer)

            scores = []
            java_suggestion_scores = []
            best_java_score = -1e9
            i = 0

            for java_suggestion in java_suggestions:
                start_time = time.time()
                score = predict(java_model_rnn, java_context + java_suggestion,
                                tokenizer=java_tokenizer, train_len=train_len, start_pos=len(java_context))
                java_suggestion_scores.append((java_suggestion, score, i))
                i += 1
                logger.debug("--- %s seconds ---" % (time.time() - start_time))
            sorted_scores = sorted(java_suggestion_scores, key=lambda x: -x[1])
            logger.debug(sorted_scores)
            logger.debug('-----------------------------\n-----------------------------\n-----------------------------')
            logger.debug("Best java suggestion(s):")

            result_rnn = []
            for i in range(min(top_k, len(sorted_scores))):
                candidate = lexemes[sorted_scores[i][2]]
                logger.debug(candidate)
                if candidate == data['expected_lex']:
                    rnn_lex_correct[i] += 1
                result_rnn.append(candidate)
            runtime_rnn = perf_counter() - startTime
            logger.debug("Total rnn runtime: " + str(runtime_rnn))

        # n-gram
        startTime = perf_counter()

        scores = []
        excode_suggestion_scores = []
        best_excode_score = -1e9
        i = 0

        excode_context_textform = excode_tokenizer.sequences_to_texts([excode_context])[0].split()
        length = len(excode_context_textform)
        if length > ngram:
            excode_context_textform = excode_context_textform[length - ngram:length]

        excode_suggestions_textforms = excode_tokenizer.sequences_to_texts(excode_suggestions)
        for excode_suggestions_textform in excode_suggestions_textforms:
            start_time = time.time()
            score = score_ngram(model=excode_model_ngram,
                                sentence=excode_context_textform + excode_suggestions_textform.split(),
                                n=ngram,
                                start_pos=len(excode_context_textform))
            excode_suggestion_scores.append((excode_suggestions_textform, score, i))
            i += 1
            logger.debug("--- %s seconds ---" % (time.time() - start_time))

        sorted_scores = sorted(excode_suggestion_scores, key=lambda x: -x[1])
        logger.debug(sorted_scores)
        logger.debug('-----------------------------\n-----------------------------\n-----------------------------')
        logger.debug("Best excode suggestion(s):")
        lexemes = []
        for i in range(min(top_k, len(sorted_scores))):
            logger.debug(data['next_lex'][sorted_scores[i][2]])
            if data['expected_excode'] == data['next_excode'][sorted_scores[i][2]]:
                ngram_excode_correct[i] += 1
            lexemes = lexemes + data['next_lex'][sorted_scores[i][2]]
        java_suggestions_textforms = java_tokenize_one_sentence(lexemes,
                                                                tokenizer=java_tokenizer,
                                                                to_sequence=False)
        java_context_textform = java_tokenizer.sequences_to_texts([java_context])[0].split()
        length = len(java_context_textform)
        if length > ngram:
            java_context_textform = java_context_textform[length - ngram:length]
        scores = []
        java_suggestion_scores = []
        best_java_score = -1e9
        i = 0
        for java_suggestions_textform in java_suggestions_textforms:
            start_time = time.time()
            score = score_ngram(model=java_model_ngram,
                                sentence=java_context_textform + java_suggestions_textform,
                                n=ngram,
                                start_pos=len(java_context_textform))
            java_suggestion_scores.append((java_suggestions_textform, score, i))
            i += 1
            logger.debug("--- %s seconds ---" % (time.time() - start_time))
        sorted_scores = sorted(java_suggestion_scores, key=lambda x: -x[1])
        logger.debug(sorted_scores)
        logger.debug('-----------------------------\n-----------------------------\n-----------------------------')
        logger.debug("Best java suggestion(s):")
        result_ngram = []
        for i in range(min(top_k, len(sorted_scores))):
            candidate = lexemes[sorted_scores[i][2]]
            logger.debug(candidate)
            if candidate == data['expected_lex']:
                ngram_lex_correct[i] += 1
            result_ngram.append(candidate)
        runtime_ngram = perf_counter() - startTime
        logger.debug("Total n-gram runtime: " + str(runtime_ngram))
        response = '{type:"predict", data:{' \
                   + 'ngram:{' \
                   + 'result:' + json.dumps(result_ngram) \
                   + ',runtime:' + str(runtime_ngram) \
                   + '}'
        if USE_RNN:
            response += ',' \
                        + 'rnn:{' \
                        + 'result:' + json.dumps(result_rnn) \
                        + ',runtime:' + str(runtime_rnn) \
                        + '}'
        conn.send((response + '}}\n').encode())
    conn.close()
    logger.debug('Client disconnected')

    write_result(project, ngram_excode_correct, ngram_lex_correct, rnn_excode_correct, rnn_lex_correct)
