top_k = 10
train_len = 6 + 1
ngram = 4 + 1  # 5-gram
NGRAM_SCORE_WEIGHT = 1
USE_EXCODE_MODEL = True
USE_JAVA_MODEL = True
USE_METHOD_CALL_MODEL = False
USE_RNN = False
USE_NGRAM = True
USE_LEXSIM = True
USE_LOCAL_VAR = False
PARAM_LEXICAL_ONLY = False

LEXSIM_MULTIPLIER = 1
LEXSIM_SMALL_PENALTY = -3.32193  # log2(0.1)
LOCAL_VAR_BONUS = 1  # log2(2)

PRINT_LOG = False

project = 'all'
testfold = 0
# excode_model_rnn_path = "../../../../../model/excode_model_" + project + "_testfold_" + str(testfold) + ".h5"
# java_model_rnn_path = "../../../../../model/java_model_" + project + "_testfold_" + str(testfold) + ".h5"
method_call_model_rnn_path = "../../../../../model/v4-10fold-method/" + str(ngram) + " gram/method_call_model_" \
                             + project + "_testfold_" + str(testfold) + "_rnn.h5"
excode_model_rnn_path = "../../../../../model/excode_model_all.h5"
java_model_rnn_path = "../../../../../model/java_model_all.h5"
method_call_model_ngram_path = "../../../../../model/v4-10fold/" + str(ngram) + " gram/method_call_model_" \
                              + project + "_testfold_" + str(testfold) + "_ngram.pkl"
excode_model_ngram_path = "../../../../../model/v4-10fold/5 gram/excode_model_all_tokens_testfold_0_ngram.pkl"
java_model_ngram_path = "../../../../../model/v3-10fold-ngram/" + str(ngram) + " gram/java_model_" \
                        + project + "_testfold_" + str(testfold) + "_ngram.pkl"
excode_tokenizer_path = '../../../../src/main/python/model/excode/excode_tokenizer'
java_tokenizer_path = '../../../../src/main/python/model/java/java_tokenizer'
method_call_tokenizer_path = '../../../../src/main/python/model/method_call/method_call_eclipse_swt_tokenizer_3'
excode_tokens_path = '../../../../data_dict/excode/excode_tokens_n_symbols.txt'
