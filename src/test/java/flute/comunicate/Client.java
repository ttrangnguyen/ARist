package flute.comunicate;

import com.google.gson.Gson;
import flute.comunicate.schema.DefaultResponse;
import flute.comunicate.schema.PredictResponse;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        Gson gson= new Gson();

        Socket socketOfClient = null;
        BufferedWriter os = null;
        BufferedReader is = null;

        try {
            socketOfClient = new Socket("localhost", 18005);

            System.out.println("Connected!");
            os = new BufferedWriter(new OutputStreamWriter(socketOfClient.getOutputStream()));
            is = new BufferedReader(new InputStreamReader(socketOfClient.getInputStream()));

        } catch (UnknownHostException e) {
            System.err.println("Don't know about host ");
            return;
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection");
            return;
        }

        Scanner scanner = new Scanner(System.in);
        try {
            while (true) {
                System.out.println("Enter input:");
                String input = scanner.nextLine();
                os.write(input);
                os.newLine();
                os.flush();

                String responseLine;
                responseLine = is.readLine();
                System.out.println(responseLine);

                DefaultResponse defaultResponse = gson.fromJson(responseLine, DefaultResponse.class);

                switch(defaultResponse.getType())
                {
                    case "predict":
                        PredictResponse predictResponse = gson.fromJson(responseLine, PredictResponse.class);
//                        List data = predictResponse.getData();
                        break;
                    default:
                        System.out.println("No match");
                }

                if (input.equals("exit")) break;
            }

            os.close();
            is.close();
            socketOfClient.close();
        } catch (UnknownHostException e) {
            System.err.println("Trying to connect to unknown host: " + e);
        } catch (IOException e) {
            System.err.println("IOException:  " + e);
        }
    }

}