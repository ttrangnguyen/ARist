class Tokenizer:
    word_index = {}
    oov_token = ""

    def __init__(self, oov_token=""):
        self.oov_token = oov_token
        self.word_index[oov_token] = 1

    def fit_on_texts(self, texts):
        for text in texts:
            for s in text:
                if s not in self.word_index.keys():
                    self.word_index[s] = len(self.word_index) + 1

    def texts_to_sequences(self, texts):
        sequences = []
        for text in texts:
            sequence = []
            for s in text:
                if s not in self.word_index.keys():
                    if self.oov_token != "":
                        sequence.append(self.word_index.get(self.oov_token))
                else:
                    sequence.append(self.word_index.get(s))
            sequences.append(sequence)
        return sequences
