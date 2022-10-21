package flute.evaluating;

import com.google.gson.Gson;
import com.opencsv.CSVWriter;
import flute.communicating.PredictionDetail;
import flute.communicating.SingleParamRequest;
import flute.config.Config;
import flute.config.ModelConfig;
import flute.config.ProjectConfig;
import flute.config.TestConfig;
import flute.testing.TestFilesManager;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SequenceEvaluatorCuglm {
    private int[] supportedTest = new int[11];
    private int[] nonSupportedTest = new int[11];
    private int[][] supportedTestCorrect = new int[11][10];
    private int[][] nonSupportedTestCorrect = new int[11][10];
    private double[] totalRuntime = new double[11];
    private String[] argTypes = {"NAME", "METHOD_INVOC", "FIELD_ACCESS", "ARRAY_ACCESS", "CAST", "STRING_LIT",
            "NUM_LIT", "CHAR_LIT", "TYPE_LIT", "BOOL_LIT", "NULL_LIT", "OBJ_CREATION", "ARR_CREATION",
            "THIS", "SUPER", "COMPOUND", "LAMBDA", "METHOD_REF", "OTHERS"};
    private HashMap<String, Integer> typeMap = new HashMap<>();

    private int nArgTypes = argTypes.length;
    private int[][] supportedTestByType = new int[11][nArgTypes];
    private int[][][] supportedTestCorrectByType = new int[11][nArgTypes][10];
    private int[][] nonSupportedTestByType = new int[11][nArgTypes];
    private int[][][] nonSupportedTestCorrectByType = new int[11][nArgTypes][10];
    private double ALL_RR = 0;
    private double PROJECT_RR = 0;

    public SequenceEvaluatorCuglm() {
        for (int i = 0; i < argTypes.length; ++i) {
            typeMap.put(argTypes[i], i);
        }
    }

    public void evaluate(String solution) {
        File[] testProjectPaths = new File(ProjectConfig.cugLMTestProjectsPath).listFiles(File::isDirectory);
        assert testProjectPaths != null;
        for (File testProjectPath : testProjectPaths) {
            ProjectConfig.project = testProjectPath.getName();
            Config.init();
            evaluate(testProjectPath.getName(), solution);
            PROJECT_RR = 0;
            supportedTest[10] += supportedTest[0];supportedTest[0] = 0;
            nonSupportedTest[10] += nonSupportedTest[0];nonSupportedTest[0] = 0;
            totalRuntime[10] += totalRuntime[0];totalRuntime[0] = 0;
            for (int topK = 0; topK < 10; ++topK) {
                supportedTestCorrect[10][topK] += supportedTestCorrect[0][topK];
                supportedTestCorrect[0][topK] = 0;
                nonSupportedTestCorrect[10][topK] += nonSupportedTestCorrect[0][topK];
                nonSupportedTestCorrect[0][topK] = 0;
            }
            for (int argTypeId = 0; argTypeId < nArgTypes; ++argTypeId) {
                supportedTestByType[10][argTypeId] += supportedTestByType[0][argTypeId];
                supportedTestByType[0][argTypeId] = 0;
                nonSupportedTestByType[10][argTypeId] += nonSupportedTestByType[0][argTypeId];
                nonSupportedTestByType[0][argTypeId] = 0;
                for (int topK = 0; topK < 10; ++topK) {
                    supportedTestCorrectByType[10][argTypeId][topK] += supportedTestCorrectByType[0][argTypeId][topK];
                    supportedTestCorrectByType[0][argTypeId][topK] = 0;
                    nonSupportedTestCorrectByType[10][argTypeId][topK] += nonSupportedTestCorrectByType[0][argTypeId][topK];
                    nonSupportedTestCorrectByType[0][argTypeId][topK] = 0;
                }
            }
        }
        writeResult("<ALL>", solution);
    }

    public void evaluate(String project, String solution) {
        System.out.println(project + " " + solution);
        int fold = 0;
        Gson gson = new Gson();
        if (ModelConfig.USE_MAINTENANCE) {
            TestFilesManager.init();
        }
        try {
            String predictionDetailPath = TestConfig.predictionDetailPath;
            String testCasesFilePath = "/home/hieuvd/Tannm/Flute/storage/logs/" + project + "_ArgRecTests.txt";

            BufferedReader brDetail = new BufferedReader(new FileReader(predictionDetailPath));
            BufferedReader brTest = new BufferedReader(new FileReader(testCasesFilePath));
            String resultString;
            while ((resultString = brDetail.readLine()) != null) {
                PredictionDetail result = gson.fromJson(resultString, PredictionDetail.class);
                String request;
                SingleParamRequest jsonRequest;
                String filePath;
                do {
                    // check if file is testfile only in maintenance setting
                    request = brTest.readLine();
                    jsonRequest = gson.fromJson(request, SingleParamRequest.class);
                    filePath = Paths.get(ProjectConfig.cugLMAllProjectsPath, ProjectConfig.project, jsonRequest.filePath).toString();
                } while (ModelConfig.USE_MAINTENANCE && !TestFilesManager.isTestFile(new File(filePath)));
                if (jsonRequest.expected_lex.equals(")")) continue;
                String testArgType = jsonRequest.argType;
                boolean isSupported = !jsonRequest.ignored || testArgType.equals("COMPOUND") || testArgType.equals("LAMBDA");
                int argTypeIndex = typeMap.get(testArgType);
                if (isSupported) {
                    supportedTest[0] += 1;
                    supportedTestByType[0][argTypeIndex] += 1;
                } else {
                    nonSupportedTest[fold] += 1;
                    nonSupportedTestByType[fold][argTypeIndex] += 1;
                }
                String normalizedAnswer = TestQuery.getNormalizedAnswer(request);
                int i = 0, j = 0;
//                HashSet<String> commonTokens = new HashSet<>();
//                commonTokens.add("0");
//                commonTokens.add(".class");
//                commonTokens.add("\"\"");
//                commonTokens.add("null");
                while (true) {
                    if (j == result.predictions.size() || i == 10) {
                        break;
                    }
                    if (result.predictions.get(j).equals("<COMPOUND>")) {
                        ++j;
                        continue;
                    }
//                    if (commonTokens.contains(result.predictions.get(j))) {
//                        ++j;
//                        continue;
//                    }
                    if (TestQuery.softEqualSequence(result.predictions.get(j), normalizedAnswer, testArgType)) {
                        if (isSupported) {
                            supportedTestCorrect[fold][i] += 1;
                            supportedTestCorrectByType[fold][argTypeIndex][i] += 1;
                        } else {
                            nonSupportedTestCorrect[fold][i] += 1;
                            nonSupportedTestCorrectByType[fold][argTypeIndex][i] += 1;
                        }
                        break;
                    }
                    ++i;
                    ++j;
                }
                totalRuntime[fold] += result.runtime;
            }
//            if (solution.equals("flute")) {
            if (!ModelConfig.USE_BEAM_SEARCH) {
                for (int i = 0; i < 10; ++i) {
                    for (int t = 0; t < argTypes.length; ++t) {
                        nonSupportedTestCorrectByType[fold][t][i] = 0;
                    }
                    nonSupportedTestCorrect[fold][i] = 0;
                }
            }
            for (int i = 0; i < 10; ++i) {
                ALL_RR += 1.0 * (supportedTestCorrect[fold][i] + nonSupportedTestCorrect[fold][i]) / (i+1);
                PROJECT_RR += 1.0 * (supportedTestCorrect[fold][i] + nonSupportedTestCorrect[fold][i]) / (i+1);
            }
            for (int i = 1; i < 10; ++i) {
                supportedTestCorrect[fold][i] += supportedTestCorrect[fold][i - 1];
                nonSupportedTestCorrect[fold][i] += nonSupportedTestCorrect[fold][i - 1];
            }
            for (int t = 0; t < argTypes.length; ++t) {
                for (int i = 1; i < 10; ++i) {
                    supportedTestCorrectByType[fold][t][i] += supportedTestCorrectByType[fold][t][i - 1];
                    nonSupportedTestCorrectByType[fold][t][i] += nonSupportedTestCorrectByType[fold][t][i - 1];
                }
            }

            brDetail.close();
            brTest.close();
            writeResult(project, solution);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeResult(String project, String solution)  {
        try {
            String predictionResultPath = TestConfig.predictionResultPath;
            int fold = 0;
            if (project.equals("<ALL>")) {
                predictionResultPath = "storage/result-cuglm" +
                        (ModelConfig.USE_MAINTENANCE?"-maintenance": (ModelConfig.USE_DYNAMIC?"-dynamic":"-static"))
                        + (ModelConfig.USE_BEAM_SEARCH?"-beamsearch":"")
                        + "/ALL_prediction_result_flute_" +
                        (TestConfig.predictType== TestConfig.PredictType.SEQUENCE ?"sequence":"firsttoken") + ".csv";
                fold = 10;
                PROJECT_RR = ALL_RR;
            }
            List<String[]> result = new ArrayList<>();
            File fout = new File(predictionResultPath);
            FileWriter outputFile = new FileWriter(fout);
            CSVWriter writer = new CSVWriter(outputFile);
            String[] columns = {"Param type", "Distribution", "Number of tests", "Number of supported tests",
                    "Top-1 precision", "Top-3 precision", "Top-5 precision", "Top-10 precision",
                    "Top-1 recall", "Top-3 recall", "Top-5 recall", "Top-10 recall"};
            result.add(columns);
            for (int t = 0; t < argTypes.length; ++t) {
                String[] argTypeResult = new String[12];
                argTypeResult[0] = argTypes[t];
                argTypeResult[1] = String.format("%.2f", 100.0 * (supportedTestByType[fold][t] + nonSupportedTestByType[fold][t]) /
                        (supportedTest[fold] + nonSupportedTest[fold])) + "%";
                argTypeResult[2] = String.valueOf(supportedTestByType[fold][t] + nonSupportedTestByType[fold][t]);
                argTypeResult[3] = String.valueOf(supportedTestByType[fold][t]);
                argTypeResult[4] = String.format("%.2f", 100.0 * supportedTestCorrectByType[fold][t][0] / supportedTestByType[fold][t]) + "%";
                argTypeResult[5] = String.format("%.2f", 100.0 * supportedTestCorrectByType[fold][t][2] / supportedTestByType[fold][t]) + "%";
                argTypeResult[6] = String.format("%.2f", 100.0 * supportedTestCorrectByType[fold][t][4] / supportedTestByType[fold][t]) + "%";
                argTypeResult[7] = String.format("%.2f", 100.0 * supportedTestCorrectByType[fold][t][9] / supportedTestByType[fold][t]) + "%";
                argTypeResult[8] = String.format(
                        "%.2f", 100.0 * (supportedTestCorrectByType[fold][t][0] + nonSupportedTestCorrectByType[fold][t][0])
                                / (supportedTestByType[fold][t] + nonSupportedTestByType[fold][t])) + "%";
                argTypeResult[9] = String.format(
                        "%.2f", 100.0 * (supportedTestCorrectByType[fold][t][2] + nonSupportedTestCorrectByType[fold][t][2])
                                / (supportedTestByType[fold][t] + nonSupportedTestByType[fold][t])) + "%";
                argTypeResult[10] = String.format(
                        "%.2f", 100.0 * (supportedTestCorrectByType[fold][t][4] + nonSupportedTestCorrectByType[fold][t][4])
                                / (supportedTestByType[fold][t] + nonSupportedTestByType[fold][t])) + "%";
                argTypeResult[11] = String.format(
                        "%.2f", 100.0 * (supportedTestCorrectByType[fold][t][9] + nonSupportedTestCorrectByType[fold][t][9])
                                / (supportedTestByType[fold][t] + nonSupportedTestByType[fold][t])) + "%";
                result.add(argTypeResult);
            }
            String[] summarizationResult = new String[12];
            summarizationResult[2] = String.valueOf(supportedTest[fold] + nonSupportedTest[fold]);
            summarizationResult[3] = String.valueOf(supportedTest[fold]);
            summarizationResult[4] = String.format("%.2f", 100.0 * supportedTestCorrect[fold][0] / supportedTest[fold]) + "%";
            summarizationResult[5] = String.format("%.2f", 100.0 * supportedTestCorrect[fold][2] / supportedTest[fold]) + "%";
            summarizationResult[6] = String.format("%.2f", 100.0 * supportedTestCorrect[fold][4] / supportedTest[fold]) + "%";
            summarizationResult[7] = String.format("%.2f", 100.0 * supportedTestCorrect[fold][9] / supportedTest[fold]) + "%";
            summarizationResult[8] = String.format(
                    "%.2f", 100.0 * (supportedTestCorrect[fold][0] + nonSupportedTestCorrect[fold][0]) /
                            (supportedTest[fold] + nonSupportedTest[fold])) + "%";
            summarizationResult[9] = String.format(
                    "%.2f", 100.0 * (supportedTestCorrect[fold][2] + nonSupportedTestCorrect[fold][2]) /
                            (supportedTest[fold] + nonSupportedTest[fold])) + "%";
            summarizationResult[10] = String.format(
                    "%.2f", 100.0 * (supportedTestCorrect[fold][4] + nonSupportedTestCorrect[fold][4]) /
                            (supportedTest[fold] + nonSupportedTest[fold])) + "%";
            summarizationResult[11] = String.format(
                    "%.2f", 100.0 * (supportedTestCorrect[fold][9] + nonSupportedTestCorrect[fold][9]) /
                            (supportedTest[fold] + nonSupportedTest[fold])) + "%";
            result.add(summarizationResult);
            String[] emptyLine = {};
            result.add(emptyLine);
            String[] avgRuntime = {"Average run time", totalRuntime[fold] / (supportedTest[fold] + nonSupportedTest[fold]) + " second(s)"};
            result.add(avgRuntime);
            String[] MRR = {"MRR", String.format("%.2f", PROJECT_RR / (supportedTest[fold] + nonSupportedTest[fold]))};
            result.add(MRR);
            writer.writeAll(result);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
