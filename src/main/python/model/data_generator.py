import numpy as np
import keras
import pandas as pd
import sklearn
from keras.utils import to_categorical


class DataGenerator(keras.utils.Sequence):
    def __init__(self, data_path, data_size, batch_size, vocabulary_size,
                 to_fit=True, shuffle=True):
        self.data_path = data_path
        self.data_size = data_size
        self.batch_size = batch_size
        self.vocabulary_size = vocabulary_size
        self.to_fit = to_fit
        self.shuffle = shuffle
        # self.on_epoch_end()

    def __len__(self):
        return int(np.ceil(self.data_size / self.batch_size))

    def __getitem__(self, index):
        df = pd.read_csv(
            self.data_path, skiprows=range(1, index * self.batch_size),
            nrows=self.batch_size)
        if self.shuffle:
            df = sklearn.utils.shuffle(df)
        x = df.iloc[:, :-1]
        y = df.iloc[:, -1]
        return x, to_categorical(y, num_classes=self.vocabulary_size + 1)

