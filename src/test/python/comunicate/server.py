import socket
import json
from name_stat.name_tokenizer import tokenize
from model.java.java_preprocess import java_tokenize, create_java_tokenizer, prepare_sequence, \
    java_tokenize_one_sentence
from model.tokenizer import Tokenizer
from model.excode.excode_preprocess import excode_tokenize, create_excode_tokenizer, excode_tokenize_candidates
from keras.models import load_model
from model.predictor import predict


def read_file(filepath):
    with open(filepath) as f:
        str_text = f.read()
    return str_text


serv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

serv.bind(('0.0.0.0', 18007))
serv.listen(1)

dataJson = [
    "hello",
    "how are you"
]

jsonData = json.dumps(dataJson)
excode_tokenizer = create_excode_tokenizer(token_path='../../../../data_dict/excode/excode_tokens_n_symbols.txt',
                                           dict_path='../../../../data_dict/excode/names.txt', )
java_tokenizer = create_java_tokenizer(token_path='../../../../data_dict/java/java_words_n_symbols.txt',
                                       dict_path='../../../../data_dict/java/names.txt', )
excode_tokens = read_file('../../../../data_dict/excode/excode_tokens_n_symbols.txt').lower().split("\n")
train_len = 20 + 1

while True:
    conn, addr = serv.accept()
    while True:
        data = conn.recv(4096)
        if not data:
            break
        data = data.decode("utf-8")
        data = json.loads(data)
        java_context = java_tokenize(data['lex_context'],
                                     tokenizer=java_tokenizer,
                                     train_len=train_len,
                                     last_only=True)[0]
        excode_context = excode_tokenize(data['excode_context'],
                                         tokenizer=excode_tokenizer,
                                         train_len=train_len,
                                         tokens=excode_tokens,
                                         last_only=True)[0]
        java_suggestions = java_tokenize_one_sentence(data['next_lex'],
                                                      tokenizer=java_tokenizer
                                                      )
        excode_suggestions = excode_tokenize_candidates(data['next_excode'],
                                                        tokenizer=excode_tokenizer,
                                                        tokens=excode_tokens)

        print(java_context)
        print(java_tokenizer.sequences_to_texts([java_context]))
        print(java_suggestions)
        print(excode_context)
        print(excode_tokenizer.sequences_to_texts([excode_context]))
        print(excode_suggestions)
        for excode_suggestion in excode_suggestions:
            print(excode_tokenizer.sequences_to_texts([excode_suggestion]))
        excode_model = load_model("../model/excode_model.h5")
        java_model = load_model("../model/java_model.h5")
        scores = []
        java_suggestion_scores = []
        best_java_score = -1e9
        top_k = 5
        i = 0
        for java_suggestion in java_suggestions:
            score = predict(java_model, java_context + java_suggestion,
                            tokenizer=java_tokenizer, train_len=train_len, start_pos=train_len - 1)
            java_suggestion_scores.append((java_suggestion, score, i))
            i += 1

        sorted_scores = sorted(java_suggestion_scores, key=lambda x: -x[1])
        print(sorted_scores)
        print('-----------------------------\n-----------------------------\n-----------------------------')
        print("Best java suggestion(s):")

        result = []

        for i in range(min(top_k, len(sorted_scores))):
            print(data['next_lex'][sorted_scores[i][2]])
            result.append(data['next_lex'][sorted_scores[i][2]]);

        print(result)
        conn.send(('{type:"predict",data:' + json.dumps(result) + '}\n').encode())
        # best_excode_suggestion = ""
        # best_excode_score = -1e9
        #
        # for excode_suggestion in excode_suggestions:
        #     score = predict(excode_model, excode_context + excode_suggestion,
        #                     tokenizer=excode_tokenizer, train_len=train_len, start_pos=train_len-1)
        #     if score > best_excode_score:
        #         best_excode_score = score
        #         best_excode_suggestion = excode_suggestion
        #
        # print("Best excode suggestion:", ''.join(excode_tokenizer.sequences_to_texts([best_excode_suggestion])[0]))
    conn.send(('{type:"predict",data:' + jsonData + '}\n').encode())
    conn.close()
    print('client disconnected')
