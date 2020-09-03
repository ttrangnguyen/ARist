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


def java_tokenize(lines, tokenizer, train_len, last_only=False):
    text_sequences = []
    all_tokens = []
    for line in lines:
        stripped = line.strip()
        if stripped == '':
            continue
        if 'a' <= stripped[0] <= 'z' or 'A' <= stripped[0] <= 'Z':
            all_tokens += tokenize(stripped)
        else:
            all_tokens += [stripped]
    if not last_only:
        for j in range(1, len(all_tokens)):
            seq = all_tokens[max(j - train_len, 0):j]
            text_sequences.append(seq)
    else:
        seq = all_tokens[max(len(all_tokens) - train_len, 0):len(all_tokens)]
        text_sequences.append(seq)
    sequences = tokenizer.texts_to_sequences(text_sequences)
    return sequences


def java_tokenize_one_sentence(lexes, tokenizer):
    text_sequences = []
    for lex in lexes:
        all_tokens = []
        stripped = lex.strip()
        if stripped == '':
            continue
        if 'a' <= stripped[0] <= 'z' or 'A' <= stripped[0] <= 'Z':
            all_tokens += tokenize(stripped)
        else:
            all_tokens += [stripped]
        text_sequences.append(all_tokens)
    sequences = tokenizer.texts_to_sequences(text_sequences)
    return sequences


def create_java_tokenizer(token_path, dict_path):
    tokenizer = Tokenizer(oov_token="<unk>")
    names = read_file(dict_path).split("\n")
    tokens = read_file(token_path).lower().split("\n")
    vocab = tokens + names + list(map(str, list(range(0, 10))))
    tokenizer.fit_on_texts([vocab])
    return tokenizer


def preprocess(train_path, token_path, dict_path, csv_path, train_len):
    tokenizer = create_java_tokenizer(token_path, dict_path)

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
            sequences = java_tokenize(lines, tokenizer, train_len)
            writer.writerows(prepare_sequence(sequences, train_len))


# data_types = ['train', 'validate', 'test']
# for data_type in data_types:
#     preprocess(train_path='../../../../../javaFileTokens/' + data_type + '/',
#                token_path='../../../../../data_dict/java/java_words_n_symbols.txt',
#                dict_path='../../../../../data_dict/java/names.txt',
#                csv_path='../../../../../../data_csv/java/java_' + data_type + '.csv',
#                train_len=20 + 1)
