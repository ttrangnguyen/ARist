import os
from model.tokenizer import Tokenizer
from name_stat.name_tokenizer import tokenize
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


train_len = 20 + 1
train_path = '../../../excodeFiles/training/'
token_path = 'all_tokens.txt'
dict_path = 'dict_4.txt'

tokenizer = Tokenizer(oov_token="<unk>")
names = read_file(dict_path).split("\n")
tokens = read_file(token_path).lower().split("\n")
vocab = [" "] + tokens + names + list(map(str, list(range(0, 10))))
tokenizer.fit_on_texts([vocab])
print(tokenizer.word_index)

with open('excode_train.csv', 'w', newline='') as excode_csv:
    writer = csv.writer(excode_csv)
    index = []
    for i in range(train_len-1):
        index.append("input{}".format(i))
    index.append("output")
    writer.writerow(index)
    for f in os.listdir(train_path):
        text = read_file(os.path.join(train_path, f))
        data = text.strip().split(" ")
        all_tokens = []
        text_sequences = []
        for word in data:
            all_tokens += modify(word, tokens) + [" "]
        for i in range(train_len,len(all_tokens)):
            seq = all_tokens[i-train_len:i]
            text_sequences.append(seq)
        sequences = tokenizer.texts_to_sequences(text_sequences)
        for sequence in sequences:
            writer.writerow(sequence)
