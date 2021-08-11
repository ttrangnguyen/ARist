package flute.config;

import com.google.gson.Gson;
import flute.utils.file_processing.DirProcessor;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Config {
    public static String STORAGE_DIR = "storage/";

    public static String LOG_DIR = STORAGE_DIR + "logs/";
    public static String JAVAFX_DIR = STORAGE_DIR + "lib/JavaFX/";

    public final static int PARAM_SERVICE_PORT = 18007;
    public final static int METHOD_NAME_SERVICE_PORT = 17007;

    public final static String MVN_HOME = "/usr/local/Cellar/maven/3.6.3_1/libexec";
    public static boolean TARGET_PARAM_POSITION = false;

    public static boolean API_CRAWLER = false;

    /***
     * Logging
     */

    public final static float PRINT_PROGRESS_DELTA = 0.001f; //0.1%
    public final static int PROGRESS_SIZE = 40;

    /***
     * For training, analyser
     */

    public final static boolean IGNORE_JAVADOC = false;
    public final static boolean IGNORE_PARSE_AFTER_SET_POSITION = false;
    public final static List<String> BLACKLIST_FOLDER_SRC = Arrays.asList(new String[]{"lib", ".idea", "out", "test", "demo", "example", "examples"}); //for filter project folder
    public final static List<String> BLACKLIST_NAME_SRC = Arrays.asList(new String[]{"test", "demo", "example"}); //for filter file path

    /***
     * Multiprocess
     */

    public final static boolean MULTIPROCESS = false;
    public final static int NUM_THREAD = 5;

    /***
     * Config for feature
     */

    public static boolean FEATURE_USER_CHOOSE_METHOD = true;
    public final static boolean FEATURE_DFG_VARIABLE = true;

    public final static boolean FLAG_ALL = true;
    public final static boolean FEATURE_ADD_FIELD_AND_METHOD_FROM_SUPER_INTERFACE = false|FLAG_ALL;
    /***
     *  Type feature
     */
    public final static boolean FEATURE_PARAM_TYPE_ARRAY_ACCESS = false|FLAG_ALL;
    public final static boolean FEATURE_PARAM_TYPE_CAST = false|FLAG_ALL;

    public final static boolean FEATURE_PARAM_TYPE_TYPE_LIT = false|FLAG_ALL;
    public final static boolean FEATURE_PARAM_TYPE_NULL_LIT = false|FLAG_ALL;

    public final static boolean FEATURE_PARAM_TYPE_METHOD_INVOC = false|FLAG_ALL;
    public final static boolean FEATURE_PARAM_TYPE_OBJ_CREATION = false|FLAG_ALL;
    public final static boolean FEATURE_PARAM_TYPE_ARR_CREATION = false|FLAG_ALL;
    public final static boolean FEATURE_PARAM_TYPE_LAMBDA = false|FLAG_ALL;
    public final static boolean FEATURE_PARAM_TYPE_COMPOUND = false;

    public final static boolean FEATURE_PARAM_STATIC_FIELD_ACCESS_FROM_CLASS = true;

    public final static boolean FEATURE_LIMIT_CANDIDATES = false;

    /***
     *  Method name feature
     */

    public final static boolean FEATURE_IGNORE_NATIVE_METHOD = false;
    public final static boolean FEATURE_ONLY_VOID_FOR_STMT = false;

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

    public static String PUBLIC_STATIC_MEMBER_PATH = "storage/flute-ide/%s_public_static_members.txt";
    /***
     Config for test
     ***/

    public final static boolean TEST_ARG_ONE_BY_ONE = true;
    public final static boolean TEST_ZERO_ARG = false;
    public final static boolean TEST_LEX_SIM = false;

    public static String TEST_FILE_PATH = "";
    public static int TEST_POSITION = 1662;
    public static final String[] TEST_APIS = {
//            "org.eclipse.swt",  //Eclipse (SWT)
//            "java.awt",         //Netbeans (AWT)
//            "javax.swing",      //Netbeans (Swing)
    };

    public static void enablePluginProcess() {
        Config.FEATURE_USER_CHOOSE_METHOD = false;
        Config.TARGET_PARAM_POSITION = true;
    }

    public static void loadSrcPath(String path, String parentFolderName) throws FileNotFoundException {
        File parentFolder = new File(path);
        if (!parentFolder.exists()) throw new FileNotFoundException("Folder doesn't exists! " + path);
        List<File> fileList = DirProcessor.getAllEntity(parentFolder, true);

        List<String> listSource = new ArrayList<>();
        List<String> encode = new ArrayList<>();

        for (File file : fileList) {
            if (file.getAbsolutePath().replace("\\", "/").endsWith(parentFolderName)) {
                listSource.add(file.getAbsolutePath());
                encode.add("utf-8");
            }
        }

        SOURCE_PATH = ArrayUtils.addAll(SOURCE_PATH, listSource.toArray(new String[0]));
        ENCODE_SOURCE = ArrayUtils.addAll(ENCODE_SOURCE, encode.toArray(new String[0]));
    }

    public static void loadSrcPathFromPackage(String path, String packageName) throws FileNotFoundException {
        File parentFolder = new File(path);
        if (!parentFolder.exists()) throw new FileNotFoundException("Folder doesn't exists! " + path);
        List<File> fileList = DirProcessor.getAllEntity(parentFolder, true);

        List<String> listSource = new ArrayList<>();
        List<String> encode = new ArrayList<>();

        for (File file : fileList) {
            if (!file.getAbsolutePath().contains("src") && file.getAbsolutePath().replace("\\", "/")
                    .endsWith(packageName.replace(".", "/"))) {
                File parentFile = file;
                int parentLength = packageName.split("\\.").length;
                for (int i = 0; i < parentLength; i++) {
                    parentFile = parentFile.getParentFile();
                }
                listSource.add(parentFile.getAbsolutePath());
                encode.add("utf-8");
            }
        }

        SOURCE_PATH = ArrayUtils.addAll(SOURCE_PATH, listSource.toArray(new String[0]));
        ENCODE_SOURCE = ArrayUtils.addAll(ENCODE_SOURCE, encode.toArray(new String[0]));
    }

    public static void loadJarPath(String path) throws FileNotFoundException {
        File parentFolder = new File(path);
        if (!parentFolder.exists()) throw new FileNotFoundException("Folder doesn't exists! " + path);

        List<File> fileList = DirProcessor.getAllEntity(new File(path), false);
        List<String> jarFiles = new ArrayList<>();

        for (File file : fileList) {
            if (com.google.common.io.Files.getFileExtension(file.getName()).equals("jar")) {
                jarFiles.add(file.getAbsolutePath());
            }
        }

        CLASS_PATH = ArrayUtils.addAll(CLASS_PATH, jarFiles.toArray(new String[0]));
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

        //auto load binding
        if (config.getSourceFolder() != null) {
            if (config.getPrefixSourceFolders() != null) {
                for (String prefix : config.getPrefixSourceFolders()) {
                    Config.loadSrcPath(config.getSourceFolder(), prefix);
                }
            }

            if (config.getPackageSourceFolders() != null) {
                for (String packageName : config.getPackageSourceFolders()) {
                    Config.loadSrcPathFromPackage(config.getSourceFolder(), packageName);
                }
            }
        }

        if (config.getJarFolders() != null) {
            for (String jarFolder : config.getJarFolders()) {
                Config.loadJarPath(jarFolder);
            }
        }

        JDT_LEVEL = config.getJdtLevel();
        JAVA_VERSION = config.getJavaVersion();
        IGNORE_FILES = config.getIgnoreFiles().toArray(new String[0]);
        TEST_FILE_PATH = config.getTestFilePath();
        TEST_POSITION = config.getTestPosition();
    }

}