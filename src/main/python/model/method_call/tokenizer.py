from keras.preprocessing.text import Tokenizer
from pickle import dump, load

token_path = '../../../../../data_dict/methodcall/method_call_eclipse_swt.txt'


def read_file(filepath):
    with open(filepath) as f:
        str_text = f.read()
    return str_text


if __name__ == "__main__":
    lower_bound = 3
    vocab = []
    for line in read_file(token_path).split('\n'):
        i = len(line)-1
        freq = 0
        method = ''
        pow_10 = 1
        while True:
            if not line[i].isdigit():
                method = line[:i]
                break
            else:
                freq += int(line[i]) * pow_10
                pow_10 *= 10
            i -= 1
        if freq >= lower_bound:
            vocab.append(method)

    tokenizer = Tokenizer(oov_token='<UNK>')
    tokenizer.fit_on_texts([vocab])

    # dump(tokenizer, open('method_call_eclipse_swt_tokenizer_3', 'wb'))
    tokenizer = load(open('method_call_eclipse_swt_tokenizer_3', 'rb'))
    # print(tokenizer.texts_to_sequences([['org.eclipse.swt.widgets.Widget.getData(java.lang.String)java.lang.Object'], ['org.eclipse.swt.widgets.Widget.getData(java.lang.String)java.lang.Object']]))
    print(tokenizer.word_index)
