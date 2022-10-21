package flute.config;

import static flute.config.ProjectConfig.CUGLM;

public class TestConfig {
    public static String fold;

    public final static int TOP_K = 50;

    public enum PredictType {
        FIRST_TOKEN, SEQUENCE
    }

    public static PredictType predictType;
    public final static boolean TEMPORARY_CORRECT_TEST = false;
    public final static Double INFINITE_NEGATIVE = -100.0;

    public static String testFilePath;
    public static String testCasesFilePath;
    public static String predictionDetailPath;
    public static String predictionResultPath;

    public static boolean EVALUATE_PARC;

    public static Integer lastParcFold = null;

    public static void init() {
        if (CUGLM) {
            testCasesFilePath = "/home/hieuvd/Tannm/Flute/storage/logs/" + ProjectConfig.project + "_ArgRecTests.txt";
            predictionDetailPath = "storage/result-cuglm" +
                    (ModelConfig.USE_MAINTENANCE?"-maintenance":
                    (ModelConfig.USE_DYNAMIC?"-dynamic":"-static"))
                    + (ModelConfig.USE_BEAM_SEARCH?"-beamsearch":"")
                    + "/" + ProjectConfig.project + "/"
                    + ProjectConfig.project + "_prediction_detail_flute_" +
                    (predictType==PredictType.SEQUENCE ?"sequence":"firsttoken") + ".txt";
        } else {
            testCasesFilePath = "storage/testcase/" +
                    ProjectConfig.project + "/" +
                    (fold == null?"":("fold"+fold+"/")) +
                    ProjectConfig.project + "_ArgRecTests" +
                    "_fold" + fold + ".txt";
            if (TestConfig.lastParcFold != null)
                predictionDetailPath = "storage/result_sensitivity/" + ProjectConfig.project
                    + "/" + (fold == null?"":("fold"+fold+"/"))
                    + ProjectConfig.project + "_prediction_detail_flute_" +
                    (predictType==PredictType.SEQUENCE ?"sequence":"firsttoken") +
                        "_until" + TestConfig.lastParcFold + ".txt";
            else
                predictionDetailPath = "storage/result/" + ProjectConfig.project
                        + "/" + (fold == null?"":("fold"+fold+"/"))
                        + ProjectConfig.project + "_prediction_detail_flute_" +
                        (predictType==PredictType.SEQUENCE ?"sequence":"firsttoken") + ".txt";
        }
        predictionResultPath = predictionDetailPath
                .replace("_prediction_detail_flute_", "_prediction_result_flute_")
                .replace(".txt", ".csv");
    }
}
