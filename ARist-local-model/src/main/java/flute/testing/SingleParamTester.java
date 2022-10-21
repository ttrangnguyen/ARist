package flute.testing;

import com.google.gson.Gson;
import flute.communicating.PredictionDetail;
import flute.communicating.SingleParamRequest;
import flute.communicating.SingleParamServer;
import flute.config.ModelConfig;
import flute.config.ProjectConfig;
import flute.config.TestConfig;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class SingleParamTester extends SingleParamServer {
    @Override
    public void run() {
        try {
            File fout = new File(TestConfig.predictionDetailPath);
            fout.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(fout);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            BufferedReader br = new BufferedReader(new FileReader(TestConfig.testCasesFilePath));
            String request;
            Gson gson = new Gson();
            while ((request = br.readLine()) != null) {
                PredictionDetail result = getTopCands(request);
                bw.write(gson.toJson(result));
                bw.newLine();
            }
            br.close();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isNoParamTest(String request) {
        Gson gson = new Gson();
        SingleParamRequest jsonRequest = gson.fromJson(request, SingleParamRequest.class);
        if (jsonRequest.expected_lex.equals(")")) {
            return true;
        }
        return false;
    }

    public PredictionDetail getTopCands(String request) {
        long start_time_test = System.nanoTime();
        Gson gson = new Gson();
        SingleParamRequest jsonRequest = gson.fromJson(request, SingleParamRequest.class);
        test(request, jsonRequest);
        ArrayList<ScoreInfo> topCands = singleParamTestManager.getTopCands();
        PredictionDetail predictionDetail = new PredictionDetail();
        predictionDetail.predictions = topCands.stream()
                .map(obj -> obj.candidate.lexical)
                .collect(Collectors.toList());
        predictionDetail.lexModelScores = topCands.stream()
                .map(obj -> obj.lexModelScore)
                .collect(Collectors.toList());
        predictionDetail.lexSimScores = topCands.stream()
                .map(obj -> obj.lexSimScore)
                .collect(Collectors.toList());
        predictionDetail.defRecentness = topCands.stream()
                .map(obj -> obj.defRecentness)
                .collect(Collectors.toList());
        predictionDetail.useRecentness = topCands.stream()
                .map(obj -> obj.useRecentness)
                .collect(Collectors.toList());
        predictionDetail.answer = jsonRequest.expected_lex;
        predictionDetail.test_id = jsonRequest.test_id;
        predictionDetail.runtime = (1.0 * System.nanoTime() - start_time_test) / 1000000000;
        predictionDetail.ranking_time = singleParamTestManager.testCase.rankingTime;
        predictionDetail.ps_time = singleParamTestManager.testCase.psCheckTime;
        predictionDetail.n_cands = singleParamTestManager.testCase.numberOfCands;
        return predictionDetail;
    }

    public void test(String requestString, SingleParamRequest jsonRequest) {
        String currentFilePath = ProjectConfig.generatedDataRoot + jsonRequest.filePath;

        if (ProjectConfig.CUGLM) {
            currentFilePath = ProjectConfig.cugLMAllProjectsPath + ProjectConfig.project + "/" + jsonRequest.filePath;
        }

        singleParamTestManager.resolveTestFilePath(currentFilePath);
        singleParamTestManager.initTestCase(requestString);
        singleParamTestManager.resolveContext();
        singleParamTestManager.resolveParamName();
        singleParamTestManager.resolveParamTypeName();
        singleParamTestManager.resolveRealParam();

        // parse lexical params
        singleParamTestManager.addCandsSingleParam();

        // add special tokens
        singleParamTestManager.addCandsSpecialToken();

        if (ModelConfig.USE_PS) {
            singleParamTestManager.addPublicStaticMember();
        }
        if (TestConfig.predictType == TestConfig.PredictType.SEQUENCE) {
            if (TestConfig.TEMPORARY_CORRECT_TEST) singleParamTestManager.resolveCorrectLexicalParam();
            singleParamTestManager.scoreSequences();
        } else {
            singleParamTestManager.scoreFirstToken();
        }
        singleParamTestManager.postProcessing();
    }

    public static boolean softEqual(String prediction, String answer) {
        if (prediction.equals(answer)) return true;
        if (NumberUtils.isCreatable(prediction) && NumberUtils.isCreatable(answer))  return true;
        if (prediction.startsWith("\"") && answer.startsWith("\"") &&
                prediction.endsWith("\"") && answer.endsWith("\"")) return true;
        if (prediction.startsWith("'") && prediction.endsWith("'") && answer.equals("0")) return true;
        return false;
    }

    public static String getNormalizedAnswer(String request) {
        Gson gson = new Gson();
        SingleParamRequest jsonRequest = gson.fromJson(request, SingleParamRequest.class);
        String normalizedAnswer = jsonRequest.expected_lex;
        String testArgType = jsonRequest.argType;
        if (testArgType == null) return normalizedAnswer;
        normalizedAnswer = normalizedAnswer.replaceAll("\\[.*?]", "[]");
        if (testArgType.equals("METHOD_INVOC") || testArgType.equals("OBJ_CREATION"))
            normalizedAnswer = normalizedAnswer.substring(0, normalizedAnswer.indexOf("(")+1);
        return normalizedAnswer;
    }
}
