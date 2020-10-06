import os
from name_stat.name_tokenizer import tokenize
from keras.preprocessing.sequence import pad_sequences
from pickle import load
import csv
from pathlib import Path


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


def java_tokenize_one_sentence(lexes, tokenizer, to_sequence=True):
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
    if to_sequence:
        sequences = tokenizer.texts_to_sequences(text_sequences)
        return sequences
    else:
        return text_sequences


def create_java_tokenizer():
    tokenizer = load(open('java_tokenizer', 'rb'))
    return tokenizer


def preprocess(train_path, csv_path, train_len):
    tokenizer = create_java_tokenizer()

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


def listdirs(folder):
    return [d for d in os.listdir(folder) if os.path.isdir(os.path.join(folder, d))]


# data_types = ['train', 'validate', 'test']
# for data_type in data_types:
#     projects = listdirs("../../../../../../data_classform/java/" + data_type)
#     for project in projects:
#         Path('../../../../../../data_csv/java/' + project).mkdir(parents=True, exist_ok=True)
#         preprocess(train_path='../../../../../../data_classform/java/' + data_type + '/' + project + '/',
#                    csv_path='../../../../../../data_csv/java/' + project + '/java_' +
#                             data_type + "_" + project + '.csv',
#                    train_len = 6 + 1)
