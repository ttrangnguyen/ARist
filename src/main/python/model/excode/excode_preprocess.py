import os
from name_stat.name_tokenizer import tokenize
from keras.preprocessing.sequence import pad_sequences
import csv
from pickle import load
from pathlib import Path
from pickle import load


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


def prepare_sequence(sequence, train_len, *args):
    padded_sequences = pad_sequences(sequence, maxlen=train_len, padding='pre')
    rows = []
    for i in range(len(padded_sequences)):
        row = list(padded_sequences[i])
        for j in range(len(args)):
            row.append(args[j][i])
        rows.append(row)
    return rows


def get_method_name(method_token):
    return method_token[method_token.find(",")+1:-1]


def get_method_name_tokens(method_token):
    method_name = get_method_name(method_token)
    return tokenize(method_name)


def excode_tokenize(text, tokenizer, train_len, tokens, method_only=True):
    data = text.strip().split(" ")
    text_sequences = []
    text_method_names = []
    if method_only:
        i = 0
        while i < len(data):
            if data[i][:7] == "METHOD{":
                method_name_tokens = get_method_name_tokens(data[i])
                all_tokens = modify(data[i], tokens)
                start_pos = len(all_tokens)
                i += 1
                while i < len(data) and data[i] != "ENDMETHOD":
                    all_tokens += modify(data[i], tokens)
                    i += 1
                for j in range(start_pos, len(all_tokens)):
                    seq = all_tokens[max(j - train_len, 0):j]
                    text_sequences.append(seq)
                    text_method_names.append(method_name_tokens)
            i += 1
        sequences = tokenizer.texts_to_sequences(text_sequences)
        method_names_tokens = tokenizer.texts_to_sequences(text_method_names)
        return sequences, method_names_tokens
    else:
        all_tokens = []
        for d in data:
            all_tokens += modify(d, tokens)
        seq = all_tokens[max(len(all_tokens) - train_len, 0):len(all_tokens)]
        text_sequences = [seq]
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
        index=[]
        index.append("label")
        for i in range(train_len - 1):
            index.append("input{}".format(i))
        index.append("class_name")
        index.append("method_name")
        writer.writerow(index)
        tokens = read_file(token_path).lower().split("\n")
        for f in os.listdir(train_path):
            text = read_file(os.path.join(train_path, f))
            # print(text_sequences)
            sequences, method_names_tokens = excode_tokenize(text, tokenizer, train_len, tokens)
            writer.writerows(prepare_sequence(sequences, train_len, method_names_tokens))

    import pandas as pd
    df = pd.read_csv(csv_path)
    cols = list(df.columns)
    cols[:train_len] = ["label"] + cols[:train_len-1]
    df_reorder = df[cols]
    df_reorder.to_csv(csv_path, index=False)


def listdirs(folder):
    return [d for d in os.listdir(folder) if os.path.isdir(os.path.join(folder, d))]


if __name__ == '__main__':
    data_types = ['train', 'validate', 'test']
    data_parent_folders = ['data_csv_21_gram', 'data_csv_3_gram', 'data_csv_7_gram']
    train_len = [20 + 1, 2 + 1, 6 + 1]
    for data_type in data_types:
        for i in range(len(data_parent_folders)):
            projects = listdirs("../../../../../../data_classform/excode/" + data_type)
            for project in projects:
                Path('../../../../../../' + data_parent_folders[i] + '/excode/' + project).mkdir(parents=True, exist_ok=True)
                preprocess(train_path='../../../../../../data_classform/excode/' + data_type + '/' + project + '/',
                           token_path='../../../../../data_dict/excode/excode_tokens_n_symbols.txt',
                           csv_path='../../../../../../' + data_parent_folders[i] + '/excode/' + project + '/excode_' +
                                    data_type + "_" + project + '.csv',
                           train_len = train_len[i])
