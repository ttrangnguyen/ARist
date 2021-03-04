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


def listdirs(folder):
    return [d for d in os.listdir(folder) if os.path.isdir(os.path.join(folder, d))]


def load_method_call_tokenizer():
    tokenizer = load(open('method_call_tokenizer', 'rb'))
    return tokenizer


def prepare_sequence(sequence, train_len):
    return pad_sequences(sequence, maxlen=train_len, padding='pre')


def tokenize(text, tokenizer, train_len):
    sequences = tokenizer.texts_to_sequences(text.split('\n'))
    print(sequences)
    ngrams = []
    for sequence in sequences:
        for i in range(len(sequence)):
            ngrams.append(sequence[max(0, i - train_len):i + 1])
    ngrams = prepare_sequence(ngrams, train_len)
    return ngrams


def preprocess(train_path, csv_path, train_len):
    tokenizer = load_method_call_tokenizer()

    with open(csv_path, 'w', newline='') as java_csv:
        writer = csv.writer(java_csv)
        index = []
        for i in range(train_len - 1):
            index.append("input{}".format(i))
        index.append("label")
        writer.writerow(index)
        for f in os.listdir(train_path):
            # print(f)
            text = read_file(os.path.join(train_path, f))
            sequences = tokenize(text, tokenizer, train_len)
            writer.writerows(sequences)

    import pandas as pd
    df = pd.read_csv(csv_path)
    cols = list(df.columns)
    cols[:train_len] = ["label"] + cols[:train_len - 1]
    df_reorder = df[cols]
    df_reorder.to_csv(csv_path, index=False)


if __name__ == '__main__':
    data_types = ['fold_' + str(x) for x in range(10)]
    data_parent_folders = ['data_csv_5_gram']
    train_len = [4 + 1]
    version = '4'
    n_folds = 10
    data_version_path = '../../../../../../data_v' + version + '/'

    for data_type in data_types:
        for i in range(len(data_parent_folders)):
            projects_path = data_version_path + 'data_classform/method_call' + '/' \
                            + str(n_folds) + '_folds' + '/' + data_type + '/'
            projects = ['eclipse']
            # projects = listdirs(projects_path)
            for project in projects:
                Path(data_version_path + data_parent_folders[i] + '/method_call/' + project). \
                    mkdir(parents=True, exist_ok=True)
                preprocess(train_path=projects_path + project + '/',
                           csv_path=data_version_path + data_parent_folders[i] + '/method_call/' + project
                                    + '/method_call_' + data_type + "_" + project + '.csv',
                           train_len = train_len[i])
