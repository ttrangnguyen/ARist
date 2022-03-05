package flute.testing;

import flute.config.ModelConfig;
import flute.config.ProjectConfig;
import flute.config.TestConfig;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class TestFilesManager {
    public static HashSet<File> testFiles = new HashSet<>();
    public static HashSet<File> testProjects = new HashSet<>();

    public static void init() {
        if (ProjectConfig.CUGLM) {
            if (!ModelConfig.USE_MAINTENANCE) {
                File[] testProjectPaths = new File(ProjectConfig.cugLMTestProjectsPath).listFiles(File::isDirectory);
                assert testProjectPaths != null;
                for (File testProjectPath : testProjectPaths) {
                    testProjects.add(new File(testProjectPath.getAbsolutePath().replace(ProjectConfig.cugLMTestProjectsPath, ProjectConfig.cugLMAllProjectsPath)));
                }
            } else {
                TestConfig.testFilePath = "storage/testfilepath/maintenance_files.txt";
                try {
                    File allTestFilePaths = new File(TestConfig.testFilePath);
                    FileReader fr = new FileReader(allTestFilePaths);
                    BufferedReader br = new BufferedReader(fr);
                    String path;
                    while ((path = br.readLine()) != null) {
                        testFiles.add(new File(path));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (TestConfig.lastParcFold == null) {
                TestConfig.testFilePath = "storage/testfilepath/" + ProjectConfig.project + "/fold"
                        + TestConfig.fold + "/" + ProjectConfig.project + ".txt";
                addParcTestFiles();
            } else {
                for(int i = TestConfig.lastParcFold + 1; i < 10; ++i) {
                    TestConfig.testFilePath = "storage/testfilepath/" + ProjectConfig.project + "/fold"
                            + i + "/" + ProjectConfig.project + ".txt";
                    addParcTestFiles();
                }
            }
        }
    }

    private static void addParcTestFiles() {
        try {
            File allTestFilePaths = new File(TestConfig.testFilePath);
            FileReader fr = new FileReader(allTestFilePaths);
            BufferedReader br = new BufferedReader(fr);
            String path;
            while ((path = br.readLine()) != null) {
                path = path.substring(0, path.length() - 4);
                testFiles.add(new File(ProjectConfig.generatedDataRoot + path + "java"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isTestFile(File f) {
        // if cuglm and maintenance, use a list of test files
        if (ProjectConfig.CUGLM && !ModelConfig.USE_MAINTENANCE) {
            for (File proj : testProjects) {
                if (f.getAbsolutePath().startsWith(proj.getAbsolutePath())) {
                    return true;
                }
            }
            return false;
        } else return testFiles.contains(f);
    }
}
