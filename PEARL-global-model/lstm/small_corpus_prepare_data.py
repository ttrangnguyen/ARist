from pathlib import Path
import os
import re
from tokenizer import tokenize_fulltoken
import np
from pickle import dump, load
from keras.preprocessing.text import Tokenizer
from keras.preprocessing.sequence import pad_sequences
import csv


project = 'netbeans'
fold = '2'
train_len = 6
src_root = f'/home/hieuvd/Kien/SLP-Modified-fulltoken/storage/gendata/{project}'
testfiles_path = f'/home/hieuvd/Kien/SLP-Modified-fulltoken/storage/testfilepath/{project}/fold{fold}/{project}.txt'
tokenizer_path = f'/home/hieuvd/Kien/Flute-LSTM/storage/tokenizer/small_corpus/{project}/{project}.tk'
data_path = f'/home/hieuvd/Kien/Flute-LSTM/storage/data/small_corpus/{project}/{project}_{fold}.csv'
Path(data_path).parent.mkdir(parents=True, exist_ok=True)

testfiles = open(testfiles_path, "r").read().splitlines()

def prepare_sequence(sequence, train_len):
    return pad_sequences(sequence, maxlen=train_len, padding='pre')

text_sequences = []

for subdir, dirs, files in os.walk(src_root):
    for file in files:
        abs_path = os.path.join(subdir, file)
        if not abs_path.endswith('java'):
            continue
        if abs_path[len('/home/hieuvd/Kien/SLP-Modified-fulltoken/storage/gendata/'):] in testfiles:
            continue
        try:
            src = open(abs_path, "rb").read().decode(errors="ignore")
            src = tokenize_fulltoken(src)
            for j in range(1, len(src)):
                seq = src[max(j - train_len, 0):j]
                text_sequences.append(seq)
        except:
            pass

tokenizer = load(open(tokenizer_path, 'rb'))
sequences = tokenizer.texts_to_sequences(text_sequences)
java_csv = open(data_path, 'w', newline='\n')
writer = csv.writer(java_csv)
index = []
for i in range(train_len - 1):
    index.append("input{}".format(i))
index.append("label")
writer.writerow(index)
writer.writerows(prepare_sequence(sequences, train_len))
