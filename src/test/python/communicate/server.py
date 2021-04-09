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
from model.config import *
from config import *


class Server:
    def __init__(self, service, port):
        self.serv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.serv.bind(('0.0.0.0', port))
        self.serv.listen(10)

        self.rnn_manager = None
        if USE_RNN:
            self.rnn_manager = RNNManager(TOP_K, PROJECT, TRAIN_LEN_RNN,
                                          EXCODE_MODEL_RNN_PATH,
                                          JAVA_MODEL_RNN_PATH,
                                          METHODCALL_MODEL_RNN_PATH)
        self.ngram_manager = None
        if USE_NGRAM:
            self.ngram_manager = NgramManager(TOP_K, PROJECT, TRAIN_LEN_RNN,
                                              EXCODE_MODEL_NGRAM_PATH,
                                              JAVA_MODEL_NGRAM_PATH,
                                              METHODCALL_MODEL_NGRAM_PATH)
        self.service = service

    def recvall(self, sock):
        BUFF_SIZE = 1024
        fragments = []
        while True:
            chunk = sock.recv(BUFF_SIZE)

            fragments.append(chunk)

            if (not chunk) or chunk[-1] == 255:
                break
        arr = b''.join(fragments)
        return arr[:-1]

    def flute(self, conn, clientId):
        while True:
            data = self.recvall(conn)
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
                response += self.rnn_manager.process(data, self.service)
            if USE_NGRAM:
                if USE_RNN:
                    response += ','
                response += self.ngram_manager.process(data, self.service)
            conn.send((response + '}}\n').encode())
        conn.close()
        print('Client disconnected')

        # write_result(project, clientId, ngram_excode_correct, ngram_lex_correct, rnn_excode_correct, rnn_lex_correct,
        #              total_guesses)

    def run(self):
        clientId = 0
        for filename in glob.glob('../../../../storage/result/' + PROJECT + '.' + "*"):
            os.remove(filename)
        print('[SERVER IS STARTED]')
        while True:
            conn, addr = self.serv.accept()
            print("ID[" + str(clientId) + "] New client is connected!")
            print_lock = threading.Lock()
            start_new_thread(self.flute, (conn, clientId))
            clientId += 1


if __name__ == "__main__":
    if TEST_SERVICE == "param":
        param_server = Server(service="param", port=PORT_PARAM)
        param_server.run()
    elif TEST_SERVICE == "method_name":
        method_name_server = Server(service="method_name", port=PORT_METHOD_NAME)
        method_name_server.run()
