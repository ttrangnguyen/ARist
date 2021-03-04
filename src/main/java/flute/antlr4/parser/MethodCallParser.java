package flute.antlr4.parser;

import flute.config.Config;
import flute.jdtparser.FileParser;
import flute.jdtparser.ProjectParser;
import flute.jdtparser.callsequence.FileNode;
import flute.jdtparser.callsequence.MethodCallNode;
import flute.jdtparser.callsequence.node.cfg.MinimalNode;
import flute.jdtparser.callsequence.node.cfg.Utils;
import flute.utils.file_processing.FileProcessor;
import flute.utils.logging.Logger;
import org.eclipse.jdt.core.dom.IBinding;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class MethodCallParser {
    static ProjectParser projectParser;
    static int n_folds = 10;

    public static List<String> fileList;

    public static void readFileListFromTxt(String path) {
        fileList = FileProcessor.readLineByLineToList(path);
    }

    public void removeDuplicatesFile() throws IOException {
        String projectName = "eclipse";
        ArrayList<List<String>> folds = new ArrayList<>();
        ArrayList<List<String>> toDel = new ArrayList<>();
        for (int i = 0; i < 10; ++i) {
            toDel.add(new ArrayList<>());
        }

        for (int fold = 0; fold < n_folds; ++fold) {
            folds.add(FileProcessor.readLineByLineToList("../data_v3/datapath/fold_" + fold + "/" + projectName + ".txt"));
        }
        for (int fold = 0; fold < n_folds; ++fold) {
            for(int fold2 = fold+1; fold2 < n_folds; ++fold2) {
                for (String pat : folds.get(fold)) {
                    for (String papa : folds.get(fold2)) {
                        if (pat.equals(papa)) {
                            toDel.get(fold2).add(pat);
                            System.out.println(fold + " " + fold2 + " " + pat);
                            FileProcessor.deleteFile(new File(Config.LOG_DIR + "/" + projectName + "/fold" + fold2 + "/" + pat.substring(pat.lastIndexOf('\\')) + ".txt"));
                        }
                    }
                }
            }
        }
        for (int fold = 0; fold < n_folds; ++fold) {
            for (String del : toDel.get(fold)) {
                folds.get(fold).remove(del);
            }
            FileProcessor.writeListLineByLine(folds.get(fold), "../data_v3/datapath/fold_" + fold + "/" + projectName + ".txt");
        }
    }

    public static void main(String[] args) throws IOException {
        String projectName = "eclipse";

        Config.loadConfig(Config.STORAGE_DIR + "/json/" + projectName + ".json");
        projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
                Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);
        boolean gogogo = false;
        for (int fold = 0; fold < n_folds; ++fold) {
            readFileListFromTxt("../data_v3/datapath/fold_" + fold + "/" + projectName + ".txt");

            File directory = new File(Config.LOG_DIR + "/" + projectName + "/fold" + fold + "/");
            if (!directory.exists()) {
                Files.createDirectories(Paths.get(directory.getAbsolutePath()));
            }
            // eclipse\eclipse.pde.ui\apitools\org.eclipse.pde.api.tools\src\org\eclipse\pde\api\tools\internal\comparator\ClassFileComparator.java
            // eclipse\rt.equinox.framework\bundles\org.eclipse.osgi\container\src\org\eclipse\osgi\internal\framework\EquinoxBundle.java
            // eclipse\eclipse.pde.ui\ ui\org.eclipse.pde.ui\src\org\eclipse\pde\internal\ ui\PDELabelProvider.java
            // eclipse\eclipse.pde.ui\apitools\org.eclipse.pde.api.tools\src\org\eclipse\pde\api\tools\internal\builder\Reference.java
            // eclipse\eclipse.platform.ui.tools\bundles\org.eclipse.e4.tools.emf.ui\src\org\eclipse\e4\tools\emf\ ui\internal\common\component\UnsettableUpdateValueStrategy.java
            // eclipse\eclipse.platform.ui\bundles\org.eclipse.jface.databinding\src\org\eclipse\jface\internal\databinding\swt\WidgetTextProperty.java
            // eclipse\eclipse.platform.text\org.eclipse.ui.workbench.texteditor\src\org\eclipse\ ui\texteditor\SourceViewerDecorationSupport.java
            // 2 3 4 5?
            for (String filePath : fileList) {
                if (!(filePath.equals("eclipse\\eclipse.platform.ui\\bundles\\org.eclipse.jface.databinding\\src\\org\\eclipse\\jface\\internal\\databinding\\swt\\WidgetTextProperty.java")
                || filePath.equals("eclipse\\eclipse.platform.text\\org.eclipse.ui.workbench.texteditor\\src\\org\\eclipse\\ui\\texteditor\\SourceViewerDecorationSupport.java"))){
                    continue;
                }
//                if (filePath.equals("eclipse\\eclipse.pde.ui\\apitools\\org.eclipse.pde.api.tools\\src\\org\\eclipse\\pde\\api\\tools\\internal\\comparator\\ClassFileComparator.java")) {
//                    gogogo = true;
//                    continue;
//                }
//                if (!gogogo) continue;
                System.out.println(filePath);
                File curFile = new File(flute.antlr4.config.Config.projectsPath + filePath);
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
                        if (Config.TEST_APIS != null && Config.TEST_APIS.length > 0) {
                            String orgPackage = Utils.getOrgPackage(id);
                            if (!Utils.checkTargetAPI(orgPackage)) {
                                continue;
                            }
                        }
                        List<List<String>> sequences = Utils.visitMethodCallNode(map.get(id));
                        for (List<String> sequence : sequences) {
                            Logger.write(String.join(" ", sequence), projectName + "/fold" + fold + "/" + curFile.getName() + ".txt");
                        }
                    }
                }
            }
        }
    }
}