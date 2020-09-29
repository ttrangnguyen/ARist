package flute.config;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Config {
    public static String STORAGE_DIR = "storage/";

    public static String LOG_DIR = STORAGE_DIR + "logs/";

    /***
     Config for feature
     ***/

    public final static boolean FEATURE_USER_CHOOSE_METHOD = true;
    public final static boolean FEATURE_DFG_VARIABLE = true;

    public final static boolean FEATURE_TYPE_METHOD_CALL = true;

    /***
     Config for crawler
     ***/

    public static String REPO_DIR = STORAGE_DIR + "repositories/";

    /***
     Config for project
     ***/

    public static String PROJECT_NAME = "";

    public static String PROJECT_DIR = "";
    public static String[] SOURCE_PATH = {};

    public static String[] ENCODE_SOURCE = {};

    public static String[] CLASS_PATH = {
    };

    public static int JDT_LEVEL = 13;
    public static String JAVA_VERSION = "13";

    public static String[] IGNORE_FILES = new String[]{};

    /***
     Config for test
     ***/

    public static String TEST_FILE_PATH = "";
    public static int TEST_POSITION = 1662;

    public static void loadConfig(String filePath) throws IOException {
        Gson gson = new Gson();
        // Create a reader
        Reader reader = Files.newBufferedReader(Paths.get(filePath));
        // Convert JSON object
        ConfigSchema config = gson.fromJson(reader, ConfigSchema.class);
        reader.close();

        PROJECT_NAME = config.getName();
        PROJECT_DIR = config.getProjectDir();
        SOURCE_PATH = config.getSourcePaths().toArray(new String[0]);
        ENCODE_SOURCE = config.getEncodeSources().toArray(new String[0]);
        CLASS_PATH = config.getClassPaths().toArray(new String[0]);
        JDT_LEVEL = config.getJdtLevel();
        JAVA_VERSION = config.getJavaVersion();
        IGNORE_FILES = config.getIgnoreFiles().toArray(new String[0]);
        TEST_FILE_PATH = config.getTestFilePath();
        TEST_POSITION = config.getTestPosition();
    }

}
