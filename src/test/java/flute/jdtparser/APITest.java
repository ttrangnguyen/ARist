package flute.jdtparser;

import com.github.javaparser.Position;
import flute.config.Config;
import flute.data.MultiMap;
import flute.data.exception.TestPathDetectException;
import flute.data.testcase.BaseTestCase;
import flute.jdtparser.callsequence.node.cfg.Utils;
import flute.utils.Pair;
import flute.utils.file_processing.DirProcessor;
import org.apache.maven.shared.invoker.*;
import org.eclipse.jdt.core.dom.IMethodBinding;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class APITest {
    private static String curProject = "";
    private static ProjectParser curProjectParser = null;

    private static void initProject(String projectName) {
        curProject = projectName;
        Config.PROJECT_DIR = APITestGenerator.REPO_FOLDER + projectName;

        String[] prefixSrc = new String[]{"/src", "/demosrc", "/testsrc", "/antsrc", "/src_ant", "/src/main/java"};
        for (String str : prefixSrc) {
            try {
                Config.loadSrcPath(Config.PROJECT_DIR, str);
            } catch (Exception e) {
            }
        }

        //download jar from pom.xml
        DirProcessor.getAllEntity(new File(Config.PROJECT_DIR), false).stream().filter(file -> {
            return file.getAbsolutePath().endsWith("/pom.xml");
        }).forEach(pomFile -> {
//            try {
//                MvnDownloader.download(Config.PROJECT_DIR, pomFile.getAbsolutePath());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
            InvocationRequest request = new DefaultInvocationRequest();
            request.setPomFile(pomFile);
            request.setGoals(Arrays.asList("dependency:copy-dependencies", "-DoutputDirectory=\"flute-mvn\""));

            Invoker invoker = new DefaultInvoker();
            invoker.setMavenHome(new File(Config.MVN_HOME));
            try {
                invoker.execute(request);
            } catch (MavenInvocationException e) {
                e.printStackTrace();
            }
        });

        try {
            Config.loadJarPath(Config.PROJECT_DIR);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        curProjectParser = new ProjectParser(Config.PROJECT_DIR,
                Config.SOURCE_PATH, Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);
    }

    private static FileParser genFileParser(BaseTestCase testCase) throws TestPathDetectException {
        if (!curProject.equals(testCase.getProjectName())) {
            initProject(testCase.getProjectName());
        }

        File curFile = new File(APITestGenerator.REPO_FOLDER + testCase.getProjectName() + "/" + testCase.getRelativeFilePath());
        if (Utils.checkTestFile(curFile)) throw new TestPathDetectException("");
        FileParser fileParser = new FileParser(curProjectParser, curFile, testCase.getBeginPosition().line, testCase.getBeginPosition().column);
        return fileParser;
    }

    public static MultiMap test(BaseTestCase testCase) throws Exception {
        FileParser fileParser = genFileParser(testCase);

        fileParser.parse();
        return fileParser.genCurParams();
    }

    public static Pair<FileParser, Optional<List<IMethodBinding>>> methodTest(BaseTestCase testCase) throws Exception {
        FileParser fileParser = genFileParser(testCase);

        fileParser.parse();
        return new Pair<>(fileParser, fileParser.genMethodCall());
    }

    public static void main(String[] args) throws MavenInvocationException {
        BaseTestCase testCase = new BaseTestCase(
                "3breadt_dd-plist", "src/test/java/com/dd/plist/test/NSNumberTest.java",
                new Position(81, 40), "context", "outer", "target"
        );

        try {
            MultiMap result = test(testCase);
            ProjectTest.printMap(result, "RESULT");
        } catch (Exception e) {
            ;
            e.printStackTrace();
        }
    }
}
