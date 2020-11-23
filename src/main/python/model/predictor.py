import numpy as np
from keras.preprocessing.sequence import pad_sequences


def prepare_sentence(seq, train_len, start_pos):
    # Pads seq and slides windows
    x = []
    y = []
    for i in range(start_pos, len(seq)):
        x_padded = pad_sequences([seq[:i]],
                                 maxlen=train_len - 1,
                                 padding='pre')[0]  # Pads before each sequence
        x.append(x_padded)
        y.append(seq[i])
    return x, y


def prepare(context, sentences, train_len, start_pos):
    x_test_all = []
    y_test_all = []
    sentence_len = []
    for sentence in sentences:
        x_test, y_test = prepare_sentence(context + sentence, train_len, start_pos)
        x_test_all += x_test
        y_test_all += y_test
        sentence_len += [len(x_test)]
    return np.array(x_test_all), np.array(y_test_all), sentence_len


def predict(model, x):
    return model.predict(x, workers=4, use_multiprocessing=True, batch_size=200)


def evaluate(p_pred, y_test, sentence_len):
    log_p_sentence = [0] * len(sentence_len)
    x_test_id = 0
    accumulate_len = 0

    for i, prob in enumerate(p_pred):
        # word = vocab_inv[y_test[i] + 1]  # Index 0 from vocab is reserved to <PAD>
        # history = ''.join([vocab_inv[w] for w in x_test[i, :] if w != 0])
        if i - accumulate_len == sentence_len[x_test_id]:
            accumulate_len += sentence_len[x_test_id]
            x_test_id += 1
        prob_word = prob[y_test[i]]
        log_p_sentence[x_test_id] += np.log(prob_word)
        # print('P(w={}|h={})={}'.format(word, history, prob_word))
        # print('Prob. sentence: {}'.format(log_p_sentence))
    return log_p_sentence


# def predict(model, sentence, tokenizer, train_len, start_pos):
#     # vocab = dict(tokenizer.word_index)
#     x_test, y_test = prepare_sentence(sentence, train_len, start_pos)
#     x_test = np.array(x_test)
#     y_test = np.array(y_test)
#     p_pred = model.predict(x_test)
#     # vocab_inv = {v: k for k, v in vocab.items()}
#     log_p_sentence = 0
#
#     for i, prob in enumerate(p_pred):
#         # word = vocab_inv[y_test[i] + 1]  # Index 0 from vocab is reserved to <PAD>
#         # history = ''.join([vocab_inv[w] for w in x_test[i, :] if w != 0])
#         prob_word = prob[y_test[i]]
#         log_p_sentence += np.log(prob_word)
#         # print('P(w={}|h={})={}'.format(word, history, prob_word))
#
#         # print('Prob. sentence: {}'.format(log_p_sentence))
#     return log_p_sentence
