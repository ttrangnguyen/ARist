package flute.utils.file_processing;

import org.checkerframework.checker.units.qual.A;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

public class FileProcessor {
    public static String read(File f) {
        String content = "";
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                content += (line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    public static HashSet<String> readLineByLine(String path) {
        HashSet<String> lines = new HashSet<>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(path));
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }
}
