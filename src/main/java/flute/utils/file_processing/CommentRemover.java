package flute.utils.file_processing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class CommentRemover {
    public static void main(String[] args) {
        String filePath = "D:/zzzz/CommentRemover.java";
        String removed = CommentRemover.removeComment(filePath);
    }

    public static String removeComment(String filePath) {
        File file = new File(filePath);
        String fileString = readLineByLine(file);
        fileString = fileString.replaceAll(
                "(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)", "");
        System.out.println(fileString);
        return fileString;
    }

    private static String readLineByLine(File file) {
        String textFile = "";
        FileInputStream fstream;
        try {
            fstream = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    fstream));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                textFile = textFile + replaceComments(strLine) + "\n";
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return textFile;
    }

    private static String replaceComments(String strLine) {
        if (strLine.startsWith("//")) {
            return "";
        } else if (strLine.contains("//")) {
            if (strLine.contains("\"")) {
                int lastIndex = strLine.lastIndexOf("\"");
                int lastIndexComment = strLine.lastIndexOf("//");
                if (lastIndexComment > lastIndex) { // ( "" // )
                    strLine = strLine.substring(0, lastIndexComment);
                }
            } else {
                int index = strLine.lastIndexOf("//");
                strLine = strLine.substring(0, index);
            }
        }
        return strLine;
    }
}
