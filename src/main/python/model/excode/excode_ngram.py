from nltk.lm.preprocessing import padded_everygram_pipeline
import pandas as pd
from nltk.lm import MLE
from pickle import load
import dill
import time

project = 'all'
train_len = 5 + 1
train_len_str = '6'
train_csv_path = '../../../../../../data_csv_' + train_len_str + '_gram/excode/' + project + '/excode_train_' + project + '.csv'
validate_csv_path = '../../../../../../data_csv_' + train_len_str + '_gram/excode/' + project + '/excode_validate_' + project + '.csv'
tokenizer = load(open('excode_tokenizer', 'rb'))
col_list = ['label']
for i in range(train_len-1):
    col_list.append("input"+str(i))
data = pd.read_csv(train_csv_path,usecols=col_list).values.tolist() + pd.read_csv(validate_csv_path,usecols=col_list).values.tolist()
data = pd.DataFrame(data)
cols = data.columns.tolist()
cols = cols[1:] + cols[0:1]
data = data[cols]
data = data.values.tolist()
# data2 = []
# for i in range(train_len, len(data[0])+1):
#     if sum(data[0][i-train_len:i]) > 0:
#         data2 = data2 + [data[0][i-train_len:i]]
# for i in range(1, len(data)):
#     if sum(data[i][len(data[i])-train_len:len(data[i])]) > 0:
#         data2 = data2 + [data[i][len(data[i])-train_len:len(data[i])]]

data = tokenizer.sequences_to_texts(data)
modi = []
for d in data:
    modi += [d.split(' ')]
model = MLE(train_len)
train_data, padded_sents = padded_everygram_pipeline(train_len, modi)
print("Training...")
start_time = time.time()
model.fit(train_data, padded_sents)
print("--- %s minutes ---" % ((time.time() - start_time) / 60))

model_path = "../../../../../../model/excode_model_" + project + train_len_str + "ngram.pkl"
with open(model_path, 'wb') as fout:
    dill.dump(model, fout)