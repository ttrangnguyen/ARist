package flute.tokenizing.exe;

import flute.config.Config;
import flute.tokenizing.excode_data.MultipleArgRecTest;

import java.io.IOException;
import java.util.List;

public class ArgRecClientGPT extends ArgRecClient {
    public ArgRecClientGPT(String projectName) {
        super(projectName);
    }

    @Override
    void createNewGenerator() {
        generator = new ArgRecTestGeneratorGPT(Config.PROJECT_DIR, projectParser);
    }

    public static void main(String[] args) throws IOException {
        RecClient client = new ArgRecClientGPT("lucene");
        List<MultipleArgRecTest> tests = (List<MultipleArgRecTest>) client.getTestsAndReport(false, false);
        //List<MultipleArgRecTest> tests = (List<MultipleArgRecTest>) client.generateTestsFromFile(Config.REPO_DIR + "sampleproj/src/Main.java");

        client.validateTests(tests, false);
        //RecClient.logTests(tests);
        client.queryAndTest(tests, false, false);
        client.printTestResult();
        System.exit(0);
    }
}
