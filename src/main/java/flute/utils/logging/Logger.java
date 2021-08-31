package flute.utils.logging;

import flute.config.Config;
import flute.jdtparser.ProjectParser;
import flute.tokenizing.excode_data.ArgRecTest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {
    static FileWriter fw;

    public static void write(String line) {
        write(line, "log.txt");
    }

    public static void error(String line) {
        System.err.println("ERROR: " + line);
        write(line, "error.txt");
    }

    public static void warning(String line) {
        System.err.println("WARNING: " + line);
        write(line, "warning.txt");
    }

    public static void write(String line, String filename) {
        File output = new File(Config.LOG_DIR + filename);
        output.getParentFile().mkdirs();
        try {
            FileWriter fileWriter = new FileWriter(output, true);
            fileWriter.append(line + "\n");
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void testCount(ArgRecTest test, ProjectParser projectParser) {
        int publicStaticCount = projectParser.getFasterPublicStaticCandidates(
                test.getParamTypeKey()
        ).size();
        write(String.valueOf(test.getNext_lexList().size() + publicStaticCount), "test_count.txt");
    }

    public static void delete(String filename) {
        File file = new File(Config.LOG_DIR + filename);
        file.delete();
    }

    public static void initDebug(String debugName) {
        try {
            fw = new FileWriter(Config.LOG_DIR + debugName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void logDebug(Object obj) {
        try {
            fw.append(obj + "\r\n");
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void closeDebug() {
        try {
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Logs the given object to console.
     */
    public static void log(Object obj) {
        System.out.println(obj);
    }
}
