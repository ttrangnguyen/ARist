package flute.testing;

import com.google.gson.Gson;
import flute.communicating.PredictionDetail;
import flute.communicating.SingleParamRequest;
import flute.config.ModelConfig;
import flute.config.ProjectConfig;
import flute.config.TestConfig;

import java.io.*;
import java.nio.file.Paths;

public class CugLMMaintenanceTester extends SingleParamTester {
    @Override
    public void run() {
        try {
            File fout = new File(TestConfig.predictionDetailPath);
            fout.getParentFile().mkdirs();
            if (fout.exists()) {
                System.out.println("Already tested");
                return;
            }
            FileOutputStream fos = new FileOutputStream(fout);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            BufferedReader br = new BufferedReader(new FileReader(TestConfig.testCasesFilePath));
            String request;
            Gson gson = new Gson();
            while ((request = br.readLine()) != null) {
                SingleParamRequest jsonRequest = gson.fromJson(request, SingleParamRequest.class);
                String filePath = Paths.get(ProjectConfig.cugLMAllProjectsPath, ProjectConfig.project, jsonRequest.filePath).toString();
                if (!TestFilesManager.isTestFile(new File(filePath))) continue;
                System.out.println(filePath);
                PredictionDetail result = getTopCands(request);
                bw.write(gson.toJson(result));
                bw.newLine();
            }
            br.close();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
