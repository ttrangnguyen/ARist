top_k = 10
train_len = 6 + 1
ngram = 6 + 1  # 7-gram
NGRAM_SCORE_WEIGHT = 1
USE_EXCODE_MODEL = False
USE_JAVA_MODEL = False
USE_METHOD_CALL_MODEL = True
USE_RNN = True
USE_NGRAM = True
USE_LEXSIM = True
USE_LOCAL_VAR = False

LEXSIM_MULTIPLIER = 1
LEXSIM_SMALL_PENALTY = -3.32193  # log2(0.1)
LOCAL_VAR_BONUS = 1  # log2(2)

PRINT_LOG = False

project = 'eclipse'
testfold = 0
# excode_model_rnn_path = "../../../../../model/excode_model_" + project + "_testfold_" + str(testfold) + ".h5"
# java_model_rnn_path = "../../../../../model/java_model_" + project + "_testfold_" + str(testfold) + ".h5"
method_call_model_rnn_path = "../../../../../model/v4-10fold-method/" + str(ngram) + " gram/method_call_model_" \
                             + project + "_testfold_" + str(testfold) + "_rnn.h5"
excode_model_rnn_path = "../../../../../model/excode_model_all.h5"
java_model_rnn_path = "../../../../../model/java_model_all.h5"
method_call_model_ngram_path = "../../../../../model/v4-10fold-method/" + str(ngram) + " gram/method_call_model_" \
                              + project + "_testfold_" + str(testfold) + "_ngram.pkl"
excode_model_ngram_path = "../../../../../model/v3-10fold-ngram/" + str(ngram) + " gram/excode_model_" \
                          + project + "_testfold_" + str(testfold) + "_ngram.pkl"
java_model_ngram_path = "../../../../../model/v3-10fold-ngram/" + str(ngram) + " gram/java_model_" \
                        + project + "_testfold_" + str(testfold) + "_ngram.pkl"
excode_tokenizer_path = '../../../../src/main/python/model/excode/excode_tokenizer'
java_tokenizer_path = '../../../../src/main/python/model/java/java_tokenizer'
method_call_tokenizer_path = '../../../../src/main/python/model/method_call/method_call_eclipse_swt_tokenizer_3'
excode_tokens_path = '../../../../data_dict/excode/excode_tokens_n_symbols.txt'
