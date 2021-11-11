package flute.crawling;

import flute.config.Config;
import flute.jdtparser.APITest;
import flute.utils.file_processing.FileProcessor;
import flute.utils.logging.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.util.List;

public class GitCloner {

    public static void cloneRepoFromURL(String repoUrl) {
        try {
            String repoFullName = repoUrl.substring(repoUrl.indexOf("github.com/") + 11);
            repoFullName = repoFullName.replaceAll("/", "_");
            File repoDir = new File(Config.REPO_DIR + "git/" + repoFullName);
            if (repoDir.exists()) return;

            System.out.println("Cloning " + repoUrl + " into " + repoFullName);
            Git result = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(repoDir)
                    .call();

        } catch (GitAPIException e) {
            System.out.println("Exception occurred while cloning repo");
            e.printStackTrace();
        }
    }

    public static void cloneRepo(String repoFullName) {
        cloneRepoFromURL("https://github.com/" + repoFullName);
    }

    public static void main(String args[]) {
        int LIMIT = 1000;
        List<String> repos = FileProcessor.readLineByLineToList("docs/10000_projects.txt");
        int repoCount = 0;
        for (String repo: repos) {
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

                if (APITest.initProject(repo) == 0) {
                    //Logger.write(repo, "10000_projects_built.txt");
                    ++repoCount;
                    if (repoCount >= LIMIT) break;
                }
            } catch (GitAPIException e) {
                System.out.println("Exception occurred while cloning repo");
                e.printStackTrace();
            }
        }
    }

    public static void bulkCloneRepo(List<String> repos) {
        try {
            repos.forEach(repo -> {
                cloneRepoFromURL(repo);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
