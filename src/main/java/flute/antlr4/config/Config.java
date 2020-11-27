package flute.antlr4.config;

/**
 * Created by Minology on 11:46 SA
 */

public class Config {
    public static final String dataVersion = "1";
    public static final String projectsPath = "../../Research/java-data/";
    public static final String projectsSrcPath = "../../Research/java-data/";
    public static final String parsingResult = "parsingResult.txt/";
    public static final String parsingResultFast = "parsingResultFast.txt/";
    public static final int maxParsingTimeInMillis = 100;
    public static final String excodeCsvPath = "excodeFiles/excode.csv/";
    public static final String excodeJSONPath = "excodeFiles/excode.json/";
    public static final String dataRoot = "../data_v" + dataVersion + "/";
    public static final String excodeTrainDataPath = dataRoot + "data_classform/excode/train/";
    public static final String excodeTestDataPath = dataRoot + "data_classform/excode/test/";
    public static final String excodeValidateDataPath = dataRoot + "data_classform/excode/validate/";
    public static final String javaTrainDataPath = dataRoot + "data_classform/java/train/";
    public static final String javaTestDataPath = dataRoot + "data_classform/java/test/";
    public static final String javaValidateDataPath = dataRoot + "data_classform/java/validate/";
    public static final String trainFilesPath = dataRoot + "datapath/train/";
    public static final String validateFilesPath = dataRoot + "datapath/validate/";
    public static final String testFilesPath = dataRoot + "datapath/test/";
}
