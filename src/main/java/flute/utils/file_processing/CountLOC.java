package flute.utils.file_processing;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;


public class CountLOC {
    public static void main(String args[]) {
        System.out.println(count(new File("D:/zzzz/CommentRemover.java")));
    }

    public static int count(File file) {
        int cnt = 0;
        FileInputStream fstream;
        try {
            fstream = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    fstream));
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
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cnt;
    }
}