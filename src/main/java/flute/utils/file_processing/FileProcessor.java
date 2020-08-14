package flute.utils.file_processing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

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
}
