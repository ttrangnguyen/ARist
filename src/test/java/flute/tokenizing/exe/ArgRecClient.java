package flute.tokenizing.exe;

import flute.config.Config;
import flute.tokenizing.excode_data.ArgRecTest;
import flute.tokenizing.excode_data.MultipleArgRecTest;
import flute.tokenizing.excode_data.RecTest;

import java.io.IOException;
import java.util.List;

public class ArgRecClient extends MethodCallRecClient {
    public ArgRecClient(String projectName) {
        super(projectName);
        setTestClass(ArgRecTest.class);
    }

    @Override
    void createNewGenerator() {
        generator = new ArgRecTestGenerator(Config.PROJECT_DIR, projectParser);
    }

    @Override
    public List<? extends RecTest> getTests(boolean fromSavefile, boolean doSaveTestsAfterGen) throws IOException {
        List<ArgRecTest> tests = (List<ArgRecTest>) super.getTests(fromSavefile, doSaveTestsAfterGen);

        MultipleArgRecTestGenerator multipleGenerator = Config.TEST_ARG_ONE_BY_ONE?
                new SingleArgRecTestGenerator((ArgRecTestGenerator) generator): new AllArgRecTestGenerator((ArgRecTestGenerator) generator);

        return multipleGenerator.generate(tests);
    }

    public static void main(String[] args) throws IOException {
        RecClient client = new ArgRecClient("lucene");
        List<MultipleArgRecTest> tests = (List<MultipleArgRecTest>) client.getTestsAndReport(false, true);
        //List<MultipleArgRecTest> tests = (List<MultipleArgRecTest>) client.generateTestsFromFile(Config.REPO_DIR + "sampleproj/src/Main.java");
    }
}
