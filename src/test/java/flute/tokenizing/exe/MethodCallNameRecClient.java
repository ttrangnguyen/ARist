package flute.tokenizing.exe;

import flute.config.Config;
import flute.tokenizing.excode_data.MethodCallNameRecTest;

import java.io.IOException;
import java.util.List;

public class MethodCallNameRecClient extends MethodCallRecClient {
    public MethodCallNameRecClient(String projectName) {
        super(projectName);
        setTestClass(MethodCallNameRecTest.class);
    }

    @Override
    void createNewGenerator() {
        generator = new MethodCallNameRecTestGenerator(Config.PROJECT_DIR, projectParser);
    }

    public static void main(String[] args) throws IOException {
        RecClient client = new MethodCallNameRecClient("lucene");
        List<MethodCallNameRecTest> tests = (List<MethodCallNameRecTest>) client.getTestsAndReport(false, true);
        //List<MethodCallNameRecTest> tests = (List<MethodCallNameRecTest>) client.generateTestsFromFile(Config.REPO_DIR + "sampleproj/src/Main.java");
    }
}
