package flute.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ModelConfig {
    public enum TokenizedType {
        SUB_TOKEN, FULL_TOKEN
    }
    public static TokenizedType tokenizedType;

    // If token appearance >= cutoff then preserve
    public static final int LEXICAL_VOCABULARY_CUTOFF = 0;
    public static final int EXCODE_VOCABULARY_CUTOFF = 0;

    public static final int NGRAM = 6;

    // either USE_LEXICAL or USE_LEXSIM must be set to true
    public static final boolean USE_LEXICAL = true;
    public static final boolean USE_EXCODE = false;
    public static final boolean USE_LEXSIM = true;
    public static final boolean USE_RECENTNESS = false;

    // model architecture
    public static boolean USE_NESTED;
    public static boolean USE_CACHE;
    public static boolean USE_DYNAMIC;

    // weather use beam search
    public static boolean USE_BEAM_SEARCH;
    public static final int BEAM_WIDTH = 10;
    public static final int BEAM_SEARCH_MAX_ITERATION = 10;

    // weather use comma, dot
    public static final boolean CHECK_END_TOKEN = true;

    // turn this on if testing cross validation per file
    public static final boolean CROSS_VALIDATION_PER_FILE = false;

    public static String lexicalModelPath;
    public static String excodeModelPath;

    // vocab location
    public static String vocabPath;
    public static String argumentUsagePath;

    // PS
    public static boolean USE_PS;
    public static final String PS_EXCODE = "PS";
    public static final Double PS_RATE_REQUIREMENT = 0.4;
    public static boolean USE_PS_FILTER_OVERLAP;
    public static boolean USE_PS_RECENT_CLASS;

    // cugLM
    public static String cugLMModelPath = "storage/model/nestedcache/java/CugLM/CugLM.model";

    // special tokens
    public static HashSet<String> specialTokens;

    // recentness
    public static HashMap<Integer, Double> defRecentness;
    public static HashMap<Integer, Double> useRecentness;

    public static boolean USE_MAINTENANCE;

    public static boolean USE_NGRAM_GLOBAL;

    public static void init() {
        USE_PS = !USE_BEAM_SEARCH;
        USE_PS_FILTER_OVERLAP = USE_PS;
        USE_PS_RECENT_CLASS = USE_PS;
        USE_CACHE = USE_NESTED;
        USE_DYNAMIC = USE_NESTED;
        specialTokens = new HashSet<>();
        specialTokens.add("COMPOUND");
        specialTokens.add("LAMBDA");
        if (USE_RECENTNESS) {
            try {
                JsonReader getLocalJsonFile = new JsonReader(new FileReader("storage/def-use/def_recentness.json"));
                Type mapTokenType = new TypeToken<Map<Integer, Double>>(){}.getType();
                defRecentness = new Gson().fromJson(getLocalJsonFile, mapTokenType);
                getLocalJsonFile = new JsonReader(new FileReader("storage/def-use/use_recentness.json"));
                mapTokenType = new TypeToken<Map<Integer, Double>>(){}.getType();
                useRecentness = new Gson().fromJson(getLocalJsonFile, mapTokenType);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (Config.mode == Config.Mode.EVALUATE) return;
        if (ProjectConfig.CUGLM) {
            if (!USE_NESTED) {
                cugLMModelPath = cugLMModelPath.replace("nestedcache", "global");
            }
            lexicalModelPath = cugLMModelPath;
        } else {
            if (TestConfig.lastParcFold != null) {
                lexicalModelPath = "storage/model/sensibility/"
                        + ProjectConfig.project + "/"
                        + ProjectConfig.project + "_until_" + TestConfig.lastParcFold
                        + ".model";
            }
            else lexicalModelPath = "storage/model/" +
                    (!USE_NESTED?"global":(USE_CACHE?"nestedcache":"nested")) +
                    "/java/" + ProjectConfig.project + "/" +
                    (TestConfig.fold.equals("")?"":("fold"+TestConfig.fold+"/"))
                    + ProjectConfig.project + ".model";
            excodeModelPath = "storage/model/" +
                    (!USE_NESTED?"global":(USE_CACHE?"nestedcache":"nested")) +
                    "/excode/" + ProjectConfig.project + "/" +
                    (TestConfig.fold.equals("")?"":("fold"+TestConfig.fold+"/"))
                    + ProjectConfig.project + ".model";
            vocabPath = "storage/vocab/"
                    + ProjectConfig.project + "/" + ProjectConfig.project + ".vocab";
            argumentUsagePath = String.format("storage/statistics/pbcnt/%s_usage_test%s.csv",
                    ProjectConfig.project, TestConfig.fold);
        }
    }
}
