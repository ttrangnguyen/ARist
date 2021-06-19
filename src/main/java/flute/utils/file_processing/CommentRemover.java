package flute.utils.file_processing;

import java.io.*;
import java.util.Scanner;

public class CommentRemover {
    public static void main(String[] args) throws FileNotFoundException {
//        String content = new Scanner(new File("D:/zzzzz/Toolbar.java")).useDelimiter("\\Z").next();
//        System.out.println(content);
//        String removed = CommentRemover.removeCommentFromFileString(content);
//        System.out.println(removed);
//        System.out.println("----------------------------------------------");
        File file = new File("D:/zzzzz/GuidedActionsStylist.java");
        String removed2 = CommentRemover.removeCommentFromFile(file);
        System.out.println(removed2);
    }

    public static String removeCommentFromFile(File file) {
        String fileString = readLineByLine(file);
        fileString = fileString.replaceAll("/\\*[^*]*(?:\\*(?!/)[^*]*)*\\*/|//.*", "");
        return fileString;
    }

    public static String removeCommentFromFileString(String fileString) {
        String removedCommentfileString = readLineByLine(fileString);
        removedCommentfileString = removedCommentfileString.replaceAll("/\\*[^*]*(?:\\*(?!/)[^*]*)*\\*/|//.*", "");
        return removedCommentfileString;
    }

    private static String readLineByLine(String fileString) {
        StringBuilder removedCommentfileString = new StringBuilder();
        for (String line: fileString.split("\n")) {
            String removedCommentLine = replaceComments(line);
            removedCommentfileString.append(removedCommentLine).append("\n");
        }
        return removedCommentfileString.toString();
    }

    private static String readLineByLine(File file) {
        StringBuilder textFile = new StringBuilder();
        FileInputStream fstream;
        try {
            fstream = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    fstream));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                String removedCommentLine = replaceComments(strLine);
                if (!removedCommentLine.trim().equals(""))
                    textFile.append(removedCommentLine).append("\n");
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return textFile.toString();
    }

    private static String replaceComments(String strLine) {
        if (strLine.contains("//")) {
            if (strLine.contains("\"")) {
                int lastIndex = strLine.lastIndexOf("\"");
                int lastIndexComment = strLine.lastIndexOf("//");
                if (lastIndexComment > lastIndex) { // ( "" // )
                    strLine = strLine.substring(0, lastIndexComment) + " ";
                }
            } else {
                int index = strLine.lastIndexOf("//");
                strLine = strLine.substring(0, index) + " ";
            }
        }
        return strLine;
    }
}
