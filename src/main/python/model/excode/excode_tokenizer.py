from keras.preprocessing.text import Tokenizer
from pickle import dump, load

token_path = '../../../../../data_dict/excode/excode_tokens_n_symbols.txt'
dict_path = '../../../../../data_dict/excode/names.txt'


def read_file(filepath):
    with open(filepath) as f:
        str_text = f.read()
    return str_text


names = read_file(dict_path).split("\n")
tokens = read_file(token_path).lower().split("\n")
vocab = tokens + names + list(map(str, list(range(0, 10))))

tokenizer = Tokenizer(oov_token='<UNK>')
tokenizer.fit_on_texts([vocab])

dump(tokenizer, open('excode_tokenizer', 'wb'))
tokenizer = load(open('excode_tokenizer', 'rb'))
print(tokenizer.word_index)