package flute.utils.file_processing;
import flute.config.Config;
import flute.preprocessing.EmptyStringLiteralDecorator;
import flute.preprocessing.FileFilter;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LOCCounter {
    public static int count(File file) {
        int cnt = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String strLine;
            boolean inCommentBlock = false;
            while ((strLine = br.readLine()) != null) {
                strLine = strLine.trim();
                if (strLine.endsWith("*/")) {
                    inCommentBlock = false;
                } else if (inCommentBlock || strLine.startsWith("import") ||
                        strLine.startsWith("package") || strLine.equals("") ||
                        strLine.startsWith("//")) {
                } else if (strLine.startsWith("/**")) {
                    inCommentBlock = true;
                } else {
                    ++cnt;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cnt;
    }

    public static int countJava(File file) {
        String fileContent = CommentRemover.removeCommentFromFileAfterParsing(file);
        fileContent = EmptyStringLiteralDecorator.preprocess(fileContent);
        String[] lines = fileContent.split("\n");
        List<String> linesOfCode = Arrays.stream(lines).filter(line -> {
            return !line.matches("^\\s*$");
        }).collect(Collectors.toList());

        return linesOfCode.size();
    }

    public static int countJavaProject(String projectDir) {
        List<File> javaFiles = DirProcessor.walkJavaFile(projectDir);
        javaFiles = FileFilter.filter(javaFiles);

        int totalLOC = 0;
        for (File file: javaFiles) {
            totalLOC += countJava(file);
        }
        return totalLOC;
    }

    public static void main(String args[]) throws IOException {
        StringBuilder sb = new StringBuilder();
        File repoDir = new File("../../CodeCompletion/dataset/CugLM/java_repos/");
        //File repoDir = new File(Config.REPO_DIR + "oneproj/");
        for (File project: repoDir.listFiles()) {
            sb.append(project.getName());
            sb.append("\n");
            sb.append(countJavaProject(project.getAbsolutePath()));
            sb.append("\n");
        }
        FileProcessor.write(sb.toString(), Config.LOG_DIR + "log_loc.txt");
    }
}