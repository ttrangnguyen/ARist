package flute.communicate;

import com.google.gson.Gson;
import flute.communicate.schema.DefaultResponse;
import flute.communicate.schema.PredictResponse;
import flute.communicate.schema.Response;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        try {
            SocketClient socketClient = new SocketClient(18007);
            Response response = socketClient.write("{\"lex_context\":[\"categoryDetail\",\"=\",\"event.getTarget\",\"(\",\")\",\".\",\"getName\",\"(\",\")\",\";\",\"else\",\"{\",\"categoryString\",\"=\",\"categoryObject.getClass\",\"(\",\")\",\".\",\"getName\",\"(\",\")\",\";\",\"}\",\"}\",\"Log\",\"log\",\"=\",\"getLog\",\"(\"],\"excode_context\":\"METHOD{void,messageLogged} OPEN_PART TYPE(BuildEvent) VAR(BuildEvent,event) CLOSE_PART OPBLK STSTM{IF} OPEN_PART VAR(boolean,initialized) CLOSE_PART OPBLK STSTM{EXPR} TYPE(Object) VAR(Object,categoryObject) ASSIGN(ASSIGN) VAR(BuildEvent,event) M_ACCESS(BuildEvent,getTask,0) OPEN_PART CLOSE_PART ENSTM{EXPR} STSTM{EXPR} TYPE(String) VAR(String,categoryString) ASSIGN(ASSIGN) LIT(null) ENSTM{EXPR} STSTM{EXPR} TYPE(String) VAR(String,categoryDetail) ASSIGN(ASSIGN) LIT(null) ENSTM{EXPR} STSTM{IF} OPEN_PART VAR(Object,categoryObject) OP(EQUALS) LIT(null) CLOSE_PART OPBLK STSTM{EXPR} VAR(Object,categoryObject) ASSIGN(ASSIGN) VAR(BuildEvent,event) M_ACCESS(BuildEvent,getTarget,0) OPEN_PART CLOSE_PART ENSTM{EXPR} STSTM{IF} OPEN_PART VAR(Object,categoryObject) OP(EQUALS) LIT(null) CLOSE_PART OPBLK STSTM{EXPR} VAR(Object,categoryObject) ASSIGN(ASSIGN) VAR(BuildEvent,event) M_ACCESS(BuildEvent,getProject,0) OPEN_PART CLOSE_PART ENSTM{EXPR} STSTM{EXPR} VAR(String,categoryString) ASSIGN(ASSIGN) VAR(String,PROJECT_LOG) ENSTM{EXPR} STSTM{EXPR} VAR(String,categoryDetail) ASSIGN(ASSIGN) VAR(BuildEvent,event) M_ACCESS(BuildEvent,getProject,0) OPEN_PART CLOSE_PART M_ACCESS(<unk>,getName,0) OPEN_PART CLOSE_PART ENSTM{EXPR} CLBLK ENSTM{IF} STSTM{ELSE} OPBLK STSTM{EXPR} VAR(String,categoryString) ASSIGN(ASSIGN) VAR(String,TARGET_LOG) ENSTM{EXPR} STSTM{EXPR} VAR(String,categoryDetail) ASSIGN(ASSIGN) VAR(BuildEvent,event) M_ACCESS(BuildEvent,getTarget,0) OPEN_PART CLOSE_PART M_ACCESS(<unk>,getName,0) OPEN_PART CLOSE_PART ENSTM{EXPR} CLBLK ENSTM{ELSE} CLBLK ENSTM{IF} STSTM{ELSE} OPBLK STSTM{IF} OPEN_PART VAR(BuildEvent,event) M_ACCESS(BuildEvent,getTarget,0) OPEN_PART CLOSE_PART OP(NOT_EQUALS) LIT(null) CLOSE_PART OPBLK STSTM{EXPR} VAR(String,categoryString) ASSIGN(ASSIGN) VAR(Object,categoryObject) M_ACCESS(Object,getClass,0) OPEN_PART CLOSE_PART M_ACCESS(<unk>,getName,0) OPEN_PART CLOSE_PART ENSTM{EXPR} STSTM{EXPR} VAR(String,categoryDetail) ASSIGN(ASSIGN) VAR(BuildEvent,event) M_ACCESS(BuildEvent,getTarget,0) OPEN_PART CLOSE_PART M_ACCESS(<unk>,getName,0) OPEN_PART CLOSE_PART ENSTM{EXPR} CLBLK ENSTM{IF} STSTM{ELSE} OPBLK STSTM{EXPR} VAR(String,categoryString) ASSIGN(ASSIGN) VAR(Object,categoryObject) M_ACCESS(Object,getClass,0) OPEN_PART CLOSE_PART M_ACCESS(<unk>,getName,0) OPEN_PART CLOSE_PART ENSTM{EXPR} CLBLK ENSTM{ELSE} CLBLK ENSTM{ELSE} STSTM{EXPR} TYPE(Log) VAR(Log,log) ASSIGN(ASSIGN) M_ACCESS(CommonsLoggingListener,getLog,2) OPEN_PART\",\"next_excode\":[\"VAR(LogFactory,logFactory) F_ACCESS(String,HASHTABLE_IMPLEMENTATION_PROPERTY)\",\"VAR(LogFactory,logFactory) F_ACCESS(String,FACTORY_PROPERTY)\",\"VAR(LogFactory,logFactory) F_ACCESS(String,DIAGNOSTICS_DEST_PROPERTY)\",\"VAR(String,categoryString)\",\"VAR(String,TARGET_LOG)\",\"VAR(String,categoryDetail)\",\"VAR(LogFactory,logFactory) F_ACCESS(String,SERVICE_ID)\",\"VAR(LogFactory,logFactory) F_ACCESS(String,TCCL_KEY)\",\"VAR(LogFactory,logFactory) F_ACCESS(String,diagnosticPrefix)\",\"VAR(LogFactory,logFactory) F_ACCESS(String,PRIORITY_KEY)\",\"VAR(LogFactory,logFactory) F_ACCESS(String,FACTORY_DEFAULT)\",\"VAR(LogFactory,logFactory) F_ACCESS(String,FACTORY_PROPERTIES)\",\"VAR(BuildEvent,event) F_ACCESS(String,message)\",\"VAR(LogFactory,logFactory) F_ACCESS(String,WEAK_HASHTABLE_CLASSNAME)\",\"VAR(String,PROJECT_LOG)\"],\"next_lex\":[\"logFactory.HASHTABLE_IMPLEMENTATION_PROPERTY\",\"logFactory.FACTORY_PROPERTY\",\"logFactory.DIAGNOSTICS_DEST_PROPERTY\",\"categoryString\",\"TARGET_LOG\",\"categoryDetail\",\"logFactory.SERVICE_ID\",\"logFactory.TCCL_KEY\",\"logFactory.diagnosticPrefix\",\"logFactory.PRIORITY_KEY\",\"logFactory.FACTORY_DEFAULT\",\"logFactory.FACTORY_PROPERTIES\",\"event.message\",\"logFactory.WEAK_HASHTABLE_CLASSNAME\",\"PROJECT_LOG\"]}");
            socketClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}