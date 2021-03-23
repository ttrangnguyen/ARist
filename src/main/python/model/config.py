TOP_K = 10
TRAIN_LEN_RNN = 6 + 1
NGRAM_EXCODE_PARAM = 12 + 1  # 13-gram
NGRAM_LEXICAL_PARAM = 12 + 1  # 13-gram
NGRAM_EXCODE_METHODCALL = 4 + 1
NGRAM_LEXICAL_METHODCALL = 4 + 1
NGRAM_SCORE_WEIGHT = 1
USE_EXCODE_MODEL = True
USE_JAVA_MODEL = True
USE_METHOD_CALL_MODEL = False
USE_RNN = False
USE_NGRAM = True
USE_LEXSIM = True
USE_LOCAL_VAR = False
PARAM_LEXICAL_ONLY = True

LEXSIM_MULTIPLIER = 1
LEXSIM_SMALL_PENALTY = -3.32193  # log2(0.1)
LOCAL_VAR_BONUS = 1  # log2(2)

PRINT_LOG = False

PROJECT = 'all'
TESTFOLD = 0
# excode_model_rnn_path = "../../../../../model/excode_model_" + project + "_testfold_" + str(testfold) + ".h5"
# java_model_rnn_path = "../../../../../model/java_model_" + project + "_testfold_" + str(testfold) + ".h5"
METHODCALL_MODEL_RNN_PATH = "../../../../../model/v4-10fold-method/" + str(NGRAM_EXCODE_METHODCALL) + " gram/method_call_model_" \
                             + PROJECT + "_testfold_" + str(TESTFOLD) + "_rnn.h5"
EXCODE_MODEL_RNN_PATH = "../../../../../model/excode_model_all.h5"
JAVA_MODEL_RNN_PATH = "../../../../../model/java_model_all.h5"
METHODCALL_MODEL_NGRAM_PATH = "../../../../../model/v4-10fold/" + str(NGRAM_LEXICAL_METHODCALL) + " gram/method_call_model_" \
                              + PROJECT + "_testfold_" + str(TESTFOLD) + "_ngram.pkl"
EXCODE_MODEL_NGRAM_PATH = "../../../../../model/v4-10fold/eclipse swt/excode - 13gram/excode_model_eclipse_swt_tokens_testfold_0full_vocab_ngram.pkl"
JAVA_MODEL_NGRAM_PATH = "../../../../../model/v4-10fold/eclipse swt/java - 10gram/java_model_eclipse_swt_tokens_testfold_0full_vocab_ngram.pkl"
EXCODE_TOKENIZER_PATH = '../../../../src/main/python/model/excode/excode_tokenizer'
JAVA_TOKENIZER_PATH = '../../../../src/main/python/model/java/java_tokenizer'
METHODCALL_TOKENIZER_PATH = '../../../../src/main/python/model/method_call/method_call_eclipse_swt_tokenizer_3'
EXCODE_TOKENS_PATH = '../../../../data_dict/excode/excode_tokens_n_symbols.txt'
