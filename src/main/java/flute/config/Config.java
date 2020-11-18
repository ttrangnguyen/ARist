package flute.config;

import com.google.gson.Gson;
import flute.utils.file_processing.DirProcessor;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Config {
    public static String STORAGE_DIR = "storage/";

    public static String LOG_DIR = STORAGE_DIR + "logs/";

    /***
     Config for feature
     ***/

    public final static boolean FEATURE_USER_CHOOSE_METHOD = true;
    public final static boolean FEATURE_DFG_VARIABLE = true;

    public final static boolean FEATURE_ADD_FIELD_FROM_SUPER_INTERFACE = false;

    //Type feature

    public final static boolean FEATURE_PARAM_TYPE_ARRAY_ACCESS = false;
    public final static boolean FEATURE_PARAM_TYPE_CAST = false;

    public final static boolean FEATURE_PARAM_TYPE_TYPE_LIT = false;
    public final static boolean FEATURE_PARAM_TYPE_NULL_LIT = false;

    public final static boolean FEATURE_PARAM_TYPE_METHOD_INVOC = false;
    public final static boolean FEATURE_PARAM_TYPE_OBJ_CREATION = false;
    public final static boolean FEATURE_PARAM_TYPE_ARR_CREATION = false;

    public final static boolean FEATURE_PARAM_TYPE_COMPOUND = false;

    public final static boolean FEATURE_PARAM_STATIC_FIELD_ACCESS_FROM_CLASS = false;

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

    public static void loadSrcPath(String path, String parentFolderName) {
        List<File> fileList = DirProcessor.getAllEntity(new File(path), true);

        List<String> listSource = new ArrayList<>();
        List<String> encode = new ArrayList<>();

        for (File file : fileList) {
            if (file.getName().equals(parentFolderName)) {
                listSource.add(file.getAbsolutePath());
                encode.add("utf-8");
            }
        }

        SOURCE_PATH = listSource.toArray(new String[0]);
        ENCODE_SOURCE = encode.toArray(new String[0]);
    }

    public static void loadJarPath(String path) {
        List<File> fileList = DirProcessor.getAllEntity(new File(path), false);

        List<String> jarFiles = new ArrayList<>();

        for (File file : fileList) {
            if (com.google.common.io.Files.getFileExtension(file.getName()).equals("jar")) {
                jarFiles.add(file.getAbsolutePath());
            }
        }

        CLASS_PATH = jarFiles.toArray(new String[0]);
    }

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
