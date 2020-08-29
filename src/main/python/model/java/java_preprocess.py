import os
from model.tokenizer import Tokenizer
from name_stat.name_tokenizer import tokenize
from keras.preprocessing.sequence import pad_sequences
import csv


def read_file(filepath):
    with open(filepath) as f:
        str_text = f.read()
    return str_text


def prepare_sequence(sequence, train_len):
    return pad_sequences(sequence, maxlen=train_len, padding='pre')


def preprocess(train_path, token_path, dict_path, csv_path, train_len):
    tokenizer = Tokenizer(oov_token="<unk>")
    names = read_file(dict_path).split("\n")
    tokens = read_file(token_path).lower().split("\n")
    vocab = tokens + names + list(map(str, list(range(0, 10))))
    tokenizer.fit_on_texts([vocab])
    print(tokenizer.word_index)

    with open(csv_path, 'w', newline='') as excode_csv:
        writer = csv.writer(excode_csv)
        index = []
        for i in range(train_len - 1):
            index.append("input{}".format(i))
        index.append("output")
        writer.writerow(index)
        for f in os.listdir(train_path):
            text = read_file(os.path.join(train_path, f))
            lines = text.split("\n")
            text_sequences = []
            all_tokens = []
            for line in lines:
                low = line.strip().lower()
                if low == '':
                    continue
                if 'a' <= low[0] <= 'z':
                    all_tokens += tokenize(low)
                else:
                    all_tokens += [low]
            for j in range(1, len(all_tokens)):
                seq = all_tokens[max(j - train_len, 0):j]
                text_sequences.append(seq)
            # print(text_sequences)
            # break
            sequences = tokenizer.texts_to_sequences(text_sequences)
            writer.writerows(prepare_sequence(sequences, train_len))


data_types = ['train', 'validate', 'test']
for data_type in data_types:
    preprocess(train_path='../../../../../javaFileTokens/' + data_type + '/',
               token_path='../../../../../data_dict/java/java_words_n_symbols.txt',
               dict_path='../../../../../data_dict/java/names.txt',
               csv_path='../../../../../../data_csv/java/java_' + data_type + '.csv',
               train_len=20 + 1)
