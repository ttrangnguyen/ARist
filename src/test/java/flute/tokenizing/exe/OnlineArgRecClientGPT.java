package flute.tokenizing.exe;

import flute.config.Config;

import java.io.IOException;

public class OnlineArgRecClientGPT extends ArgRecClient {
    public OnlineArgRecClientGPT(String projectName) {
        super(projectName);
    }

    @Override
    void createNewGenerator() {
        generator = new ArgRecTestGeneratorGPT(Config.PROJECT_DIR, projectParser);
    }

    public static void main(String[] args) throws IOException {
        RecClient client = new OnlineArgRecClientGPT("lucene");
        client.generateTestsAndQuerySimultaneously(true, true);
        client.printTestResult();
    }
}
