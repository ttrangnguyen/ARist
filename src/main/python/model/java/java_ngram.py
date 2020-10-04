from nltk.lm.preprocessing import padded_everygram_pipeline
import pandas as pd
from nltk.lm import MLE
from pickle import load
import dill
import time

project = 'ant'
train_csv_path = '../../../../../../data_csv/java/' + project + '/java_train_' + project + '.csv'
validate_csv_path = '../../../../../../data_csv/java/' + project + '/java_validate_' + project + '.csv'
data = pd.read_csv(train_csv_path).values.tolist() + pd.read_csv(validate_csv_path).values.tolist()
tokenizer = load(open('java_tokenizer', 'rb'))
data = tokenizer.sequences_to_texts(data)
modi = []
for d in data:
    modi += [d.split(' ')]
for i in range(100):
    print(modi[i])
# Preprocess the tokenized text for 3-grams language modelling
train_len = 6 + 1

model = MLE(train_len)
train_data, padded_sents = padded_everygram_pipeline(train_len, modi)

# start_time = time.time()
# model.fit(train_data, padded_sents)
# print("--- %d minutes ---" % ((time.time() - start_time) / 60))
#
#
# with open('java_ngram.pkl', 'wb') as fout:
#     dill.dump(model, fout)
# print(model.vocab)

# print(model.vocab.lookup(tokenized_text[0]))

# print(model.vocab.lookup('language is never random lah .'.split()))