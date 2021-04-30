package flute.utils.mvn;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.bouncycastle.math.raw.Mod;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;

public class MvnDownloader {
    public static void main(String[] args) throws IOException, XmlPullParserException {
        System.out.println(
                MvnDownloader.download("/Users/maytinhdibo/Project/Flute", "/Users/maytinhdibo/Project/Flute/pom.xml")
                        .getAbsolutePath()
        );
    }

    public static File download(String projectPath, String pomPath) throws IOException, XmlPullParserException {
        System.out.println("=========DOWNLOADER START=========");
        MavenXpp3Reader reader = new MavenXpp3Reader();

        Model model = reader.read(new FileReader(pomPath));

        model.getDependencies().forEach(dependency -> {
            String version = getVersion(dependency, model);
            String jarUrl = "https://repo1.maven.org/maven2/" +
                    dependency.getGroupId().replace(".", "/")
                    + "/" + dependency.getArtifactId() + "/" + version + "/" + dependency.getArtifactId() + "-" + version + ".jar";
            String jarName = dependency.getArtifactId() + "-" + version + ".jar";
            try {
                FileUtils.copyURLToFile(
                        new URL(jarUrl),
                        new File(projectPath + "/flute-mvn/" + jarName));
                System.out.println("Flute Maven: Downloaded");
            } catch (IOException e) {
                System.out.println("WARNING: Download is failed " + jarUrl);
            }
        });
        return new File(projectPath + "/flute-mvn");
    }

    private static String getVersion(Dependency dependency, Model model) {
        if (dependency.getVersion() != null) {
            if (dependency.getVersion().startsWith("${") && dependency.getVersion().endsWith("}")) {
                return model.getProperties().getProperty(
                        dependency.getVersion().substring(2, dependency.getVersion().length() - 1)
                );
            }
            return dependency.getVersion();
        }
        String metaUrl = "https://repo1.maven.org/maven2/" +
                dependency.getGroupId().replace(".", "/")
                + "/" + dependency.getArtifactId() + "/maven-metadata.xml";
        try {
            URL url = new URL(metaUrl);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(url.openStream());

            Node release = null;

            Node version = findNode("versioning", doc.getDocumentElement());
            if (version != null) {
                release = findNode("release", version);
                if (release != null) return release.getTextContent();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Node findNode(String name, Node parentNode) {
        for (int i = 0; i < parentNode.getChildNodes().getLength(); i++) {
            if (parentNode.getChildNodes().item(i).getNodeName().equals(name)) {
                return parentNode.getChildNodes().item(i);
            }
        }
        return null;
    }
}
