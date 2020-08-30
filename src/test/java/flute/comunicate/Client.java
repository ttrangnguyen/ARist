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
                String testLexical = "categoryDetail = event.getTarget().getName();\n" +
                        "} else {\n" +
                        "categoryString = categoryObject.getClass().getName();\n" +
                        "}\n" +
                        "}\n" +
                        "Log log = getLog(";
                //Log log = getLog(categoryString, categoryDetail);    245th line
                String testExcode = "METHOD{void,messageLogged} OPEN_PART TYPE(BuildEvent) VAR(BuildEvent,event) CLOSE_PART OPBLK STSTM{IF} OPEN_PART VAR(boolean,initialized) CLOSE_PART OPBLK STSTM{EXPR} TYPE(Object) VAR(Object,categoryObject) ASSIGN(ASSIGN) VAR(BuildEvent,event) M_ACCESS(BuildEvent,getTask,0) OPEN_PART CLOSE_PART ENSTM{EXPR} STSTM{EXPR} TYPE(String) VAR(String,categoryString) ASSIGN(ASSIGN) LIT(null) ENSTM{EXPR} STSTM{EXPR} TYPE(String) VAR(String,categoryDetail) ASSIGN(ASSIGN) LIT(null) ENSTM{EXPR} STSTM{IF} OPEN_PART VAR(Object,categoryObject) OP(EQUALS) LIT(null) CLOSE_PART OPBLK STSTM{EXPR} VAR(Object,categoryObject) ASSIGN(ASSIGN) VAR(BuildEvent,event) M_ACCESS(BuildEvent,getTarget,0) OPEN_PART CLOSE_PART ENSTM{EXPR} STSTM{IF} OPEN_PART VAR(Object,categoryObject) OP(EQUALS) LIT(null) CLOSE_PART OPBLK STSTM{EXPR} VAR(Object,categoryObject) ASSIGN(ASSIGN) VAR(BuildEvent,event) M_ACCESS(BuildEvent,getProject,0) OPEN_PART CLOSE_PART ENSTM{EXPR} STSTM{EXPR} VAR(String,categoryString) ASSIGN(ASSIGN) VAR(String,PROJECT_LOG) ENSTM{EXPR} STSTM{EXPR} VAR(String,categoryDetail) ASSIGN(ASSIGN) VAR(BuildEvent,event) M_ACCESS(BuildEvent,getProject,0) OPEN_PART CLOSE_PART M_ACCESS(<unk>,getName,0) OPEN_PART CLOSE_PART ENSTM{EXPR} CLBLK ENSTM{IF} STSTM{ELSE} OPBLK STSTM{EXPR} VAR(String,categoryString) ASSIGN(ASSIGN) VAR(String,TARGET_LOG) ENSTM{EXPR} STSTM{EXPR} VAR(String,categoryDetail) ASSIGN(ASSIGN) VAR(BuildEvent,event) M_ACCESS(BuildEvent,getTarget,0) OPEN_PART CLOSE_PART M_ACCESS(<unk>,getName,0) OPEN_PART CLOSE_PART ENSTM{EXPR} CLBLK ENSTM{ELSE} CLBLK ENSTM{IF} STSTM{ELSE} OPBLK STSTM{IF} OPEN_PART VAR(BuildEvent,event) M_ACCESS(BuildEvent,getTarget,0) OPEN_PART CLOSE_PART OP(NOT_EQUALS) LIT(null) CLOSE_PART OPBLK STSTM{EXPR} VAR(String,categoryString) ASSIGN(ASSIGN) VAR(Object,categoryObject) M_ACCESS(Object,getClass,0) OPEN_PART CLOSE_PART M_ACCESS(<unk>,getName,0) OPEN_PART CLOSE_PART ENSTM{EXPR} STSTM{EXPR} VAR(String,categoryDetail) ASSIGN(ASSIGN) VAR(BuildEvent,event) M_ACCESS(BuildEvent,getTarget,0) OPEN_PART CLOSE_PART M_ACCESS(<unk>,getName,0) OPEN_PART CLOSE_PART ENSTM{EXPR} CLBLK ENSTM{IF} STSTM{ELSE} OPBLK STSTM{EXPR} VAR(String,categoryString) ASSIGN(ASSIGN) VAR(Object,categoryObject) M_ACCESS(Object,getClass,0) OPEN_PART CLOSE_PART M_ACCESS(<unk>,getName,0) OPEN_PART CLOSE_PART ENSTM{EXPR} CLBLK ENSTM{ELSE} CLBLK ENSTM{ELSE} STSTM{EXPR} TYPE(Log) VAR(Log,log) ASSIGN(ASSIGN) M_ACCESS(CommonsLoggingListener,getLog,2) OPEN_PART";
                String excodeTempls = "VAR(LogFactory,logFactory)\n" +
                        "F_ACCESS(String,HASHTABLE_IMPLEMENTATION_PROPERTY) [logFactory.HASHTABLE_IMPLEMENTATION_PROPERTY]\n" +
                        "VAR(LogFactory,logFactory)\n" +
                        "F_ACCESS(String,FACTORY_PROPERTY) [logFactory.FACTORY_PROPERTY]\n" +
                        "VAR(LogFactory,logFactory)\n" +
                        "F_ACCESS(String,DIAGNOSTICS_DEST_PROPERTY) [logFactory.DIAGNOSTICS_DEST_PROPERTY]\n" +
                        "VAR(String,categoryString) [categoryString]\n" +
                        "VAR(String,TARGET_LOG) [TARGET_LOG]\n" +
                        "VAR(String,categoryDetail) [categoryDetail]\n" +
                        "VAR(LogFactory,logFactory)\n" +
                        "F_ACCESS(String,SERVICE_ID) [logFactory.SERVICE_ID]\n" +
                        "VAR(LogFactory,logFactory)\n" +
                        "F_ACCESS(String,TCCL_KEY) [logFactory.TCCL_KEY]\n" +
                        "VAR(LogFactory,logFactory)\n" +
                        "F_ACCESS(String,diagnosticPrefix) [logFactory.diagnosticPrefix]\n" +
                        "VAR(LogFactory,logFactory)\n" +
                        "F_ACCESS(String,PRIORITY_KEY) [logFactory.PRIORITY_KEY]\n" +
                        "VAR(LogFactory,logFactory)\n" +
                        "F_ACCESS(String,FACTORY_DEFAULT) [logFactory.FACTORY_DEFAULT]\n" +
                        "VAR(LogFactory,logFactory)\n" +
                        "F_ACCESS(String,FACTORY_PROPERTIES) [logFactory.FACTORY_PROPERTIES]\n" +
                        "VAR(BuildEvent,event)\n" +
                        "F_ACCESS(String,message) [event.message]\n" +
                        "VAR(LogFactory,logFactory)\n" +
                        "F_ACCESS(String,WEAK_HASHTABLE_CLASSNAME) [logFactory.WEAK_HASHTABLE_CLASSNAME]\n" +
                        "VAR(String,PROJECT_LOG) [PROJECT_LOG]";

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