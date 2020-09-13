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
from nltk.lm import MLE


def read_file(filepath):
    with open(filepath) as f:
        str_text = f.read()
    return str_text


serv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
serv.bind(('0.0.0.0', 18007))
serv.listen(1)

project = 'ant'
excode_model = load_model("../model/excode_model_" + project + ".h5")
java_model = load_model("../model/java_model_" + project + ".h5")
with open("../model/excode_model_" + project + "ngram.pkl", 'rb') as fin:
    excode_model_ngram = dill.load(fin)
with open("../model/java_model_" + project + "ngram.pkl", 'rb') as fin:
    java_model_ngram = dill.load(fin)
excode_tokenizer = load(open('../../../../src/main/python/model/excode/excode_tokenizer', 'rb'))
java_tokenizer = load(open('../../../../src/main/python/model/java/java_tokenizer', 'rb'))
excode_tokens = read_file('../../../../data_dict/excode/excode_tokens_n_symbols.txt').lower().split("\n")
train_len = 20 + 1
n_gram = 2 + 1
top_k = 5

while True:
    conn, addr = serv.accept()
    while True:
        data = conn.recv(10240)
        if not data:
            break

        startTime = perf_counter()

        data = data.decode("utf-8")
        data = json.loads(data)
        excode_context = excode_tokenize(data['excode_context'],
                                         tokenizer=excode_tokenizer,
                                         train_len=train_len,
                                         tokens=excode_tokens,
                                         last_only=True)[0]
        excode_suggestions = excode_tokenize_candidates(data['next_excode'],
                                                        tokenizer=excode_tokenizer,
                                                        tokens=excode_tokens)
        scores = []
        excode_suggestion_scores = []
        best_excode_score = -1e9
        i = 0

        for excode_suggestion in excode_suggestions:
            start_time = time.time()
            score = predict(excode_model, excode_context + excode_suggestion,
                            tokenizer=excode_tokenizer, train_len=train_len, start_pos=len(excode_context))
            excode_suggestion_scores.append((excode_suggestion, score, i))
            i += 1
            print("--- %s seconds ---" % (time.time() - start_time))

        sorted_scores = sorted(excode_suggestion_scores, key=lambda x: -x[1])
        print(sorted_scores)
        print('-----------------------------\n-----------------------------\n-----------------------------')
        print("Best excode suggestion(s):")

        lexemes = []
        for i in range(min(top_k, len(sorted_scores))):
            print(data['next_lex'][sorted_scores[i][2]])
            lexemes = lexemes + data['next_lex'][sorted_scores[i][2]]
        java_suggestions = java_tokenize_one_sentence(lexemes,
                                                      tokenizer=java_tokenizer)
        java_context = java_tokenize(data['lex_context'],
                                     tokenizer=java_tokenizer,
                                     train_len=train_len,
                                     last_only=True)[0]

        scores = []
        java_suggestion_scores = []
        best_java_score = -1e9
        i = 0

        for java_suggestion in java_suggestions:
            start_time = time.time()
            score = predict(java_model, java_context + java_suggestion,
                            tokenizer=java_tokenizer, train_len=train_len, start_pos=len(java_context))
            java_suggestion_scores.append((java_suggestion, score, i))
            i += 1
            print("--- %s seconds ---" % (time.time() - start_time))
        sorted_scores = sorted(java_suggestion_scores, key=lambda x: -x[1])
        print(sorted_scores)
        print('-----------------------------\n-----------------------------\n-----------------------------')
        print("Best java suggestion(s):")

        result = []
        for i in range(min(top_k, len(sorted_scores))):
            print(lexemes[sorted_scores[i][2]])
            result.append(lexemes[sorted_scores[i][2]])
        runtime = perf_counter() - startTime
        print("Total rnn runtime: ", runtime)

        # n-gram
        startTime = perf_counter()

        scores = []
        excode_suggestion_scores = []
        best_excode_score = -1e9
        i = 0

        excode_context_textform = excode_tokenizer.sequences_to_texts([excode_context])[0].split()
        length = len(excode_context_textform)
        if length > n_gram:
            excode_context_textform = excode_context_textform[length-n_gram:length]

        excode_suggestions_textforms = excode_tokenizer.sequences_to_texts(excode_suggestions)
        for excode_suggestions_textform in excode_suggestions_textforms:
            start_time = time.time()
            score = score_ngram(model=excode_model_ngram,
                                sentence=excode_context_textform + excode_suggestions_textform.split(),
                                n=n_gram,
                                start_pos=len(excode_context_textform))
            excode_suggestion_scores.append((excode_suggestions_textform, score, i))
            i += 1
            print("--- %s seconds ---" % (time.time() - start_time))

        sorted_scores = sorted(excode_suggestion_scores, key=lambda x: -x[1])
        print(sorted_scores)
        print('-----------------------------\n-----------------------------\n-----------------------------')
        print("Best excode suggestion(s):")
        lexemes = []
        for i in range(min(top_k, len(sorted_scores))):
            print(data['next_lex'][sorted_scores[i][2]])
            lexemes = lexemes + data['next_lex'][sorted_scores[i][2]]
        java_suggestions_textforms = java_tokenize_one_sentence(lexemes,
                                                                tokenizer=java_tokenizer,
                                                                to_sequence=False)
        java_context_textform = java_tokenizer.sequences_to_texts([java_context])[0].split()
        length = len(java_context_textform)
        if length > n_gram:
            java_context_textform = java_context_textform[length-n_gram:length]
        scores = []
        java_suggestion_scores = []
        best_java_score = -1e9
        i = 0
        for java_suggestions_textform in java_suggestions_textforms:
            start_time = time.time()
            score = score_ngram(model=java_model_ngram,
                                sentence=java_context_textform + java_suggestions_textform,
                                n=n_gram,
                                start_pos=len(java_context_textform))
            java_suggestion_scores.append((java_suggestions_textform, score, i))
            i += 1
            print("--- %s seconds ---" % (time.time() - start_time))
        sorted_scores = sorted(java_suggestion_scores, key=lambda x: -x[1])
        print(sorted_scores)
        print('-----------------------------\n-----------------------------\n-----------------------------')
        print("Best java suggestion(s):")

        result = []
        for i in range(min(top_k, len(sorted_scores))):
            print(lexemes[sorted_scores[i][2]])
            result.append(lexemes[sorted_scores[i][2]])
        runtime = perf_counter() - startTime
        print("Total n-gram runtime: ", runtime)

    conn.send(('{type:"predict",data:' + json.dumps(result) + ',runtime:' + str(runtime) + '}\n').encode())
    conn.close()
    print('Client disconnected')