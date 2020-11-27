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

def prepare_sequence(sequence, train_len, *args):
    padded_sequences = pad_sequences(sequence, maxlen=train_len, padding='pre')
    rows = []
    for i in range(len(padded_sequences)):
        row = list(padded_sequences[i])
        for j in range(len(args)):
            row.append(args[j][i])
        rows.append(row)
    return rows


def java_tokenize_take_last(lines, tokenizer, train_len):
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
    # print(all_tokens)
    seq = all_tokens[max(len(all_tokens) - train_len, 0):len(all_tokens)]
    text_sequences.append(seq)
    sequences = tokenizer.texts_to_sequences(text_sequences)
    return sequences[0]


def java_tokenize(lines, tokenizer, train_len, last_only=False):
    text_sequences = []
    text_class_names = []
    text_method_names = []
    all_tokens = []

    method_name = ""
    class_name = ""
    inside_method = False

    n = len(lines)
    i = 0
    while i<n:
        if lines[i] == '`':
            # Class begin
            class_name = lines[i+1]
            i += 2
            if i >= n: break
        if lines[i] == '#':
            if inside_method == False:
                # Method begin
                method_name = lines[i+1]
                inside_method = True
                i += 2
            else:
                # Method end
                for j in range(1, len(all_tokens)):
                    seq = all_tokens[max(j - train_len, 0):j]
                    text_sequences.append(seq)
                    text_method_names.append(method_name)
                    text_class_names.append(class_name)
                all_tokens = []
                inside_method = False
                i += 1
        if inside_method:
            stripped = line.strip()
            if stripped == '':
                continue
            if 'a' <= stripped[0] <= 'z' or 'A' <= stripped[0] <= 'Z':
                all_tokens += tokenize(stripped)
            else:
                all_tokens += [stripped]
        i += 1
    # print(all_tokens)
    sequences = tokenizer.texts_to_sequences(text_sequences)
    class_names_tokens = tokenizer.texts_to_sequences(text_class_names)
    method_names_tokens = tokenizer.texts_to_sequences(text_method_names)
    return sequences, class_names_tokens, method_names_tokens


def java_tokenize_sentences(lexes, tokenizer, to_sequence=True):
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
        index=[]
        index.append("label")
        for i in range(train_len - 1):
            index.append("input{}".format(i))
        index.append("class_name")
        index.append("method_name")
        writer.writerow(index)
        for f in os.listdir(train_path):
            text = read_file(os.path.join(train_path, f))
            lines = text.split("\n")
            sequences, class_names_tokens, method_names_tokens = java_tokenize(lines, tokenizer, train_len)
            writer.writerows(prepare_sequence(sequences, train_len, class_names_tokens, method_names_tokens))



def listdirs(folder):
    return [d for d in os.listdir(folder) if os.path.isdir(os.path.join(folder, d))]


if __name__ == "__main__":
    data_types = ['train', 'validate', 'test']
    data_parent_folders = ['data_csv_21_gram', 'data_csv_3_gram', 'data_csv_7_gram']
    train_len = [20 + 1, 2 + 1, 6 + 1]
    for data_type in data_types:
        for i in range(len(data_parent_folders)):
            projects = listdirs("../../../../../../data_classform/java/" + data_type)
            for project in projects:
                Path('../../../../../../' + data_parent_folders[i] + '/java/' + project).mkdir(parents=True, exist_ok=True)
                preprocess(train_path='../../../../../../data_classform/java/' + data_type + '/' + project + '/',
                           csv_path='../../../../../../' + data_parent_folders[i] + '/java/' + project + '/java_' +
                                    data_type + "_" + project + '.csv',
                           train_len = train_len[i])
