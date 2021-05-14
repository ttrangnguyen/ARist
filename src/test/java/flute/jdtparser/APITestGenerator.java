package flute.jdtparser;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import flute.data.MultiMap;
import flute.data.testcase.BaseTestCase;
import flute.data.testcase.Candidate;
import flute.utils.file_processing.FileProcessor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class APITestGenerator {
    public static final String REPO_FOLDER = "storage/repositories/git/JAVA_repos/";
    public static final String INPUT_FOLDER = "/Users/maytinhdibo/Downloads/generated_test_cases/";
    public static final String OUTPUT_FOLDER = "storage/logs/out/";

    public static final String PROJECT_NAME = "3breadt_dd-plist";

    public static void main(String[] args) throws IOException {
//        File inputFolder = new File(INPUT_FOLDER);
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        ;

        File project = new File(INPUT_FOLDER);
//        for (File project : inputFolder.listFiles()) {
        //read input
        AtomicInteger numberOfTest = new AtomicInteger();
        AtomicInteger numberOfErrorTest = new AtomicInteger();
        List<String> inputs = FileProcessor.readLineByLineToList(
                Paths.get(INPUT_FOLDER, PROJECT_NAME, "parameter_name_test_cases.jsonl").toString());

        File outFile = new File(Paths.get(OUTPUT_FOLDER, PROJECT_NAME, "parameter_name_candidates.jsonl").toString());
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
                bw.write(gson.toJson(testCase));
                bw.newLine();
                numberOfTest.getAndIncrement();
            } catch (Exception e) {
//                    e.printStackTrace();
                numberOfErrorTest.getAndIncrement();
            }
        });
        System.out.println("[RESULT] --------------[Generated " + numberOfTest + " tests]--------------");
        System.out.println("         --------------[Ignore " + numberOfErrorTest + " tests]--------------");
        bw.close();
    }
//    }
}
