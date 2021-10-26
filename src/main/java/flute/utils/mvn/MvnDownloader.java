package flute.utils.mvn;

import flute.config.Config;
import flute.utils.file_processing.DirProcessor;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.shared.invoker.*;
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
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class MvnDownloader {
    public static void main(String[] args) throws IOException, XmlPullParserException {
        File repoDir = new File("../../Kien/Flute-Kien-full/storage/repositories/git/four_hundred_excluded");
        for (File project: repoDir.listFiles()) {
            MvnDownloader.download(project.getAbsolutePath());
        }
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

    /**
     * Download jar from pom.xml
     * @return 0 if successfully download all dependencies.
     */
    public static int download(String projectPath) {
        AtomicInteger exitCode = new AtomicInteger();

        DirProcessor.getAllEntity(new File(projectPath), false).stream().filter(file -> {
            return file.getAbsolutePath().replace('\\','/').endsWith("/pom.xml");
        }).forEach(pomFile -> {
            InvocationRequest request = new DefaultInvocationRequest();
            request.setPomFile(pomFile);
            System.out.println("=======MAVEN IS BUILDING=======");
            request.setOutputHandler(new InvocationOutputHandler() {
                @Override
                public void consumeLine(String s) {
                    //do nothing
                }
            });
            request.setGoals(Arrays.asList("dependency:copy-dependencies", "-DoutputDirectory=\"" +
                    Paths.get(pomFile.getParentFile().getAbsolutePath(), "flute-mvn")
                    + "\""));

            Invoker invoker = new DefaultInvoker();
            invoker.setMavenHome(new File(Config.MVN_HOME));
            invoker.setInputStream(InputStream.nullInputStream());
            try {
                int statusCode = invoker.execute(request).getExitCode();
                if (statusCode > exitCode.get()) {
                    exitCode.set(statusCode);
                }
            } catch (MavenInvocationException e) {
                e.printStackTrace();
            }
        });
        return exitCode.get();
    }
}
