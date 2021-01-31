package flute.tokenizing.exe;

import flute.communicate.SocketClient;
import flute.config.Config;
import flute.tokenizing.excode_data.MethodCallNameRecTest;
import flute.tokenizing.excode_data.RecTest;
import flute.utils.file_writing.CSVWritor;

import java.io.IOException;
import java.util.ArrayList;
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

    @Override
    public List<? extends RecTest> getTestsAndReport(boolean fromSavefile, boolean doSaveTestsAfterGen) throws IOException {
        List<MethodCallNameRecTest> tests = (List<MethodCallNameRecTest>) super.getTestsAndReport(fromSavefile, doSaveTestsAfterGen);

        for (MethodCallNameRecTest test: tests)
            if (!test.isIgnored()) {
                dataFrame.insert("Generated excode count", test.getMethod_candidate_excode().size());
                dataFrame.insert("Generated lexical count", test.getMethod_candidate_lex().size());
            }
        System.out.println("Number of generated excode candidates: " +
                dataFrame.getVariable("Generated excode count").getSum());

        System.out.println("Number of generated lexical candidates: " +
                dataFrame.getVariable("Generated lexical count").getSum());

        return tests;
    }

    @Override
    SocketClient getSocketClient() throws Exception {
        return new SocketClient(Config.METHOD_NAME_SERVICE_PORT);
    }

    @Override
    void updateTopKResult(RecTest test, List<String> results, int k, boolean adequateGeneratedCandidate, String modelName) {
        if (test.isIgnored()) {
            dataFrame.insert(String.format("%sActualTop%d", modelName, k), 0);
            return;
        }

        boolean isOverallCorrectTopK = false;
        for (int i = 0; i < Math.min(k, results.size()); ++i) {
            if (RecTester.canAcceptResult((MethodCallNameRecTest) test, results.get(i))) {
                isOverallCorrectTopK = true;
                break;
            }
        }

        if (isOverallCorrectTopK) {
            dataFrame.insert(String.format("%sOverallTop%d", modelName, k), 1);

            dataFrame.insert(String.format("%sActualTop%d", modelName, k), 1);
            if (adequateGeneratedCandidate) {
                dataFrame.insert(String.format("%sTop%d", modelName, k), 1);
            }
        } else {
            dataFrame.insert(String.format("%sOverallTop%d", modelName, k), 0);

            dataFrame.insert(String.format("%sActualTop%d", modelName, k), 0);
            if (adequateGeneratedCandidate) {
                dataFrame.insert(String.format("%sTop%d", modelName, k), 0);
            }
        }
    }

    @Override
    public void printTestResult() {
        super.printTestResult();

        List<String[]> accuracy = new ArrayList<>();
        List<String> row = new ArrayList<>();
        if (this.isNGramUsed) {
            for (int k: this.tops) row.add(String.format("NGram's top-%d accuracy", k));
        }
        if (this.isRNNUsed) {
            for (int k: this.tops) row.add(String.format("RNN's top-%d accuracy", k));
        }
        for (int k: this.tops) row.add(String.format("Top-%d precision", k));
        for (int k: this.tops) row.add(String.format("Top-%d recall", k));
        accuracy.add(row.toArray(new String[row.size()]));

        row = new ArrayList<>();
        if (this.isNGramUsed) {
            for (int k: this.tops) row.add(String.format("%f", dataFrame.getVariable(String.format("nGramTop%d", k)).getMean()));
        }
        if (this.isRNNUsed) {
            for (int k: this.tops) row.add(String.format("%f", dataFrame.getVariable(String.format("RNNTop%d", k)).getMean()));
        }
        for (int k: this.tops) row.add(String.format("%f", dataFrame.getVariable(String.format("nGramOverallTop%d", k)).getMean()));
        for (int k: this.tops) row.add(String.format("%f", dataFrame.getVariable(String.format("nGramActualTop%d", k)).getMean()));
        accuracy.add(row.toArray(new String[row.size()]));

        CSVWritor.write(Config.LOG_DIR + this.projectName + "_method_call_name_rec_acc.csv", accuracy);
    }

    public static void main(String[] args) throws IOException {
        RecClient client = new MethodCallNameRecClient("lucene");
        List<MethodCallNameRecTest> tests = (List<MethodCallNameRecTest>) client.getTestsAndReport(false, true);
        //List<MethodCallNameRecTest> tests = (List<MethodCallNameRecTest>) client.generateTestsFromFile(Config.REPO_DIR + "sampleproj/src/Main.java");

        client.validateTests(tests, false);
        //RecClient.logTests(tests);
        client.queryAndTest(tests, false);
        client.printTestResult();
    }
}
