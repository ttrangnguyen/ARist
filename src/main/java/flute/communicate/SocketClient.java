package flute.communicate;

import com.google.gson.Gson;
import flute.communicate.schema.DefaultResponse;
import flute.communicate.schema.PredictResponse;
import flute.communicate.schema.Response;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

public class SocketClient {
    BufferedReader is = null;
    DataOutputStream os = null;
    Socket socketOfClient = null;

    Gson gson = new Gson();

    public SocketClient(int PORT) throws Exception {
        try {
            socketOfClient = new Socket("localhost", PORT);

            //os = new BufferedWriter(new OutputStreamWriter(socketOfClient.getOutputStream()));
            os = new DataOutputStream(socketOfClient.getOutputStream());
            is = new BufferedReader(new InputStreamReader(socketOfClient.getInputStream()));

        } catch (UnknownHostException e) {
            throw new Exception("Don't know about host ");
        } catch (IOException e) {
            throw new Exception("Couldn't get I/O for the connection");
        }
    }

    public Response write(String request) throws IOException {
        os.write(request.replaceAll("\\r|\\n", "").getBytes("UTF-8"));
        //os.newLine();
        os.flush();

        String responseLine = is.readLine();

        Response response = gson.fromJson(responseLine, Response.class);

        switch (response.getType()) {
            case "predict":
                response = gson.fromJson(responseLine, PredictResponse.class);
                break;
            default:
                System.out.println("No match");
        }

        return response;
    }

    public void close() throws IOException {
        os.close();
        is.close();
        socketOfClient.close();
    }


}
