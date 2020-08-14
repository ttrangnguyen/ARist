package flute.crawling;

import flute.config.Config;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.util.List;

public class GitCloner {

    public static void cloneRepo(String repoUrl) {
        try {
            String projectDir = repoUrl.substring(repoUrl.indexOf("github.com/") + 11, repoUrl.indexOf(".git"));
            projectDir = projectDir.replaceAll("/", "_");

            System.out.println("Cloning " + repoUrl + " into " + projectDir);
            Git result = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(new File(Config.REPO_DIR + "git/" + projectDir))
                    .call();

        } catch (GitAPIException e) {
            System.out.println("Exception occurred while cloning repo");
            e.printStackTrace();
        }
    }

    public static void bulkCloneRepo(List<String> repos) {
        try {
            repos.forEach(repo -> {
                cloneRepo(repo);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
