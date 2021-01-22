top_k = 10
train_len = 6 + 1
ngram = 2 + 1  # 3-gram
ngram_weight = 1
USE_RNN = False
USE_NGRAM = True
USE_LEXSIM = True
LEXSIM_MULTIPLIER = 1
PRINT_LOG = False

project = 'all'
testfold = 0
excode_model_rnn_path = "../../../../../model/excode_model_" + project + "_testfold_" + str(testfold) + ".h5"
java_model_rnn_path = "../../../../../model/java_model_" + project + "_testfold_" + str(testfold) + ".h5"
excode_model_ngram_path = "../../../../../model/v3-10fold-ngram/" + str(ngram) + " gram/excode_model_" \
                          + project + "_testfold_" + str(testfold) + "_ngram.pkl"
java_model_ngram_path = "../../../../../model/v3-10fold-ngram/" + str(ngram) + " gram/java_model_" \
                        + project + "_testfold_" + str(testfold) + "_ngram.pkl"
excode_tokenizer_path = '../../../../src/main/python/model/excode/excode_tokenizer'
java_tokenizer_path = '../../../../src/main/python/model/java/java_tokenizer'
excode_tokens_path = '../../../../data_dict/excode/excode_tokens_n_symbols.txt'
