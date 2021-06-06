package flute.tokenizing.exe;


import java.io.*;

public class BatchTestGenerator {
    public static void main(String[] args) throws IOException {
        RecClient client = new ArgRecClient("netbeans");
        client.generateTests();
    }
}
