package flute.evaluating;

import com.google.gson.Gson;
import flute.candidate.Candidate;
import flute.communicating.SingleParamRequest;
import flute.config.ModelConfig;
import flute.matching.CandidateMatcher;
import org.apache.commons.lang3.math.NumberUtils;
import slp.core.lexing.code.JavaLexer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestQuery {
    public static boolean softEqual(String prediction, String answer) {
        if (prediction.equals(answer)) return true;
        if (NumberUtils.isCreatable(prediction) && NumberUtils.isCreatable(answer))  return true;
        if (prediction.startsWith("\"") && answer.startsWith("\"") &&
                prediction.endsWith("\"") && answer.endsWith("\"")) return true;
        if (prediction.startsWith("'") && prediction.endsWith("'") &&
                (answer.equals("0") || (answer.startsWith("'") && answer.endsWith("'")))) return true;
        if (prediction.equals(". class") && answer.endsWith(".class")) return true;
        return false;
    }

    public static boolean softEqualSequence(String prediction, String answer, String testArgType) {
        if (softEqual(prediction, answer)) return true;
        if (prediction.contains("->")) return testArgType.equals("LAMBDA");
        if (testArgType.equals("LAMBDA")) return prediction.equals("<LAMBDA>");
        if (testArgType.equals("COMPOUND")) return prediction.equals("<COMPOUND>");
        JavaLexer lexer = new JavaLexer();
        List<String> predictionTokens;
        if (ModelConfig.USE_BEAM_SEARCH) {
            predictionTokens = Arrays.asList(prediction.split(" "));
        } else {
            predictionTokens = lexer.lexLine(prediction).collect(Collectors.toList());
        }
        List<String> normalizedPredictionTokens = new ArrayList<>();
        int openBracketLevel = 0;
        for (String token : predictionTokens) {
            if (token.equals("[")) {
                openBracketLevel += 1;
                normalizedPredictionTokens.add(token);
            } else if (token.equals("]")) {
                openBracketLevel -= 1;
                normalizedPredictionTokens.add(token);
            } else if (openBracketLevel == 0) {
                normalizedPredictionTokens.add(token);
            }
        }
        String normalizedPrediction = String.join("", normalizedPredictionTokens).replaceAll(" ", "");
        if (answer.startsWith("(")) {
            // if answer is a cast, check second occurence
            int secondParenIndex = answer.indexOf("(", answer.indexOf("(")+1);
            if (secondParenIndex > -1)
                answer = answer.substring(0, secondParenIndex+1);
        } else if (answer.contains("(")) {
            // else eliminate subsequent calls
            answer = answer.substring(0, answer.indexOf("(")+1);
        }
        if (answer.startsWith("this.")) answer = answer.substring(5);
        if (normalizedPrediction.startsWith("this.")) normalizedPrediction = normalizedPrediction.substring(5);
        if (answer.startsWith("new ")) {
            // duplicate because want to distinguish "new" and things that can start with "new", like "newStudent"
            answer = answer.replaceAll(" ", "");
            // new A.B.C( == new C(
            // remove "new" to compare A.B.C( to C(
            return normalizedPrediction.length() > 3 && answer.substring(3).endsWith(normalizedPrediction.substring(3));
        } else {
            answer = answer.replaceAll(" ", "");
            // in case java.swing.SomeClass.someMethod( == SomeClass.someMethod(
            return answer.endsWith(normalizedPrediction);
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

    public static boolean properlyGeneratedLex(String request) {
        Gson gson = new Gson();
        SingleParamRequest jsonRequest = gson.fromJson(request, SingleParamRequest.class);
//        System.out.println(jsonRequest.expected_lex.substring(0, jsonRequest.expected_lex.indexOf("(")));
        String normalizedAnswer = jsonRequest.expected_lex;
        String testArgType = jsonRequest.argType;
        normalizedAnswer = normalizedAnswer.replaceAll("\\[.*?]", "[]");
        if (testArgType.equals("METHOD_INVOC") || testArgType.equals("OBJ_CREATION")) {
            normalizedAnswer = normalizedAnswer.substring(0, normalizedAnswer.indexOf("(")+1);
        }
        for (List<String> candsByExcode: jsonRequest.next_lex) {
            for (String lexCand : candsByExcode) {
                Candidate candidate = new Candidate("", lexCand);
                if (CandidateMatcher.matches(candidate, normalizedAnswer)) {
                    return true;
                }
            }
        }
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
            normalizedAnswer = normalizedAnswer.substring(0, normalizedAnswer.lastIndexOf("(")+1);
        return normalizedAnswer;
    }

    public static int getNumberOfLexicalCandidates(String request) {
        Gson gson = new Gson();
        SingleParamRequest jsonRequest = gson.fromJson(request, SingleParamRequest.class);
        int total = 0;
        for (List<String> candsByExcode: jsonRequest.next_lex) {
            total += candsByExcode.size();
        }
        return total;
    }
}
