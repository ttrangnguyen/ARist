import glob
import json
import os
import socket
import threading

from _thread import *

from model.manager.ngram_manager import NgramManager
from model.manager.rnn_manager import RNNManager
from name_stat.name_tokenizer import tokenize
from name_stat.similarly import lexSim


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
top_k = 10
train_len = 6 + 1
ngram = 4 + 1
USE_RNN = False
USE_NGRAM = True

excode_model_rnn_path = "../../../../../model/excode_model_" + project + ".h5"
java_model_rnn_path = "../../../../../model/java_model_" + project + ".h5"
excode_model_ngram_path = "../../../../../model/excode_model_" + project + "ngram.pkl", 'rb'
java_model_ngram_path = "../../../../../model/java_model_" + project + "ngram.pkl", 'rb'
excode_tokenizer_path = '../excode/excode_tokenizer'
java_tokenizer_path = '../java/java_tokenizer'
excode_tokens_path = '../../../../data_dict/excode/excode_tokens_n_symbols.txt'


def flute(conn, clientId):
    rnn_manager = None
    if USE_RNN:
        rnn_manager = RNNManager(top_k, project, train_len,
                                 excode_model_rnn_path, java_model_rnn_path,
                                 excode_tokenizer_path, java_tokenizer_path,
                                 excode_tokens_path)
    ngram_manager = None
    if USE_NGRAM:
        ngram_manager = NgramManager(top_k, project, train_len, ngram,
                                     excode_model_ngram_path, java_model_ngram_path,
                                     excode_tokenizer_path, java_tokenizer_path,
                                     excode_tokens_path)
    while True:
        data = recvall(conn)
        if not data:
            break
        data = data.decode("utf-8")
        print(data)
        data = json.loads(data)
        if ('type' in data) and (data['type'] == "tokenize"):
            conn.send(('{type:"tokenize", data:' + json.dumps(tokenize(data['data'])) + '}\n').encode())
            continue
        else:
            if ('type' in data) and (data['type'] == "lexSim"):
                conn.send(('{type:"lexSim", data:' + json.dumps(lexSim(data['s1'], data['s2'])) + '}\n').encode())
                continue
        response = '{type:"predict", data:{'
        if USE_RNN:
            response += rnn_manager.work(data)
        if USE_NGRAM:
            if USE_RNN:
                response += ','
            response += ngram_manager.work(data)
        conn.send((response + '}}\n').encode())
    conn.close()
    print('Client disconnected')

    # write_result(project, clientId, ngram_excode_correct, ngram_lex_correct, rnn_excode_correct, rnn_lex_correct,
    #              total_guesses)


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
