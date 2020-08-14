package flute.jdtparser;

import flute.config.Config;
import org.eclipse.jdt.core.compiler.IProblem;
import flute.utils.logging.Timer;

import java.io.File;
import java.io.IOException;
import java.util.List;

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

        projectParser.parse();

        System.out.print("Project parse time: ");
        System.out.printf("%.5fs\n", timer.getTimeCounter() / 1000.0);

        //parse test file
        File curFile = new File(Config.TEST_FILE_PATH);
        FileParser fileParser = new FileParser(projectParser, curFile, Config.TEST_POSITION);

        try {
            fileParser.parse();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.print("File parse: ");
        System.out.printf("%.5fs\n", timer.getTimeCounter() / 1000.0);

        //type check
        List<IProblem> problems = fileParser.getErrors(startPos, stopPos);

        System.out.println("Type check: " + problems.isEmpty());
        System.out.printf("%.5fs\n", timer.getTimeCounter() / 1000.0);

        //end parse
        System.out.println("Parse done!");
    }
}
