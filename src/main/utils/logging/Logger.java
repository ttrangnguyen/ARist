package utils.logging;

import config.Config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {
    public static void write(String line) {
        write(line, "log.txt");
    }

    public static void error(String line) {
        System.err.println("ERROR: "+ line);
        write(line, "error.txt");
    }
    public static void warning(String line) {
        System.err.println("WARNING: "+ line);
        write(line, "warning.txt");
    }

    public static void write(String line, String filename) {
        File output = new File(Config.LOG_DIR + filename);
        try {
            FileWriter fileWriter = new FileWriter(output, true);
            fileWriter.append(line + "\n");
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
