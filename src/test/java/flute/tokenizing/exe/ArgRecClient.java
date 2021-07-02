package flute.tokenizing.exe;

import flute.analysis.ExpressionType;
import flute.analysis.structure.DataFrame;
import flute.communicate.SocketClient;
import flute.communicate.schema.Response;
import flute.config.Config;
import flute.tokenizing.excode_data.ArgRecTest;
import flute.tokenizing.excode_data.MultipleArgRecTest;
import flute.tokenizing.excode_data.RecTest;
import flute.utils.file_writing.CSVWritor;
import flute.utils.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
    public List<? extends RecTest> generateTestsFromFile(String filePath) throws IOException {
        List<ArgRecTest> tests = (List<ArgRecTest>) super.generateTestsFromFile(filePath);

        MultipleArgRecTestGenerator multipleGenerator = Config.TEST_ARG_ONE_BY_ONE?
                new SingleArgRecTestGenerator((ArgRecTestGenerator) generator): new AllArgRecTestGenerator((ArgRecTestGenerator) generator);

        return multipleGenerator.generate(tests);
    }

    @Override
    public List<? extends RecTest> getTests(boolean fromSavefile, boolean doSaveTestsAfterGen) throws IOException {
        List<ArgRecTest> tests = (List<ArgRecTest>) super.getTests(fromSavefile, doSaveTestsAfterGen);

        MultipleArgRecTestGenerator multipleGenerator = Config.TEST_ARG_ONE_BY_ONE?
                new SingleArgRecTestGenerator((ArgRecTestGenerator) generator): new AllArgRecTestGenerator((ArgRecTestGenerator) generator);

        return multipleGenerator.generate(tests);
    }

    @Override
    public List<? extends RecTest> getTestsAndReport(boolean fromSavefile, boolean doSaveTestsAfterGen) throws IOException {
        List<MultipleArgRecTest> tests = (List<MultipleArgRecTest>) super.getTestsAndReport(fromSavefile, doSaveTestsAfterGen);

        for (MultipleArgRecTest test: tests)
            if (!test.isIgnored()) {
                dataFrame.insert("Generated excode count", test.getNext_excodeList().size());
                dataFrame.insert("Generated lexical count", test.getNext_lexList().size());
            }
        System.out.println("Number of generated excode candidates: " +
                dataFrame.getVariable("Generated excode count").getSum());

        System.out.println("Number of generated lexical candidates: " +
                dataFrame.getVariable("Generated lexical count").getSum());

        return tests;
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
    boolean doSkipTest(RecTest test) {
        if (((MultipleArgRecTest)test).getNumArg() == 0 && !Config.TEST_ZERO_ARG) return true;
        return false;
    }

    @Override
    void test(Response response, RecTest test, boolean verbose, boolean doPrintIncorrectPrediction) {
        super.test(response, test, verbose, doPrintIncorrectPrediction);
        dataFrame.insert("NumArg", ((MultipleArgRecTest)test).getNumArg());
        ExpressionType argType = ((MultipleArgRecTest)test).getArgRecTestList().get(0).getArgType();
        if (argType != null) dataFrame.insert("ArgType", argType.ordinal());
    }

    @Override
    void updateTopKResult(RecTest test, List<String> results, int k, boolean adequateGeneratedCandidate,
                          String modelName, boolean doPrintIncorrectPrediction) {

        MultipleArgRecTest multipleArgRecTest = (MultipleArgRecTest) test;

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

            if (adequateGeneratedCandidate) {
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
            }

            if (adequateGeneratedCandidate) {
                dataFrame.insert(String.format("%sTop%d", modelName, k), 0);
                dataFrame.insert(String.format("%sTop%dArg%d", modelName, k, multipleArgRecTest.getNumArg()), 0);
                dataFrame.insert(String.format("%sTop%d%s", modelName, k, multipleArgRecTest.getArgRecTestList().get(0)
                        .getArgType()), 0);

                if (doPrintIncorrectPrediction) {
                    Logger.write(gson.toJson(test), projectName + "_incorrect_" + getTestClass().getSimpleName() + "s_top_" + k + ".txt");
                }
            }
        }
    }

    @Override
    public void printTestResult() {
        super.printTestResult();

        List<String[]> accuracyPerNumArg = new ArrayList<>();
        List<String> row = new ArrayList<>();
        row.add("Number of params");
        row.add("Percentage distribution");
        if (this.isNGramUsed) {
            for (int k: this.tops) row.add(String.format("NGram's top-%d accuracy", k));
        }
        if (this.isRNNUsed) {
            for (int k: this.tops) row.add(String.format("RNN's top-%d accuracy", k));
        }
        if (this.isGPTUsed) {
            for (int k: this.tops) row.add(String.format("GPT's top-%d accuracy", k));
        }
        for (int k: this.tops) row.add(String.format("Top-%d precision", k));
        for (int k: this.tops) row.add(String.format("Top-%d recall", k));
        accuracyPerNumArg.add(row.toArray(new String[row.size()]));

        String bestModel = getBestModel(new ArrayList<>(Arrays.asList("nGram", "RNN", "GPT")), "%sOverallTop%d");

        if (!Config.TEST_ARG_ONE_BY_ONE) {
            DataFrame.Variable numArgVar = dataFrame.getVariable("NumArg");
            for (int i = (int)numArgVar.getMin(); i <= numArgVar.getMax(); ++i) {
                row = new ArrayList<>();
                row.add(String.format("%d", i));
                row.add(String.format("%f", dataFrame.getVariable("NumArg").getProportionOfValue(i, true)));
                if (this.isNGramUsed) {
                    for (int k: this.tops) row.add(String.format("%f", dataFrame.getVariable(String.format("nGramTop%dArg%d", k, i)).getMean()));
                }
                if (this.isRNNUsed) {
                    for (int k: this.tops) row.add(String.format("%f", dataFrame.getVariable(String.format("RNNTop%dArg%d", k, i)).getMean()));
                }
                if (this.isGPTUsed) {
                    for (int k: this.tops) row.add(String.format("%f", dataFrame.getVariable(String.format("GPTTop%dArg%d", k, i)).getMean()));
                }
                for (int k: this.tops) row.add(String.format("%f", dataFrame.getVariable(String.format(bestModel + "OverallTop%dArg%d", k, i)).getMean()));
                for (int k: this.tops) row.add(String.format("%f", dataFrame.getVariable(String.format(bestModel + "ActualTop%dArg%d", k, i)).getMean()));
                accuracyPerNumArg.add(row.toArray(new String[row.size()]));
            }
        } else {
            for (ExpressionType argType: ExpressionType.values()) {
                row = new ArrayList<>();
                row.add(String.format("%s", argType.toString()));
                row.add(String.format("%f", dataFrame.getVariable("ArgType").getProportionOfValue(argType.ordinal(), true)));
                if (this.isNGramUsed) {
                    for (int k: this.tops) row.add(String.format("%f", dataFrame.getVariable(String.format("nGramTop%d%s", k, argType)).getMean()));
                }
                if (this.isRNNUsed) {
                    for (int k: this.tops) row.add(String.format("%f", dataFrame.getVariable(String.format("RNNTop%d%s", k, argType)).getMean()));
                }
                if (this.isGPTUsed) {
                    for (int k: this.tops) row.add(String.format("%f", dataFrame.getVariable(String.format("GPTTop%d%s", k, argType)).getMean()));
                }
                for (int k: this.tops) row.add(String.format("%f", dataFrame.getVariable(String.format(bestModel + "OverallTop%d%s", k, argType)).getMean()));
                for (int k: this.tops) row.add(String.format("%f", dataFrame.getVariable(String.format(bestModel + "ActualTop%d%s", k, argType)).getMean()));
                accuracyPerNumArg.add(row.toArray(new String[row.size()]));
            }
        }

        row = new ArrayList<>();
        row.add("all");
        row.add("100");
        if (this.isNGramUsed) {
            for (int k: this.tops) row.add(String.format("%f", dataFrame.getVariable(String.format("nGramTop%d", k)).getMean()));
        }
        if (this.isRNNUsed) {
            for (int k: this.tops) row.add(String.format("%f", dataFrame.getVariable(String.format("RNNTop%d", k)).getMean()));
        }
        if (this.isGPTUsed) {
            for (int k: this.tops) row.add(String.format("%f", dataFrame.getVariable(String.format("GPTTop%d", k)).getMean()));
        }
        for (int k: this.tops) row.add(String.format("%f", dataFrame.getVariable(String.format("%sOverallTop%d", bestModel, k)).getMean()));
        for (int k: this.tops) row.add(String.format("%f", dataFrame.getVariable(String.format("%sActualTop%d", bestModel, k)).getMean()));
        accuracyPerNumArg.add(row.toArray(new String[row.size()]));

        CSVWritor.write(Config.LOG_DIR + this.projectName + "_arg_rec_acc.csv", accuracyPerNumArg);

        for (int k: this.tops) {
            System.out.println("Target showed up in top " + k + " recommendations in "
                    + (int)dataFrame.getVariable(String.format("%sActualTop%d", bestModel, k)).getSum() + " tests.");
        }
    }

    public static void main(String[] args) throws IOException {
        RecClient client = new ArgRecClient("lucene");
        List<MultipleArgRecTest> tests = (List<MultipleArgRecTest>) client.getTestsAndReport(false, true);
        //List<MultipleArgRecTest> tests = (List<MultipleArgRecTest>) client.generateTestsFromFile(Config.REPO_DIR + "sampleproj/src/Main.java");

        client.validateTests(tests, false);
        //RecClient.logTests(tests);
        client.queryAndTest(tests, false, false);
        client.printTestResult();
        System.exit(0);
    }
}
