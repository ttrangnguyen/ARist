from pathlib import Path
import os
import re
from tokenizer import tokenize_subtoken
import np
from pickle import dump
from keras.preprocessing.text import Tokenizer


project = 'netbeans'
fold = '9'
src_root = f'/home/hieuvd/Kien/SLP-Modified-fulltoken/storage/gendata/{project}'
testfiles_path = f'/home/hieuvd/Kien/SLP-Modified-fulltoken/storage/testfilepath/{project}/fold{fold}/{project}.txt'
tokenizer_path = f'/home/hieuvd/Kien/Flute-LSTM/storage/tokenizer/small_corpus/{project}/{project}.tk'
Path(tokenizer_path).parent.mkdir(parents=True, exist_ok=True)

testfiles = open(testfiles_path, "r").read().splitlines()
words = set()

for subdir, dirs, files in os.walk(src_root):
    for file in files:
        abs_path = os.path.join(subdir, file)
        if not abs_path.endswith('java'):
            continue
        if abs_path[len('/home/hieuvd/Kien/SLP-Modified-fulltoken/storage/gendata/'):] in testfiles:
            continue
        try:
            src = open(abs_path, "rb").read().decode(errors="ignore")
            src = tokenize_subtoken(src)
            words.update(src)
        except:
            pass

words = list(words)
print(project, len(words))

tokenizer = Tokenizer(oov_token='<UNK>')
tokenizer.fit_on_texts([words])

dump(tokenizer,open(tokenizer_path, 'wb'))
print(len(tokenizer.word_index))