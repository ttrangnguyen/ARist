package flute.tokenizing.exe;

import java.io.*;

public class LocalModelTestGenerator {
    public static void main(String[] args) {
        String project = args[0];
        String fold = args[1];
        RecClient client = new ArgRecClient(project);
        try {
            client.generateLocalModelTests(fold);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
