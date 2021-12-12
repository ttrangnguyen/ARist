package flute.testing;

import flute.utils.file_processing.FileProcessor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProjectTestSetEquivalenceChecker {
    public static void main(String[] args) throws IOException {
        String testFilenameSuffix = "_ArgRecTests.txt";
        String testDir1 = "storage/logs/";
        String testDir2 = "storage/logs_gpt/";
        List<String> projectList = FileProcessor.readLineByLineToList("../storage/four_hundred_projects.txt");

        List<String> badProjectList = new ArrayList<>();
        for (String project : projectList) {
            File testFile1 = new File(testDir1 + project + testFilenameSuffix);
            File testFile2 = new File(testDir2 + project + testFilenameSuffix);
            if (testFile1.exists() && testFile2.exists()) {
                List<String> testSet1 = FileProcessor.readLineByLineToList(testFile1.getAbsolutePath());
                List<String> testSet2 = FileProcessor.readLineByLineToList(testFile2.getAbsolutePath());
                if (testSet1.size() == testSet2.size()) {
                    // Compare further
                } else badProjectList.add(project);
            } else badProjectList.add(project);
        }
        FileProcessor.writeListLineByLine(badProjectList, "log_ProjectTestSetEquivalenceChecker.txt");
    }
}
