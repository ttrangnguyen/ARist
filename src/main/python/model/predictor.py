import csv
import os
import numpy as np
import pandas as pd
from model.tokenizer import Tokenizer
from keras.utils import to_categorical
from keras.models import Sequential
from keras.layers import LSTM, Dense, Dropout, Embedding
from keras import optimizers
from keras.preprocessing.sequence import pad_sequences
import sys
from keras.callbacks import ModelCheckpoint
from model.data_generator import DataGenerator
import sklearn


def prepare_sentence(seq, train_len, start_pos):
    # Pads seq and slides windows
    x = []
    y = []
    for i in range(start_pos, len(seq)):
        x_padded = pad_sequences([seq[:i]],
                                 maxlen=train_len - 1,
                                 padding='pre')[0]  # Pads before each sequence
        x.append(x_padded)
        y.append(seq[i])
    return x, y


def predict(model, sentence, tokenizer, train_len, start_pos):
    vocab = dict(tokenizer.word_index)
    x_test, y_test = prepare_sentence(sentence, train_len, start_pos)
    x_test = np.array(x_test)
    y_test = np.array(y_test) - 1  # The word <PAD> does not have a class
    p_pred = model.predict(x_test)
    vocab_inv = {v: k for k, v in vocab.items()}
    log_p_sentence = 0

    for i, prob in enumerate(p_pred):
        word = vocab_inv[y_test[i] + 1]  # Index 0 from vocab is reserved to <PAD>
        history = ''.join([vocab_inv[w] for w in x_test[i, :] if w != 0])
        prob_word = prob[y_test[i]]
        log_p_sentence += np.log(prob_word)
        print('P(w={}|h={})={}'.format(word, history, prob_word))

    print('Prob. sentence: {}'.format(log_p_sentence))
    return log_p_sentence