package flute.communicate;

import flute.communicate.schema.PredictResponse;
import flute.communicate.schema.Response;
import flute.config.Config;

public class Client {

    public static void main(String[] args) {
        try {
            SocketClient socketClient = new SocketClient(17007);
            Response response = socketClient.write("{\n" +
                    "  \"method_context\": [\n" +
                    "    \"sequence1 sequence1 sequence1 sequence1 sequence1 sequence1 sequence1 sequence1\",\n" +
                    "    \"sequence2\",\n" +
                    "    \"sequence3\"\n" +
                    "  ],\n" +
                    "  \"next_lex\": [\n" +
                    "    \"candidate1\",\n" +
                    "    \"candidate2\",\n" +
                    "    \"candidate3\"\n" +
                    "  ],\n" +
                    "  \"expected_lex\": \"candidate3\"\n" +
                    "}");
            if (response instanceof PredictResponse) {
                PredictResponse predictResponse = (PredictResponse) response;
                System.out.println("==========================");
                System.out.println("Result:");
                predictResponse.getData().ngram.getResult().forEach(item -> {
                    System.out.println(item);
                });
                System.out.println("==========================");
                System.out.println("Runtime: " + predictResponse.getData().ngram.getRuntime() + "s");
            }
            socketClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}