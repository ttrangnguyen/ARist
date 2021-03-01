package flute.jdtparser;

import flute.config.Config;
import flute.jdtparser.callsequence.FileNode;
import flute.jdtparser.callsequence.MethodCallNode;
import flute.jdtparser.callsequence.node.cfg.MinimalNode;
import flute.jdtparser.callsequence.node.cfg.Utils;
import org.eclipse.jdt.core.dom.IBinding;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TrainTest {
    static ProjectParser projectParser;

    public static void main(String[] args) throws IOException {
        Config.loadConfig(Config.STORAGE_DIR + "/json/lucene.json");
        projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
                Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);

        //change file path here
        File curFile = new File("/Users/maytinhdibo/Research/java-data/lucene/lucene/src/java/org/apache/lucene/index/SegmentInfos.java");
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
                Utils.visitMethodCallNode(map.get(id));
            }
        }
    }
}
