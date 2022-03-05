package flute.testing;

import com.google.gson.Gson;
import flute.communicating.PredictionDetail;
import flute.config.TestConfig;

import java.io.*;

public class CugLMTester extends SingleParamTester {
    @Override
    public void run() {
        try {
            File fout = new File(TestConfig.predictionDetailPath);
            fout.getParentFile().mkdirs();
//            if (fout.exists()) {
//                System.out.println("Already tested");
//                return;
//            }
            FileOutputStream fos = new FileOutputStream(fout);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            BufferedReader br = new BufferedReader(new FileReader(TestConfig.testCasesFilePath));
            String request;
            Gson gson = new Gson();
//            int cnt = 0;
            while ((request = br.readLine()) != null) {
                PredictionDetail result = getTopCands(request);
//                if (ModelConfig.USE_BEAM_SEARCH && ModelConfig.USE_DYNAMIC && !result.answer.equals(")")) {
//                    ++cnt;
//                }
//                if (cnt == 1000) break;
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
