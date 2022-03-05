package flute.util.file_processing;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class FileProcessor {
    public static String read(File f) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content.toString();
    }

    public static HashSet<String> readLineByLineToSet(String path) {
        HashSet<String> lines = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    public static List<String> readLineByLineToList(String path) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    public static String readAndDeleteBlankLines(String path) {
        List<String> lines = readLineByLineToList(path);
        lines = lines.stream().filter(line -> {
            return !line.matches("^\\s*$");
        }).collect(Collectors.toList());
        return String.join("\n", lines);
    }

    public static void write(String text, String path) throws IOException {
        File fout = new File(path);
        fout.getParentFile().mkdirs();
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fout)))) {
            bw.write(text);
        }
    }

    public static void writeListLineByLine(List<String> list, String path) throws IOException {
        File fout = new File(path);
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fout)))) {
            for (String s : list) {
                bw.write(s);
                bw.newLine();
            }
        }
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
