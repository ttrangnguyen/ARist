from keras.preprocessing.text import Tokenizer
from pickle import dump, load

token_path = '../../../../../data_dict/methodcall/methodCall_eclipse.txt'


def read_file(filepath):
    with open(filepath) as f:
        str_text = f.read()
    return str_text


if __name__ == "__main__":
    vocab = read_file(token_path).split("\n")

    tokenizer = Tokenizer(oov_token='<UNK>')
    tokenizer.fit_on_texts([vocab])

    dump(tokenizer, open('method_call_tokenizer', 'wb'))
    # tokenizer = load(open('java_tokenizer', 'rb'))
    print(tokenizer.word_index)
