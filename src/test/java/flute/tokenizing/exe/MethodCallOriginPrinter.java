package flute.tokenizing.exe;

import flute.communicate.SocketClient;
import flute.config.Config;
import flute.jdtparser.ProjectParser;
import flute.tokenizing.excode_data.ArgRecTest;
import flute.tokenizing.excode_data.MultipleArgRecTest;
import flute.tokenizing.excode_data.RecTest;
import flute.utils.file_processing.FileProcessor;
import flute.utils.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MethodCallOriginPrinter extends MethodCallRecClient {
    public MethodCallOriginPrinter(String projectName) {
        super(projectName);
        setTestClass(ArgRecTest.class);
    }

    public MethodCallOriginPrinter(String projectName, String projectDir) {
        super(projectName, projectDir);
        setTestClass(ArgRecTest.class);
    }

    @Override
    void setupProjectParser() throws IOException {
        if (projectParser != null) return;
        super.setupProjectParser();
        projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
                Config.ENCODE_SOURCE, new String[]{}, Config.JDT_LEVEL, Config.JAVA_VERSION);
        projectParser.loadPublicStaticMembers();
        projectParser.loadPublicStaticRTMembers();
        projectParser.loadObjectMapping();
        projectParser.loadTypeTree();
    }

    @Override
    void createNewGenerator() {
        generator = new MethodCallOriginEnumerator(Config.PROJECT_DIR, projectParser);
    }

    @Override
    public List<? extends RecTest> getTests(boolean fromSavefile, boolean doSaveTestsAfterGen) throws IOException {
        List<ArgRecTest> tests = (List<ArgRecTest>) super.getTests(fromSavefile, doSaveTestsAfterGen);

        MultipleArgRecTestGenerator multipleGenerator = Config.TEST_ARG_ONE_BY_ONE?
                new SingleArgRecTestGenerator(new ArgRecTestGenerator(Config.PROJECT_DIR, projectParser)): new AllArgRecTestGenerator(new ArgRecTestGenerator(Config.PROJECT_DIR, projectParser));

        return multipleGenerator.generate(tests);
    }

    void saveTests(List<RecTest> tests) {
        for (RecTest test : tests) {
            Logger.write(gson.toJson(this.getTestClass().cast(test)), projectName + "_" + this.getTestClass().getSimpleName() + "Appendice" + "s.txt");
        }
    }

    @Override
    SocketClient getSocketClient() throws Exception {
        return new SocketClient(getSocketPort());
    }

    @Override
    int getSocketPort() {
        return Config.PARAM_SERVICE_PORT;
    }

    @Override
    void updateTopKResult(RecTest test, List<String> results, int k, boolean adequateGeneratedCandidate, String modelName, boolean doPrintIncorrectPrediction) {
    }

    public static void main(String[] args) throws IOException {
        List<String> projectList = FileProcessor.readLineByLineToList("../../Tannm/storage/" + args[0]);
        for (String project: projectList) {
            RecClient client = new MethodCallOriginPrinter(project, "../../Kien/Flute-Kien-full/storage/repositories/git/four_hundred/" + project);
            List<String> filePaths = FileProcessor.readLineByLineToList("../../Tannm/Flute/storage/testFilePath/" + project + "_ArgRecTests_file_path.txt");
            List<File> fileList = filePaths.stream().map(filePath -> new File(filePath)).collect(Collectors.toList());
            if (fileList.isEmpty()) continue;
            client.getTests(fileList, true);
        }
    }
}
