import os
from model.tokenizer import Tokenizer
from name_stat.name_tokenizer import tokenize
from keras.preprocessing.sequence import pad_sequences
import csv


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


def create_excode_tokenizer(token_path, dict_path):
    tokenizer = Tokenizer(oov_token="<unk>")
    names = read_file(dict_path).split("\n")
    tokens = read_file(token_path).lower().split("\n")
    vocab = tokens + names + list(map(str, list(range(0, 10))))
    tokenizer.fit_on_texts([vocab])
    print(tokenizer.word_index)
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
                for j in range(start_pos,len(all_tokens)):
                    seq = all_tokens[max(j-train_len,0):j]
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


def preprocess(train_path, token_path, dict_path, csv_path, train_len):
    tokenizer = create_excode_tokenizer(token_path, dict_path)

    with open(csv_path, 'w', newline='') as excode_csv:
        writer = csv.writer(excode_csv)
        index = []
        for i in range(train_len-1):
            index.append("input{}".format(i))
        index.append("output")
        writer.writerow(index)
        tokens = read_file(token_path).lower().split("\n")
        for f in os.listdir(train_path):
            text = read_file(os.path.join(train_path, f))
            # print(text_sequences)
            sequences = excode_tokenize(text, tokenizer, train_len, tokens)
            writer.writerows(prepare_sequence(sequences, train_len))


# data_types = ['train', 'validate', 'test']
# for data_type in data_types:
#     preprocess(train_path='../../../../../excodeFiles/' + data_type + '/',
#                token_path='../../../../../data_dict/excode/excode_tokens_n_symbols.txt',
#                dict_path='../../../../../data_dict/excode/names.txt',
#                csv_path='../../../../../../data_csv/excode/excode_' + data_type + '.csv',
#                train_len=20 + 1)
