package flute.jdtparser;


import com.google.gson.Gson;
import flute.data.MultiMap;
import flute.data.testcase.BaseTestCase;
import flute.data.testcase.Candidate;
import flute.utils.file_processing.FileProcessor;
import flute.utils.logging.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class APITestGenerator {
    public static final String REPO_FOLDER = "storage/repositories/git/JAVA_repos/";
    public static final String INPUT_FOLDER = "/Users/maytinhdibo/Downloads/generated_test_cases/";

    public static void main(String[] args) {
        File inputFolder = new File(INPUT_FOLDER);
        Gson gson = new Gson();
        for (File project : inputFolder.listFiles()) {
            //read input
            AtomicInteger numberOfTest = new AtomicInteger();
            List<String> inputs = FileProcessor.readLineByLineToList(project.getAbsolutePath() + "/parameter_name_test_cases.jsonl");
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
                    Logger.write(gson.toJson(testCase), "/out/" + project.getName() + "/parameter_name_candidates.jsonl");
                    numberOfTest.getAndIncrement();
                } catch (Exception e) {
//                    e.printStackTrace();
                }
            });
            System.out.println("[RESULT] --------------[Generated " + numberOfTest + " tests]--------------");
        }
    }
}
