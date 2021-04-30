package flute.utils.mvn;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

public class MvnDownloader {
    public static void main(String[] args) throws IOException, XmlPullParserException {
        System.out.println(
                MvnDownloader.download("/Users/maytinhdibo/Project/Flute", "/Users/maytinhdibo/Project/Flute/pom.xml")
                        .getAbsolutePath()
        );
    }

    public static File download(String projectPath, String pomPath) throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = null;

        model = reader.read(new FileReader(pomPath));

        model.getDependencies().forEach(dependency -> {
            String jarUrl = "https://repo1.maven.org/maven2/" +
                    dependency.getGroupId().replace(".", "/")
                    + "/" + dependency.getArtifactId() + "/" + dependency.getVersion() + "/" + dependency.getArtifactId() + "-" + dependency.getVersion() + ".jar";
            String jarName = dependency.getArtifactId() + "-" + dependency.getVersion() + ".jar";
            try {
                FileUtils.copyURLToFile(
                        new URL(jarUrl),
                        new File(projectPath + "/flute-mvn/" + jarName));
                System.out.println("Flute Maven: Downloaded");
            } catch (IOException e) {
                System.out.println("WARNING: Failed for download");
            }
        });
        return new File(projectPath + "/flute-mvn");
    }

}
