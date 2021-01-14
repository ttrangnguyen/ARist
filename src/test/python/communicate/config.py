project = 'all'

testfold = 0

excode_model_rnn_path = "../../../../../model/excode_model_" + project + "_testfold_" + str(testfold) + ".h5"
java_model_rnn_path = "../../../../../model/java_model_" + project + "_testfold_" + str(testfold) + ".h5"
excode_model_ngram_path = "../../../../../model/excode_model_" + project + "_testfold_" + str(testfold) + "_ngram.pkl"
java_model_ngram_path = "../../../../../model/java_model_" + project + "_testfold_" + str(testfold) + "_ngram.pkl"
excode_tokenizer_path = '../../../../src/main/python/model/excode/excode_tokenizer'
java_tokenizer_path = '../../../../src/main/python/model/java/java_tokenizer'
excode_tokens_path = '../../../../data_dict/excode/excode_tokens_n_symbols.txt'