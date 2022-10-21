package flute.communicating;

import com.google.gson.Gson;
import flute.config.Config;
import flute.config.ProjectConfig;
import flute.testing.ScoreInfo;
import flute.testing.SingleParamTestManager;
import slp.core.util.Pair;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class SingleParamServer {
    public SingleParamTestManager singleParamTestManager;

    public SingleParamServer() {
        singleParamTestManager = new SingleParamTestManager();
    }

    public void run() {
        ServerSocket listener;
        InputStreamReader is;
        BufferedWriter os;
        Socket clientSocket;

        try {
            listener = new ServerSocket(18007);
            System.out.println("Server is waiting...");
            clientSocket = listener.accept();
            System.out.println("Accept a client!");
            is = new InputStreamReader(clientSocket.getInputStream());
            BufferedReader reader = new BufferedReader(is);

            os = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            while (true) {
                String jsonRequestString = reader.readLine();
                if (jsonRequestString == null) {
                    break;
                }
                String response = response(jsonRequestString);
                os.write(response);
                os.newLine();
                os.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void test(String requestString, MultipleParamRequest jsonRequest) {
        String currentFilePath = ProjectConfig.generatedDataRoot + jsonRequest.filePath;

        singleParamTestManager.resolveTestFilePath(currentFilePath);
        singleParamTestManager.initTestCase(requestString);
        singleParamTestManager.resolveContext();
        singleParamTestManager.resolveParamName();
        singleParamTestManager.resolveParamTypeName();
        singleParamTestManager.resolveRealParam();

        // parse lexical params
        singleParamTestManager.addCandsMultipleParam(jsonRequest.next_excode, jsonRequest.next_lex);
        singleParamTestManager.scoreSequences();
        singleParamTestManager.postProcessing();
    }

    public String response(String request) {
        long start_time_test = System.nanoTime();
        Gson gson = new Gson();
        MultipleParamRequest jsonRequest = gson.fromJson(request, MultipleParamRequest.class);
        test(request, jsonRequest);
        // get top candidates

        String response = "{\"type\":\"predict\", \"data\":{\"ngram\":{\"result\":";

        ArrayList<ScoreInfo> topCands = singleParamTestManager.getTopCands();
        gson.toJson(topCands, ArrayList.class);
        response += gson.toJson(topCands, ArrayList.class);
        response += ",\"runtime\":";
        response += Double.toString((1.0 * System.nanoTime() - start_time_test) / 1000000000);
        response += "}}}";
        return response;
    }

    public static void main(String[] args) {
//        // Config.init(args[0], args[1], args[2], args[3]);
        SingleParamServer singleParamServer = new SingleParamServer();
        singleParamServer.run();
    }
}
