package flute.testing;

import flute.utils.file_processing.FileProcessor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FoldTestSetEquivalenceChecker {
    public static void main(String[] args) throws IOException {
        String projectName = args[0];
        int foldCount = 10;
        String testFilenameFormat = "%s_ArgRecTests_fold%d.txt";
        String testDir1 = "../../Kien/Flute-Kien-full/storage/logs/";
        String testDir2 = "../../Kien/Flute-Kien-full/storage/logs_ecnet/";

        List<String> badFoldList = new ArrayList<>();
        for (int foldId = 0; foldId < foldCount; ++foldId) {
            File testFile1 = new File(testDir1 + String.format(testFilenameFormat, projectName, foldId));
            File testFile2 = new File(testDir2 + String.format(testFilenameFormat, projectName, foldId));
            if (testFile1.exists() && testFile2.exists()) {
                List<String> testSet1 = FileProcessor.readLineByLineToList(testFile1.getAbsolutePath());
                List<String> testSet2 = FileProcessor.readLineByLineToList(testFile2.getAbsolutePath());
                if (testSet1.size() == testSet2.size()) {
                    // Compare further
                } else badFoldList.add("Mismatch: " + projectName + " at fold" + foldId + ": " + testSet1.size() + " != " + testSet2.size());
            } else badFoldList.add("Missing: " + projectName + " " + foldId);
        }
        FileProcessor.writeListLineByLine(badFoldList, "log_FoldTestSetEquivalenceChecker.txt");
    }
}
