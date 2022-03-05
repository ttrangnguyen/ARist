package flute.evaluating;

import com.google.gson.Gson;
import com.opencsv.CSVWriter;
import flute.communicating.PredictionDetail;
import flute.communicating.SingleParamRequest;
import flute.config.Config;
import flute.config.ModelConfig;
import flute.config.ProjectConfig;
import flute.config.TestConfig;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SequenceEvaluator {
    private int[] supportedTest = new int[11];
    private int[] nonSupportedTest = new int[11];
    private int[][] supportedTestCorrect = new int[11][50];
    private int[][] nonSupportedTestCorrect = new int[11][50];
    private double[] totalRuntime = new double[11];
    private String[] argTypes = {"NAME", "METHOD_INVOC", "FIELD_ACCESS", "ARRAY_ACCESS", "CAST", "STRING_LIT",
            "NUM_LIT", "CHAR_LIT", "TYPE_LIT", "BOOL_LIT", "NULL_LIT", "OBJ_CREATION", "ARR_CREATION",
            "THIS", "SUPER", "COMPOUND", "LAMBDA", "METHOD_REF", "OTHERS"};
    private HashMap<String, Integer> typeMap = new HashMap<>();

    private int nArgTypes = argTypes.length;
    private int[][] supportedTestByType = new int[11][nArgTypes];
    private int[][][] supportedTestCorrectByType = new int[11][nArgTypes][50];
    private int[][] nonSupportedTestByType = new int[11][nArgTypes];
    private int[][][] nonSupportedTestCorrectByType = new int[11][nArgTypes][50];
    private double[] RR = new double[11];

    public SequenceEvaluator() {
        for (int i = 0; i < argTypes.length; ++i) {
            typeMap.put(argTypes[i], i);
        }
    }

    public void evaluate(String project, String solution) {
        ProjectConfig.project = project;
        for (int fold = 0; fold < 10; ++fold) {
            TestConfig.fold = String.valueOf(fold);
            Config.init();
            evaluate(project, solution, fold);
        }
        for (int fold = 0; fold < 10; ++fold) {
            supportedTest[10] += supportedTest[fold];
            nonSupportedTest[10] += nonSupportedTest[fold];
            totalRuntime[10] += totalRuntime[fold];
            for (int topK = 0; topK < 50; ++topK) {
                supportedTestCorrect[10][topK] += supportedTestCorrect[fold][topK];
                nonSupportedTestCorrect[10][topK] += nonSupportedTestCorrect[fold][topK];
            }
            for (int argTypeId = 0; argTypeId < nArgTypes; ++argTypeId) {
                supportedTestByType[10][argTypeId] += supportedTestByType[fold][argTypeId];
                nonSupportedTestByType[10][argTypeId] += nonSupportedTestByType[fold][argTypeId];
                for (int topK = 0; topK < 50; ++topK) {
                    supportedTestCorrectByType[10][argTypeId][topK] += supportedTestCorrectByType[fold][argTypeId][topK];
                    nonSupportedTestCorrectByType[10][argTypeId][topK] += nonSupportedTestCorrectByType[fold][argTypeId][topK];
                }
            }
        }
        writeResult(solution, 10);
    }

    public void evaluate(String project, String solution, int fold) {
        System.out.println(project + " " + solution + " " + fold);
        Gson gson = new Gson();
        try {
            BufferedReader brDetail = new BufferedReader(new FileReader(TestConfig.predictionDetailPath));
            BufferedReader brTest = new BufferedReader(new FileReader(TestConfig.testCasesFilePath));
            String resultString;
            while ((resultString = brDetail.readLine()) != null) {
                String request = brTest.readLine();
                SingleParamRequest jsonRequest = gson.fromJson(request, SingleParamRequest.class);

                if (TestConfig.EVALUATE_PARC) {
                    if (jsonRequest.methodInvocClassQualifiedName == null) continue;
                    if (jsonRequest.methodInvocClassQualifiedName.startsWith("org.eclipse.swt") ||
                            jsonRequest.methodInvocClassQualifiedName.startsWith("java.awt") ||
                            jsonRequest.methodInvocClassQualifiedName.startsWith("javax.swing")) {

                    } else continue;
                }

                if (TestQuery.isNoParamTest(request)) continue;
                String testArgType = jsonRequest.argType;
                boolean isSupported = (!jsonRequest.ignored) || testArgType.equals("COMPOUND") || testArgType.equals("LAMBDA");
                int argTypeIndex = typeMap.get(testArgType);
                if (isSupported) {
                    supportedTest[fold] += 1;
                    supportedTestByType[fold][argTypeIndex] += 1;
                } else {
                    nonSupportedTest[fold] += 1;
                    nonSupportedTestByType[fold][argTypeIndex] += 1;
                }
                PredictionDetail result = gson.fromJson(resultString, PredictionDetail.class);
                String normalizedAnswer = TestQuery.getNormalizedAnswer(request);
                int i = 0, j = 0;
//                HashSet<String> commonTokens = new HashSet<>();
//                commonTokens.add("0");
//                commonTokens.add(".class");
//                commonTokens.add("\"\"");
//                commonTokens.add("null");
                while (true) {
                    if (j == result.predictions.size() || i == 50) {
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
                for (int i = 0; i < 50; ++i) {
                    for (int t = 0; t < argTypes.length; ++t) {
                        nonSupportedTestCorrectByType[fold][t][i] = 0;
                    }
                    nonSupportedTestCorrect[fold][i] = 0;
                }
            }
            for (int i = 0; i < 10; ++i) {
                RR[fold] += 1.0 * (supportedTestCorrect[fold][i] + nonSupportedTestCorrect[fold][i]) / (i+1);
            }
            for (int i = 1; i < 50; ++i) {
                supportedTestCorrect[fold][i] += supportedTestCorrect[fold][i - 1];
                nonSupportedTestCorrect[fold][i] += nonSupportedTestCorrect[fold][i - 1];
            }
            for (int t = 0; t < argTypes.length; ++t) {
                for (int i = 1; i < 50; ++i) {
                    supportedTestCorrectByType[fold][t][i] += supportedTestCorrectByType[fold][t][i - 1];
                    nonSupportedTestCorrectByType[fold][t][i] += nonSupportedTestCorrectByType[fold][t][i - 1];
                }
            }

            brDetail.close();
            brTest.close();
            writeResult(solution, fold);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeResult(String solution, int fold)  {
        try {
            if (fold == 10) {
                TestConfig.fold = null;
                Config.init();
            }
            String predictionResultPath = TestConfig.predictionResultPath;
            List<String[]> result = new ArrayList<>();
            File fout = new File(predictionResultPath);
            FileWriter outputFile = new FileWriter(fout);
            CSVWriter writer = new CSVWriter(outputFile);
            String[] columns = {"Param type", "Distribution", "Number of tests", "Number of supported tests",
                    "Top-1 precision", "Top-3 precision", "Top-5 precision", "Top-10 precision", "Top-20 precision",
                    "Top-30 precision", "Top-50 precision", "Top-1 recall", "Top-3 recall", "Top-5 recall",
                    "Top-10 recall", "Top-20 recall", "Top-30 recall", "Top-50 recall"};
            result.add(columns);
            for (int t = 0; t < argTypes.length; ++t) {
                String[] argTypeResult = new String[18];
                argTypeResult[0] = argTypes[t];
                argTypeResult[1] = String.format("%.2f", 100.0 * (supportedTestByType[fold][t] + nonSupportedTestByType[fold][t]) /
                        (supportedTest[fold] + nonSupportedTest[fold])) + "%";
                argTypeResult[2] = String.valueOf(supportedTestByType[fold][t] + nonSupportedTestByType[fold][t]);
                argTypeResult[3] = String.valueOf(supportedTestByType[fold][t]);
                argTypeResult[4] = String.format("%.2f", 100.0 * supportedTestCorrectByType[fold][t][0] / supportedTestByType[fold][t]) + "%";
                argTypeResult[5] = String.format("%.2f", 100.0 * supportedTestCorrectByType[fold][t][2] / supportedTestByType[fold][t]) + "%";
                argTypeResult[6] = String.format("%.2f", 100.0 * supportedTestCorrectByType[fold][t][4] / supportedTestByType[fold][t]) + "%";
                argTypeResult[7] = String.format("%.2f", 100.0 * supportedTestCorrectByType[fold][t][9] / supportedTestByType[fold][t]) + "%";
                argTypeResult[8] = String.format("%.2f", 100.0 * supportedTestCorrectByType[fold][t][19] / supportedTestByType[fold][t]) + "%";
                argTypeResult[9] = String.format("%.2f", 100.0 * supportedTestCorrectByType[fold][t][29] / supportedTestByType[fold][t]) + "%";
                argTypeResult[10] = String.format("%.2f", 100.0 * supportedTestCorrectByType[fold][t][49] / supportedTestByType[fold][t]) + "%";
                argTypeResult[11] = String.format(
                        "%.2f", 100.0 * (supportedTestCorrectByType[fold][t][0] + nonSupportedTestCorrectByType[fold][t][0])
                                / (supportedTestByType[fold][t] + nonSupportedTestByType[fold][t])) + "%";
                argTypeResult[12] = String.format(
                        "%.2f", 100.0 * (supportedTestCorrectByType[fold][t][2] + nonSupportedTestCorrectByType[fold][t][2])
                                / (supportedTestByType[fold][t] + nonSupportedTestByType[fold][t])) + "%";
                argTypeResult[13] = String.format(
                        "%.2f", 100.0 * (supportedTestCorrectByType[fold][t][4] + nonSupportedTestCorrectByType[fold][t][4])
                                / (supportedTestByType[fold][t] + nonSupportedTestByType[fold][t])) + "%";
                argTypeResult[14] = String.format(
                        "%.2f", 100.0 * (supportedTestCorrectByType[fold][t][9] + nonSupportedTestCorrectByType[fold][t][9])
                                / (supportedTestByType[fold][t] + nonSupportedTestByType[fold][t])) + "%";
                argTypeResult[15] = String.format(
                        "%.2f", 100.0 * (supportedTestCorrectByType[fold][t][19] + nonSupportedTestCorrectByType[fold][t][19])
                                / (supportedTestByType[fold][t] + nonSupportedTestByType[fold][t])) + "%";
                argTypeResult[16] = String.format(
                        "%.2f", 100.0 * (supportedTestCorrectByType[fold][t][29] + nonSupportedTestCorrectByType[fold][t][29])
                                / (supportedTestByType[fold][t] + nonSupportedTestByType[fold][t])) + "%";
                argTypeResult[17] = String.format(
                        "%.2f", 100.0 * (supportedTestCorrectByType[fold][t][49] + nonSupportedTestCorrectByType[fold][t][49])
                                / (supportedTestByType[fold][t] + nonSupportedTestByType[fold][t])) + "%";
                result.add(argTypeResult);
            }
            String[] summarizationResult = new String[18];
            summarizationResult[2] = String.valueOf(supportedTest[fold] + nonSupportedTest[fold]);
            summarizationResult[3] = String.valueOf(supportedTest[fold]);
            summarizationResult[4] = String.format("%.2f", 100.0 * supportedTestCorrect[fold][0] / supportedTest[fold]) + "%";
            summarizationResult[5] = String.format("%.2f", 100.0 * supportedTestCorrect[fold][2] / supportedTest[fold]) + "%";
            summarizationResult[6] = String.format("%.2f", 100.0 * supportedTestCorrect[fold][4] / supportedTest[fold]) + "%";
            summarizationResult[7] = String.format("%.2f", 100.0 * supportedTestCorrect[fold][9] / supportedTest[fold]) + "%";
            summarizationResult[8] = String.format("%.2f", 100.0 * supportedTestCorrect[fold][19] / supportedTest[fold]) + "%";
            summarizationResult[9] = String.format("%.2f", 100.0 * supportedTestCorrect[fold][29] / supportedTest[fold]) + "%";
            summarizationResult[10] = String.format("%.2f", 100.0 * supportedTestCorrect[fold][49] / supportedTest[fold]) + "%";
            summarizationResult[11] = String.format(
                    "%.2f", 100.0 * (supportedTestCorrect[fold][0] + nonSupportedTestCorrect[fold][0]) /
                            (supportedTest[fold] + nonSupportedTest[fold])) + "%";
            summarizationResult[12] = String.format(
                    "%.2f", 100.0 * (supportedTestCorrect[fold][2] + nonSupportedTestCorrect[fold][2]) /
                            (supportedTest[fold] + nonSupportedTest[fold])) + "%";
            summarizationResult[13] = String.format(
                    "%.2f", 100.0 * (supportedTestCorrect[fold][4] + nonSupportedTestCorrect[fold][4]) /
                            (supportedTest[fold] + nonSupportedTest[fold])) + "%";
            summarizationResult[14] = String.format(
                    "%.2f", 100.0 * (supportedTestCorrect[fold][9] + nonSupportedTestCorrect[fold][9]) /
                            (supportedTest[fold] + nonSupportedTest[fold])) + "%";
            summarizationResult[15] = String.format(
                    "%.2f", 100.0 * (supportedTestCorrect[fold][19] + nonSupportedTestCorrect[fold][19]) /
                            (supportedTest[fold] + nonSupportedTest[fold])) + "%";
            summarizationResult[16] = String.format(
                    "%.2f", 100.0 * (supportedTestCorrect[fold][29] + nonSupportedTestCorrect[fold][29]) /
                            (supportedTest[fold] + nonSupportedTest[fold])) + "%";
            summarizationResult[17] = String.format(
                    "%.2f", 100.0 * (supportedTestCorrect[fold][49] + nonSupportedTestCorrect[fold][49]) /
                            (supportedTest[fold] + nonSupportedTest[fold])) + "%";
            result.add(summarizationResult);
            String[] emptyLine = {};
            result.add(emptyLine);
            String[] avgRuntime = {"Average run time", totalRuntime[fold] / (supportedTest[fold] + nonSupportedTest[fold]) + " second(s)"};
            result.add(avgRuntime);
            if (fold == 10) {
                for (int i = 0; i < 10; ++i) RR[10] += RR[i];
            }
            String[] MRR = {"MRR", String.format("%.2f", RR[fold] / (supportedTest[fold] + nonSupportedTest[fold]))};
            result.add(MRR);
            writer.writeAll(result);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
