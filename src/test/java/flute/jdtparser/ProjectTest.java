package flute.jdtparser;

import flute.config.Config;
import flute.data.MultiMap;
import flute.data.exception.ClassScopeNotFoundException;
import flute.data.exception.MethodInvocationNotFoundException;
import flute.utils.file_processing.DirProcessor;
import flute.utils.logging.Timer;
import flute.utils.mvn.MvnDownloader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.jdt.core.compiler.IProblem;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class ProjectTest {

    public static void justRun() throws IOException {
        Config.PROJECT_DIR = Config.STORAGE_DIR + "/repositories/git/JAVA_repos/demo";

        //download jar from pom.xml
        AtomicReference<String> jarFolder = new AtomicReference<>();
        DirProcessor.getAllEntity(new File(Config.PROJECT_DIR), false).stream().filter(file -> {
            return file.getAbsolutePath().endsWith("/pom.xml");
        }).forEach(pomFile -> {
            try {
                jarFolder.set(
                        MvnDownloader.download(Config.PROJECT_DIR, pomFile.getAbsolutePath()).getAbsolutePath()
                );
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
        });

        //scan src folder
        String[] prefixSrc = new String[]{"/src", "/demosrc", "/testsrc", "/antsrc", "/src_ant", "/src/main/java"};
        for (String str : prefixSrc) {
            try {
                Config.loadSrcPath(Config.PROJECT_DIR, str);
            } catch (Exception e) {
            }
        }

        //scan jar file
        if (jarFolder.get() != null) Config.loadJarPath(jarFolder.get());

        ProjectParser projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
                Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);

        //INPUT: Test file
        Config.TEST_FILE_PATH = "";
        File curFile = new File(Config.TEST_FILE_PATH);
        //INPUT: Position after .
        FileParser fileParser = new FileParser(projectParser, curFile, Config.TEST_POSITION);

        try {
            fileParser.parse();
            //INPUT: parameter position
            MultiMap result = fileParser.genParamsAt(0);
            //Print result excode -> lexcial
            printMap(result, "Result");
        } catch (MethodInvocationNotFoundException e) {
            e.printStackTrace();
        } catch (ClassScopeNotFoundException e) {
            e.printStackTrace();
        }

    }

    public static void main(String args[]) throws IOException {
        justRun();
//        Timer timer = new Timer();
//
//        System.out.println("Starting parse...");
//
//        //auto load src and .jar file
//        Config.loadConfig(Config.STORAGE_DIR + "/json/netbeans.json");
//        System.out.print("Auto load binding time: ");
//        System.out.printf("%.5fs\n", timer.getTimeCounter() / 1000.0);
//
//        //gen and parse project
//        ProjectParser projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
//                Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);
//
//        //projectParser.parse();
//        //System.out.print("Project parse time: ");
//        //System.out.printf("%.5fs\n", timer.getTimeCounter() / 1000.0);
//
//        //binding test
//        //projectParser.bindingTest();
//        //System.out.print("Binding test time: ");
//        //System.out.printf("%.5fs\n", timer.getTimeCounter() / 1000.0);
//
//        //parse test file
//        File curFile = new File(Config.TEST_FILE_PATH);
//        FileParser fileParser = new FileParser(projectParser, curFile, Config.TEST_POSITION);
//
//        try {
//            fileParser.parse();
//            MultiMap result = fileParser.genParamsAt(2);
//
//            System.out.println(result);
//        } catch (MethodInvocationNotFoundException e) {
//            e.printStackTrace();
//        } catch (ClassScopeNotFoundException e) {
//            e.printStackTrace();
//        }
//
//        MultiMap nextParams = null;
//        MultiMap firstParams = null;
//
//        try {
//            timer.startCounter();
//            fileParser.parse();
//            System.out.print("File parse time: ");
//            System.out.printf("%.5fs\n", timer.getTimeCounter() / 1000.0);
//
//            nextParams = fileParser.genNextParams();
//            System.out.print("Next param gen time: ");
//            System.out.printf("%.5fs\n", timer.getTimeCounter() / 1000.0);
//
//            firstParams = fileParser.genFirstParams();
//            System.out.print("First param gen time: ");
//            System.out.printf("%.5fs\n", timer.getTimeCounter() / 1000.0);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        printMap(nextParams, "Next param=");
//        System.out.println("");
//        printMap(firstParams, "First param");
//
//        timer.startCounter();
//        //type check
//        List<IProblem> problems = fileParser.getErrors(startPos, stopPos);
//
//        System.out.println("Type check: " + problems.isEmpty());
//        System.out.print("Type check time: ");
//        System.out.printf("%.5fs\n", timer.getTimeCounter() / 1000.0);
//
//        //end parse
//        System.out.println("Parse done!");
    }

    public static void printMap(MultiMap data, String title) {
        System.out.println("===========" + title + "===========");
        if (data != null) {
            for (Map.Entry<String, List<String>> entry : data.getValue().entrySet()) {
                System.out.printf("%-40s", "\"" + entry.getKey() + "\"");
                System.out.print(" -> ");

                System.out.print("[");
                System.out.print(String.join(", ", entry.getValue().stream().map(item -> "\"" + item + "\"").collect(Collectors.toList())));
                System.out.println("]");
            }
        } else {
            System.out.println("Can't not find any match.");
        }
        System.out.println("================================");
    }
}
