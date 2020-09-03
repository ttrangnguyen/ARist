if __name__ == '__main__':
    import csv
    import os
    import numpy as np
    import pandas as pd
    from model.tokenizer import Tokenizer
    from keras.utils import to_categorical
    from keras.models import Sequential
    from keras.layers import LSTM, Dense, Dropout, Embedding
    from keras import optimizers
    from keras.preprocessing.sequence import pad_sequences
    import sys
    from keras.callbacks import ModelCheckpoint
    from model.data_generator import DataGenerator
    import sklearn

    def read_file(filepath):
        with open(filepath) as f:
            str_text = f.read()
        return str_text


    train_len = 20 + 1
    text_sequences = []
    count = 1
    token_path = '../../../../../data_dict/excode/excode_tokens_n_symbols.txt'
    dict_path = '../../../../../data_dict/excode/names.txt'

    tokenizer = Tokenizer(oov_token="<unk>")
    names = read_file(dict_path).split("\n")
    tokens = read_file(token_path).lower().split("\n")
    vocab = tokens + names + list(map(str, list(range(0, 10))))
    tokenizer.fit_on_texts([vocab])
    f = open('../../../../../data_dict/excode/excode_names.txt', "w")
    for key in tokenizer.word_index:
        f.write(key)
        f.write('\n')
    f.close()
    vocabulary_size = len(tokenizer.word_index)


    def load_data(excode_csv_path, idx, batch_size):
        df = pd.read_csv(
            excode_csv_path, skiprows=range(1, idx * batch_size),
            nrows=batch_size)
        df = sklearn.utils.shuffle(df)
        x = df.iloc[:, :-1]
        y = df.iloc[:, -1]
        return np.array(x), to_categorical(y, num_classes=vocabulary_size + 1)


    def batch_generator(excode_csv_path, batch_size, steps):
        idx = 1
        while True:
            yield load_data(excode_csv_path, idx - 1, batch_size)
            if idx < steps:
                idx += 1
            else:
                idx = 1


    def count_lines_csv(file_path):
        input_file = open(file_path, "r+")
        reader_file = csv.reader(input_file)
        return len(list(reader_file))


    train_csv_path = 'excode_train.csv'
    validate_csv_path = 'excode_validate.csv'
    batch_size = 512
    train_data_size = count_lines_csv(train_csv_path)
    validate_data_size = count_lines_csv(validate_csv_path)
    steps_per_epoch = np.ceil(train_data_size / batch_size)
    validation_steps = np.ceil(validate_data_size / batch_size)
    # training_batch_generator = batch_generator(train_csv_path, batch_size, steps_per_epoch)
    # validation_batch_generator = batch_generator(validate_csv_path, batch_size, validation_steps)
    training_batch_generator = DataGenerator(train_csv_path, train_data_size, batch_size, vocabulary_size)
    validation_batch_generator = DataGenerator(validate_csv_path, validate_data_size, batch_size, vocabulary_size)


    def create_model(vocabulary_size, seq_len):
        model = Sequential()
        model.add(Embedding(vocabulary_size, seq_len, input_length=seq_len))
        model.add(LSTM(128))
        # model.add(Dropout(0.15))
        # model.add(LSTM(64,recurrent_dropout=0.1))
        # model.add(Dropout(0.2))
        # model.add(Dense(64,activation='relu'))
        # model.add(Dropout(0.2))
        model.add(Dense(vocabulary_size, activation='softmax'))
        opt_adam = optimizers.Adam(lr=0.003)
        model.compile(loss='categorical_crossentropy', optimizer=opt_adam, metrics=['accuracy'])
        model.summary()
        return model

    model_path = "excode_pred_Model.h5"
    model = create_model(vocabulary_size + 1, train_len - 1)
    # model = mlflow.keras.load_model("runs:/f911bbd0f82d46bc8e1e4d4c649fb445/model")
    checkpoint = ModelCheckpoint(model_path, monitor='loss', verbose=1, save_best_only=True, mode='min')
    # mlflow.create_experiment("keras_test")
    # mlflow.create_experiment("D:/Research/AutoSC/SLAMC/Excode")
    epoch = 1
    model.fit_generator(generator=training_batch_generator,
                        epochs=epoch,
                        verbose=1, validation_data=validation_batch_generator,
                        use_multiprocessing=True,
                        callbacks=[checkpoint],
                        shuffle=True)