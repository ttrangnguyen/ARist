package flute.tokenizing.exe;

import java.io.*;

public class BatchTestGenerator {
    public static void main(String[] args) throws IOException {
        String project = args[0];
        String fold = args[1];
        String setting = args[2];
        RecClient client = new ArgRecClient(project);
        client.generateTests(fold, setting);
    }
}
