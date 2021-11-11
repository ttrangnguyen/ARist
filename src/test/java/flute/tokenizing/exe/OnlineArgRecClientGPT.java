package flute.tokenizing.exe;

import flute.analysis.ExpressionType;
import flute.config.Config;
import flute.testing.PredictionDetail;
import flute.tokenizing.excode_data.MultipleArgRecTest;
import flute.tokenizing.excode_data.RecTest;
import flute.utils.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OnlineArgRecClientGPT extends ArgRecClient {
    public OnlineArgRecClientGPT(String projectName) {
        super(projectName);
    }

    @Override
    void createNewGenerator() {
        generator = new ArgRecTestGeneratorGPT(Config.PROJECT_DIR, projectParser);
    }

    @Override
    void updateTopKResult(RecTest test, List<String> results, int k, boolean adequateGeneratedCandidate,
                          String modelName, boolean doPrintIncorrectPrediction) {

        MultipleArgRecTest multipleArgRecTest = (MultipleArgRecTest) test;
        PredictionDetail predictionDetail = new PredictionDetail();
        predictionDetail.predictions_lex = results;

        boolean isOverallCorrectTopK = false;
        for (int i = 0; i < Math.min(k, results.size()); ++i) {
            if (RecTester.canAcceptResult(multipleArgRecTest, results.get(i))) {
                isOverallCorrectTopK = true;
                break;
            }
        }

        if (isOverallCorrectTopK) {
            dataFrame.insert(String.format("%sActualTop%d", modelName, k), 1);
            dataFrame.insert(String.format("%sActualTop%dArg%d", modelName, k, multipleArgRecTest.getNumArg()), 1);
            dataFrame.insert(String.format("%sActualTop%d%s", modelName, k, multipleArgRecTest.getArgRecTestList().get(0)
                    .getArgType()), 1);

            if (!test.isIgnored()) {
                dataFrame.insert(String.format("%sOverallTop%d", modelName, k), 1);
                dataFrame.insert(String.format("%sOverallTop%dArg%d", modelName, k, multipleArgRecTest.getNumArg()), 1);
                dataFrame.insert(String.format("%sOverallTop%d%s", modelName, k, multipleArgRecTest.getArgRecTestList().get(0)
                        .getArgType()), 1);
            }

            if (adequateGeneratedCandidate ||
                    multipleArgRecTest.getArgRecTestList().get(0).getArgType() == ExpressionType.TYPE_LIT ||
                    multipleArgRecTest.getArgRecTestList().get(0).getArgType() == ExpressionType.LAMBDA) {
                dataFrame.insert(String.format("%sTop%d", modelName, k), 1);
                dataFrame.insert(String.format("%sTop%dArg%d", modelName, k, multipleArgRecTest.getNumArg()), 1);
                dataFrame.insert(String.format("%sTop%d%s", modelName, k, multipleArgRecTest.getArgRecTestList().get(0)
                        .getArgType()), 1);
            }
        } else {
            dataFrame.insert(String.format("%sActualTop%d", modelName, k), 0);
            dataFrame.insert(String.format("%sActualTop%dArg%d", modelName, k, multipleArgRecTest.getNumArg()), 0);
            dataFrame.insert(String.format("%sActualTop%d%s", modelName, k, multipleArgRecTest.getArgRecTestList().get(0)
                    .getArgType()), 0);

            if (!test.isIgnored()) {
                dataFrame.insert(String.format("%sOverallTop%d", modelName, k), 0);
                dataFrame.insert(String.format("%sOverallTop%dArg%d", modelName, k, multipleArgRecTest.getNumArg()), 0);
                dataFrame.insert(String.format("%sOverallTop%d%s", modelName, k, multipleArgRecTest.getArgRecTestList().get(0)
                        .getArgType()), 0);

                if (doPrintIncorrectPrediction) {
                    String outputFileName = projectName + "_incorrect_" + getTestClass().getSimpleName() + "s_top_" + k + ".txt";
                    Logger.write(gson.toJson(test), outputFileName);
                    Logger.write(gson.toJson(predictionDetail), outputFileName);
                }
            }

            if (adequateGeneratedCandidate ||
                    multipleArgRecTest.getArgRecTestList().get(0).getArgType() == ExpressionType.TYPE_LIT ||
                    multipleArgRecTest.getArgRecTestList().get(0).getArgType() == ExpressionType.LAMBDA) {
                dataFrame.insert(String.format("%sTop%d", modelName, k), 0);
                dataFrame.insert(String.format("%sTop%dArg%d", modelName, k, multipleArgRecTest.getNumArg()), 0);
                dataFrame.insert(String.format("%sTop%d%s", modelName, k, multipleArgRecTest.getArgRecTestList().get(0)
                        .getArgType()), 0);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        RecClient client = new OnlineArgRecClientGPT("lucene");
        client.generateTestsAndQuerySimultaneously(true, true);
        client.printTestResult();

//        List<MultipleArgRecTest> tests = (List<MultipleArgRecTest>) client.generateTestsFromFile("netbeans\\ide\\bugtracking\\src\\org\\netbeans\\modules\\bugtracking\\ui\\selectors\\RepositorySelectorBuilder.java");
//        List<MultipleArgRecTest> debugTests = new ArrayList<>();
//        for (MultipleArgRecTest test: tests)
//            if (test.getExpected_lex().compareTo("RepositorySelectorBuilder.this.getSelectedRepository()") == 0) {
//                debugTests.add(test);
//            }
//        client.queryAndTest(debugTests, true, false);

//        List<MultipleArgRecTest> tests = (List<MultipleArgRecTest>) client.getTestsAndReport(false, true);
//        client.validateTests(tests, false);
//        client.queryAndTest(tests, true, true);
//        client.printTestResult();
    }
}
