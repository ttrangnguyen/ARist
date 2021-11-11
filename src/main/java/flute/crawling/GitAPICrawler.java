package flute.crawling;

import com.jcraft.jsch.IO;
import flute.config.Config;
import flute.utils.file_processing.FileProcessor;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class GitAPICrawler {
    public static List<String> getMostStarredRepositories() {
        List<String> repoList = new ArrayList<>();
        int numPage = 10;
        int numItemPerPage = 100;

        HttpClient client = HttpClient.newHttpClient();
        try {
            for (int pageId = 1; pageId <= numPage; ++pageId) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(String.format("https://api.github.com/search/repositories?q=language:java&sort=stars&order=desc&page=%d&per_page=%d", pageId, numItemPerPage)))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JSONObject jsonResponse = new JSONObject(response.body());
                jsonResponse.getJSONArray("items").forEach(item -> {
                    String repoFullName = ((JSONObject) item).getString("full_name");
                    repoList.add(repoFullName);
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return repoList;
    }

    public static void main(String args[]) {
        List<String> repoList = getMostStarredRepositories();
//        try {
//            FileProcessor.writeListLineByLine(repoList, Config.LOG_DIR + "most_starred_repos.txt");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        for (String repo: repoList) {
            GitCloner.cloneRepo(repo);
        }
    }
}
