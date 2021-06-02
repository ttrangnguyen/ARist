package flute.crawling;

import flute.config.Config;
import flute.utils.file_processing.FileProcessor;
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

    public static void main(String args[]) {
        int LIMIT = 1000;
        List<String> repos = FileProcessor.readLineByLineToList("docs/JAVA_repos.txt");
        if (LIMIT != -1) {
            repos = repos.subList(0, LIMIT);
        }
        repos.forEach(repo -> {
            String usr = repo.split("_")[0];
            String name = repo.split("_")[1];
            try {
                repo = repo.replaceAll("/", "_");
                String repoUrl = "https://github.com/" + usr + "/" + name;
                System.out.println("Cloning " + repoUrl + " into " + repo);
                Git result = Git.cloneRepository()
                        .setURI(repoUrl)
                        .setDirectory(new File(Config.REPO_DIR + "git/JAVA_repos/" + repo))
                        .call();

            } catch (GitAPIException e) {
                System.out.println("Exception occurred while cloning repo");
                e.printStackTrace();
            }
        });
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
