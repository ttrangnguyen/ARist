package flute.jdtparser;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import flute.config.Config;
import flute.data.MultiMap;
import flute.data.exception.TestPathDetectException;
import flute.data.testcase.BaseTestCase;
import flute.data.testcase.Candidate;
import flute.data.testcase.MethodCandidate;
import flute.jdtparser.callsequence.node.cfg.Utils;
import flute.testing.CandidateMatcher;
import flute.utils.Pair;
import flute.utils.file_processing.FileProcessor;
import org.eclipse.jdt.core.dom.IMethodBinding;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class APITestGenerator {
    public static final String REPO_FOLDER = System.getProperty("repoFolder", "storage/repositories/git/JAVA_repos/");
    public static final String INPUT_FOLDER = System.getProperty("inputFolder", "storage/repositories/generated_test_cases");
    public static final String OUTPUT_FOLDER = System.getProperty("outputFolder", "storage/logs/out/");

    public static final String PROJECT_NAME = System.getProperty("repoName", "3breadt_dd-plist");

    public static final boolean PARAM_TEST = Boolean.parseBoolean(System.getProperty("testParam", "true"));

    public static void main(String[] args) throws IOException {
        if (APITest.initProject(PROJECT_NAME) == 0) {
            if (PARAM_TEST) {
                Config.TARGET_PARAM_POSITION = true;
                paramTest();
            } else {
                Config.TARGET_PARAM_POSITION = false;
                methodTest();
            }
        } else {
            System.out.println("___________BUILD_FAILED___________[" + PROJECT_NAME + "]");
        }
    }

    private static void methodTest() throws IOException {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        //read input
        AtomicInteger numberOfTest = new AtomicInteger();
        AtomicInteger numberOfErrorTest = new AtomicInteger();
        AtomicInteger numberOfMatchedCandidate = new AtomicInteger();
        AtomicInteger numberOfLoggingMethodTest = new AtomicInteger();
        AtomicInteger numberOfTestFile = new AtomicInteger();

        List<String> inputs = FileProcessor.readLineByLineToList(
                Paths.get(INPUT_FOLDER, PROJECT_NAME, "method_invocation_test_cases.jsonl").toString());

        File outFile = new File(Paths.get(OUTPUT_FOLDER, PROJECT_NAME, "method_invocation_candidates.jsonl").toString());
        if (!outFile.exists()) {
            outFile.getParentFile().mkdirs();
        } else {
            outFile.delete();
        }
        outFile.createNewFile();

        FileWriter fstream = new FileWriter(outFile.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fstream);

        inputs.forEach(input -> {
            BaseTestCase testCase = gson.fromJson(input, BaseTestCase.class);
            try {
                Pair<FileParser, Optional<List<IMethodBinding>>> result = APITest.methodTest(testCase);
                IMethodBinding targetMethod = result.getFirst().getCurMethodInvocation().resolveMethodBinding();

                testCase.setTargetId(Utils.nodeToString(targetMethod));
                boolean isLoggingMethod = APITest.isLoggingMethod(targetMethod);
                if (isLoggingMethod) {
                    numberOfLoggingMethodTest.getAndIncrement();
                }
                testCase.setLoggingMethod(isLoggingMethod);
                if (result.getSecond().isPresent()) {
                    List<MethodCandidate> methodResult = result.getSecond().get().stream().map(method -> {
                        MethodCandidate methodCandidate = new MethodCandidate(method.getName(), Utils.nodeToString(method));
                        if (method.getMethodDeclaration().isEqualTo(targetMethod.getMethodDeclaration())) {
                            methodCandidate.setTargetMatched(true);
                            numberOfMatchedCandidate.getAndIncrement();
                        }

                        return methodCandidate;
                    }).collect(Collectors.toList());

                    testCase.setMethod_candidates(methodResult);
                    bw.write(gson.toJson(testCase));
                    bw.newLine();
                    numberOfTest.getAndIncrement();
                } else {
                    throw new Exception("Not found");
                }
            } catch (TestPathDetectException e) {
                numberOfTestFile.getAndIncrement();
            } catch (Exception e) {
//                    e.printStackTrace();
                numberOfErrorTest.getAndIncrement();
            }
        });

        bw.close();
        fstream.close();

        System.out.println("[RESULT]---[Project_Name " + PROJECT_NAME + "]---[Total_Valid_Test_Case " + numberOfTest + " tests]---"
                + "---[Candidate_Matched_Test_Case " + numberOfMatchedCandidate + " tests]---"
                + "---[Logging_Method_Test_Case " + numberOfLoggingMethodTest + " tests]---"
                + "---[Unknown_Ignored_Test_Case " + numberOfErrorTest + " tests]---"
                + "---[Total_Invalid_Test_Case " + numberOfTestFile + " tests]");
    }

    private static void paramTest() throws IOException {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        //read input
        AtomicInteger numberOfTest = new AtomicInteger();
        AtomicInteger numberOfMatchedCandidate = new AtomicInteger();
        AtomicInteger numberOfErrorTest = new AtomicInteger();
        AtomicInteger numberOfTestFile = new AtomicInteger();

        List<String> inputs = FileProcessor.readLineByLineToList(
                Paths.get(INPUT_FOLDER, PROJECT_NAME, "parameter_name_test_cases.jsonl").toString());

        File outFile = new File(Paths.get(OUTPUT_FOLDER, PROJECT_NAME, "parameter_name_candidates.jsonl").toString());
//        System.out.println(outFile.toPath().toAbsolutePath());
        if (!outFile.exists()) {
            outFile.getParentFile().mkdirs();
        } else {
            outFile.delete();
        }
        outFile.createNewFile();

        FileWriter fstream = new FileWriter(outFile.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fstream);

        inputs.forEach(input -> {
            BaseTestCase testCase = gson.fromJson(input, BaseTestCase.class);
            try {
                MultiMap result = APITest.test(testCase);
                List<Candidate> candidates = new ArrayList<>();
                result.getValue().forEach((excode, value) -> {
                    value.forEach(lexCandidate -> {
                                candidates.add(new Candidate(excode, lexCandidate));
                            }
                    );
                });

                testCase.setCandidates(candidates);
                for (Candidate candidate : candidates)
                    if (CandidateMatcher.matches(candidate, testCase.getTarget())) {
                        candidate.setTargetMatched(true);
                        numberOfMatchedCandidate.getAndIncrement();
                        break;
                    }
                bw.write(gson.toJson(testCase));
                bw.newLine();
                numberOfTest.getAndIncrement();
            } catch (TestPathDetectException e) {
                numberOfTestFile.getAndIncrement();
            } catch (Exception e) {
//                e.printStackTrace();
                numberOfErrorTest.getAndIncrement();
            }
        });
        bw.close();
        fstream.close();

        System.out.println("[RESULT]---[Project_Name " + PROJECT_NAME + "]---[Total_Valid_Test_Case " + numberOfTest + " tests]---"
                + "---[Candidate_Matched_Test_Case " + numberOfMatchedCandidate + " tests]---"
                + "---[Unknown_Ignored_Test_Case " + numberOfErrorTest + " tests]---"
                + "---[Total_Invalid_Test_Case " + numberOfTestFile + " tests]");
    }
}
