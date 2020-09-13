import os
from name_stat.name_tokenizer import tokenize
from keras.preprocessing.sequence import pad_sequences
import csv
from pickle import load
from pathlib import Path


def read_file(filepath):
    with open(filepath) as f:
        str_text = f.read()
    return str_text


def modify(word, tokens):
    # method token
    if word[:6] == "METHOD":
        return ["method"] + tokenize(word[6:])
    # var token
    if word[:3] == "VAR":
        return ["var"] + tokenize(word[3:word.find(",")] + ")")
    # other excode tokens without (
    if word.lower() in tokens:
        return [word.lower()]
    # excode tokens with ( : F_ACCESS, M_ACCESS, C_CALL
    open_paren = word.find('(')
    if open_paren > -1:
        return [word[:open_paren].lower()] + tokenize(word[open_paren:])
    else:
        return [word.lower()]


def prepare_sequence(sequence, train_len):
    return pad_sequences(sequence, maxlen=train_len, padding='pre')


def create_java_tokenizer():
    tokenizer = load(open('excode_tokenizer', 'rb'))
    return tokenizer


def excode_tokenize(text, tokenizer, train_len, tokens, last_only=False):
    data = text.strip().split(" ")
    text_sequences = []
    i = 0
    while i < len(data):
        if data[i][:7] == "METHOD{":
            all_tokens = modify(data[i], tokens)
            start_pos = len(all_tokens)
            i += 1
            while i < len(data) and data[i] != "ENDMETHOD":
                all_tokens += modify(data[i], tokens)
                i += 1
            if not last_only:
                for j in range(start_pos, len(all_tokens)):
                    seq = all_tokens[max(j - train_len, 0):j]
                    text_sequences.append(seq)
            else:
                seq = all_tokens[max(len(all_tokens) - train_len, 0):len(all_tokens)]
                text_sequences = [seq]
        i += 1
    sequences = tokenizer.texts_to_sequences(text_sequences)
    return sequences


def excode_tokenize_candidates(candidates, tokenizer, tokens):
    text_sequences = []
    for candidate in candidates:
        all_tokens = []
        words = candidate.strip().split(" ")
        for word in words:
            all_tokens += modify(word, tokens)
        text_sequences.append(all_tokens)
    sequences = tokenizer.texts_to_sequences(text_sequences)
    return sequences


def create_excode_tokenizer():
    tokenizer = load(open('excode_tokenizer', 'rb'))
    return tokenizer


def preprocess(train_path, token_path, csv_path, train_len):
    tokenizer = create_excode_tokenizer()

    with open(csv_path, 'w', newline='') as excode_csv:
        writer = csv.writer(excode_csv)
        index = []
        for i in range(train_len - 1):
            index.append("input{}".format(i))
        index.append("output")
        writer.writerow(index)
        tokens = read_file(token_path).lower().split("\n")
        for f in os.listdir(train_path):
            text = read_file(os.path.join(train_path, f))
            # print(text_sequences)
            sequences = excode_tokenize(text, tokenizer, train_len, tokens)
            writer.writerows(prepare_sequence(sequences, train_len))


def listdirs(folder):
    return [d for d in os.listdir(folder) if os.path.isdir(os.path.join(folder, d))]


# data_types = ['train', 'validate', 'test']
# for data_type in data_types:
#     projects = listdirs("../../../../../excodeFiles/" + data_type)
#     for project in projects:
#         Path('../../../../../../data_csv/excode/' + project).mkdir(parents=True, exist_ok=True)
#         preprocess(train_path='../../../../../excodeFiles/' + data_type + '/' + project + '/',
#                    token_path='../../../../../data_dict/excode/excode_tokens_n_symbols.txt',
#                    csv_path='../../../../../../data_csv/excode/' + project + '/excode_' +
#                             data_type + "_" + project + '.csv',
#                    train_len=20 + 1)
