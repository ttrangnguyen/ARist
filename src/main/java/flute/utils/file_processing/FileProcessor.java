package flute.utils.file_processing;

import flute.antlr4.config.Config;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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

    public static HashSet<String> readLineByLineToSet(String path) {
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

    public static List<String> readLineByLineToList(String path) {
        List<String> lines = new ArrayList<>();
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

    public static void write(String text, String path) throws IOException {
        File fout = new File(path);
        fout.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(fout);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        bw.write(text);
        bw.close();
    }

    public static void writeListLineByLine(List<String> list, String path) throws IOException {
        File fout = new File(path);
        FileOutputStream fos = new FileOutputStream(fout);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        for (String s : list) {
            bw.write(s);
            bw.newLine();
        }
        bw.close();
    }

    public static boolean deleteFile(File file) throws IOException {
        if (file != null) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();

                for (File f: files) {
                    deleteFile(f);
                }
            }
            return Files.deleteIfExists(file.toPath());
        }
        return false;
    }
}
