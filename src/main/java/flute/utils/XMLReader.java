package flute.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class XMLReader {
    public static Node findNode(Node node, String name) {
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            if (node.getChildNodes().item(i).getNodeName().equals(name))
                return node.getChildNodes().item(i);
        }
        return null;
    }

    public static List<String> read(File file) {
        List<String> result = new ArrayList<>();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);
            doc.getDocumentElement().normalize();

            NodeList dependencies = doc.getDocumentElement().getElementsByTagName("dependency");
            for (int i = 0; i < dependencies.getLength(); i++) {
                Node dependency = dependencies.item(i);
                Node codeBasePackage = findNode(dependency, "code-name-base");
                if (codeBasePackage != null) {
                    result.add(codeBasePackage.getTextContent());
                }
            }
        } catch (Exception e) {

        }
        return result;
    }

    public static String parseConfigPath(File javaFile) {
        String configPath = null;
        List<String> paths = Arrays.stream(
                javaFile.getAbsolutePath().replace("\\", "/").split("/")
        ).collect(Collectors.toList());

        int indexIDE = paths.indexOf("ide");
        if (indexIDE != -1) {
            configPath = String.join("/", paths.subList(0, indexIDE + 2)) + "/nbproject/project.xml";
        }
        return configPath;
    }

    public static void main(String[] args) {
        String fileConfig = parseConfigPath(new File("/Users/maytinhdibo/Research/java-data/netbeans/ide/api.java.classpath/src/org/netbeans/api/java/classpath/ClassPath.java"));
        List<String> result = read(new File(fileConfig));
    }
}
