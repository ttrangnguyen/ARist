package flute.jdtparser;

import flute.config.Config;
import flute.jdtparser.callsequence.FileNode;
import flute.jdtparser.callsequence.MethodCallNode;
import flute.jdtparser.callsequence.node.cfg.MinimalNode;
import flute.jdtparser.callsequence.node.cfg.Utils;
import flute.utils.logging.Logger;
import org.eclipse.jdt.core.dom.IBinding;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrainTest {
    static ProjectParser projectParser;
    static int fold = 0;

    public static List<String> fileList = new ArrayList<>();

    public static void readFileList() {
        fileList.add("/Users/maytinhdibo/Research/java-data/lucene/lucene/src/java/org/apache/lucene/index/SegmentInfos.java");
    }

    public static void setFold(int fold) {
        TrainTest.fold = fold;
    }

    public static void main(String[] args) throws IOException {
        String projectName = "lucene";

        Config.loadConfig(Config.STORAGE_DIR + "/json/" + projectName + ".json");
        projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
                Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);

        readFileList();
        setFold(0);

        File directory = new File(Config.LOG_DIR + "/" + projectName + "/fold" + fold + "/");
        if (!directory.exists()) {
            Files.createDirectories(Paths.get(directory.getAbsolutePath()));
        }

        for (String filePath : fileList) {
            File curFile = new File(filePath);
            FileParser fileParser = new FileParser(projectParser, curFile, 0);

            FileNode fileNode = new FileNode(fileParser);
            fileNode.parse();

            // Build CFGs
            List<MinimalNode> rootNodeList = fileNode.getRootNodeList();
            for (MinimalNode rootNode : rootNodeList) {
                // Build method invoc trees from CFGs
                MethodCallNode methodCallNode = Utils.visitMinimalNode(rootNode);

                // Group by tracking node
                Map<IBinding, MethodCallNode> map = Utils.groupMethodCallNodeByTrackingNode(methodCallNode);
                for (IBinding id : map.keySet()) {
                    // Generate method invoc sequences
                    List<List<String>> sequences = Utils.visitMethodCallNode(map.get(id));
                    for (List<String> sequence : sequences) {
                        Logger.write(String.join(" ", sequence), projectName + "/fold" + fold + "/" + curFile.getName() + ".txt");
                    }
                }
            }
        }
    }
}