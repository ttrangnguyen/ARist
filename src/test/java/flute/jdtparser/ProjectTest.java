package flute.jdtparser;

import flute.config.Config;
import flute.data.MultiMap;
import org.eclipse.jdt.core.compiler.IProblem;
import flute.utils.logging.Timer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProjectTest {
    private static int startPos = 0;
    private static int stopPos = 20000;

    public static void main(String args[]) throws IOException {
        Timer timer = new Timer();

        Config.loadConfig(Config.STORAGE_DIR + "/json/ant.json");
        System.out.println("Starting parse...");

        //gen and parse project
        ProjectParser projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
                Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);

        //projectParser.parse();

        System.out.print("Project parse time: ");
        System.out.printf("%.5fs\n", timer.getTimeCounter() / 1000.0);

        //parse test file
        File curFile = new File(Config.TEST_FILE_PATH);
        FileParser fileParser = new FileParser(projectParser, curFile, Config.TEST_POSITION);

        MultiMap nextParams = null;
        MultiMap firstParams = null;

        try {
            timer.startCounter();
            fileParser.parse();
            System.out.print("File parse time: ");
            System.out.printf("%.5fs\n", timer.getTimeCounter() / 1000.0);

            nextParams = fileParser.genNextParams();
            System.out.print("Next param gen time: ");
            System.out.printf("%.5fs\n", timer.getTimeCounter() / 1000.0);

            firstParams = fileParser.genFirstParams();
            System.out.print("First param gen time: ");
            System.out.printf("%.5fs\n", timer.getTimeCounter() / 1000.0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        printMap(nextParams, "Next param=");
        System.out.println("");
        printMap(firstParams, "First param");

        timer.startCounter();
        //type check
        List<IProblem> problems = fileParser.getErrors(startPos, stopPos);

        System.out.println("Type check: " + problems.isEmpty());
        System.out.print("Type check time: ");
        System.out.printf("%.5fs\n", timer.getTimeCounter() / 1000.0);

        //end parse
        System.out.println("Parse done!");
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
