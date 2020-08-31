package flute.communicate;

import flute.communicate.schema.PredictResponse;
import flute.communicate.schema.Response;

public class Client {

    public static void main(String[] args) {
        try {
            SocketClient socketClient = new SocketClient(18007);
            Response response = socketClient.write("{\n" +
                    "  \"lex_context\": [\n" +
                    "    \"categoryDetail\",\n" +
                    "    \"=\",\n" +
                    "    \"event.getTarget\",\n" +
                    "    \"(\",\n" +
                    "    \")\",\n" +
                    "    \".\",\n" +
                    "    \"getName\",\n" +
                    "    \"(\",\n" +
                    "    \")\",\n" +
                    "    \";\",\n" +
                    "    \"else\",\n" +
                    "    \"{\",\n" +
                    "    \"categoryString\",\n" +
                    "    \"=\",\n" +
                    "    \"categoryObject.getClass\",\n" +
                    "    \"(\",\n" +
                    "    \")\",\n" +
                    "    \".\",\n" +
                    "    \"getName\",\n" +
                    "    \"(\",\n" +
                    "    \")\",\n" +
                    "    \";\",\n" +
                    "    \"}\",\n" +
                    "    \"}\",\n" +
                    "    \"Log\",\n" +
                    "    \"log\",\n" +
                    "    \"=\",\n" +
                    "    \"getLog\",\n" +
                    "    \"(\"\n" +
                    "  ],\n" +
                    "  \"excode_context\": \"METHOD{void,messageLogged} OPEN_PART TYPE(BuildEvent) VAR(BuildEvent,event) CLOSE_PART OPBLK STSTM{IF} OPEN_PART VAR(boolean,initialized) CLOSE_PART OPBLK STSTM{EXPR} TYPE(Object) VAR(Object,categoryObject) ASSIGN(ASSIGN) VAR(BuildEvent,event) M_ACCESS(BuildEvent,getTask,0) OPEN_PART CLOSE_PART ENSTM{EXPR} STSTM{EXPR} TYPE(String) VAR(String,categoryString) ASSIGN(ASSIGN) LIT(null) ENSTM{EXPR} STSTM{EXPR} TYPE(String) VAR(String,categoryDetail) ASSIGN(ASSIGN) LIT(null) ENSTM{EXPR} STSTM{IF} OPEN_PART VAR(Object,categoryObject) OP(EQUALS) LIT(null) CLOSE_PART OPBLK STSTM{EXPR} VAR(Object,categoryObject) ASSIGN(ASSIGN) VAR(BuildEvent,event) M_ACCESS(BuildEvent,getTarget,0) OPEN_PART CLOSE_PART ENSTM{EXPR} STSTM{IF} OPEN_PART VAR(Object,categoryObject) OP(EQUALS) LIT(null) CLOSE_PART OPBLK STSTM{EXPR} VAR(Object,categoryObject) ASSIGN(ASSIGN) VAR(BuildEvent,event) M_ACCESS(BuildEvent,getProject,0) OPEN_PART CLOSE_PART ENSTM{EXPR} STSTM{EXPR} VAR(String,categoryString) ASSIGN(ASSIGN) VAR(String,PROJECT_LOG) ENSTM{EXPR} STSTM{EXPR} VAR(String,categoryDetail) ASSIGN(ASSIGN) VAR(BuildEvent,event) M_ACCESS(BuildEvent,getProject,0) OPEN_PART CLOSE_PART M_ACCESS(<unk>,getName,0) OPEN_PART CLOSE_PART ENSTM{EXPR} CLBLK ENSTM{IF} STSTM{ELSE} OPBLK STSTM{EXPR} VAR(String,categoryString) ASSIGN(ASSIGN) VAR(String,TARGET_LOG) ENSTM{EXPR} STSTM{EXPR} VAR(String,categoryDetail) ASSIGN(ASSIGN) VAR(BuildEvent,event) M_ACCESS(BuildEvent,getTarget,0) OPEN_PART CLOSE_PART M_ACCESS(<unk>,getName,0) OPEN_PART CLOSE_PART ENSTM{EXPR} CLBLK ENSTM{ELSE} CLBLK ENSTM{IF} STSTM{ELSE} OPBLK STSTM{IF} OPEN_PART VAR(BuildEvent,event) M_ACCESS(BuildEvent,getTarget,0) OPEN_PART CLOSE_PART OP(NOT_EQUALS) LIT(null) CLOSE_PART OPBLK STSTM{EXPR} VAR(String,categoryString) ASSIGN(ASSIGN) VAR(Object,categoryObject) M_ACCESS(Object,getClass,0) OPEN_PART CLOSE_PART M_ACCESS(<unk>,getName,0) OPEN_PART CLOSE_PART ENSTM{EXPR} STSTM{EXPR} VAR(String,categoryDetail) ASSIGN(ASSIGN) VAR(BuildEvent,event) M_ACCESS(BuildEvent,getTarget,0) OPEN_PART CLOSE_PART M_ACCESS(<unk>,getName,0) OPEN_PART CLOSE_PART ENSTM{EXPR} CLBLK ENSTM{IF} STSTM{ELSE} OPBLK STSTM{EXPR} VAR(String,categoryString) ASSIGN(ASSIGN) VAR(Object,categoryObject) M_ACCESS(Object,getClass,0) OPEN_PART CLOSE_PART M_ACCESS(<unk>,getName,0) OPEN_PART CLOSE_PART ENSTM{EXPR} CLBLK ENSTM{ELSE} CLBLK ENSTM{ELSE} STSTM{EXPR} TYPE(Log) VAR(Log,log) ASSIGN(ASSIGN) M_ACCESS(CommonsLoggingListener,getLog,2) OPEN_PART\",\n" +
                    "  \"next_excode\": [\n" +
                    "    \"VAR(LogFactory,logFactory) F_ACCESS(String,HASHTABLE_IMPLEMENTATION_PROPERTY)\",\n" +
                    "    \"VAR(LogFactory,logFactory) F_ACCESS(String,FACTORY_PROPERTY)\",\n" +
                    "    \"VAR(LogFactory,logFactory) F_ACCESS(String,DIAGNOSTICS_DEST_PROPERTY)\",\n" +
                    "    \"VAR(String,categoryString)\",\n" +
                    "    \"VAR(String,TARGET_LOG)\",\n" +
                    "    \"VAR(String,categoryDetail)\",\n" +
                    "    \"VAR(LogFactory,logFactory) F_ACCESS(String,SERVICE_ID)\",\n" +
                    "    \"VAR(LogFactory,logFactory) F_ACCESS(String,TCCL_KEY)\",\n" +
                    "    \"VAR(LogFactory,logFactory) F_ACCESS(String,diagnosticPrefix)\",\n" +
                    "    \"VAR(LogFactory,logFactory) F_ACCESS(String,PRIORITY_KEY)\",\n" +
                    "    \"VAR(LogFactory,logFactory) F_ACCESS(String,FACTORY_DEFAULT)\",\n" +
                    "    \"VAR(LogFactory,logFactory) F_ACCESS(String,FACTORY_PROPERTIES)\",\n" +
                    "    \"VAR(BuildEvent,event) F_ACCESS(String,message)\",\n" +
                    "    \"VAR(LogFactory,logFactory) F_ACCESS(String,WEAK_HASHTABLE_CLASSNAME)\",\n" +
                    "    \"VAR(String,PROJECT_LOG)\"\n" +
                    "  ],\n" +
                    "  \"next_lex\": [\n" +
                    "    \"logFactory.HASHTABLE_IMPLEMENTATION_PROPERTY\",\n" +
                    "    \"logFactory.FACTORY_PROPERTY\",\n" +
                    "    \"logFactory.DIAGNOSTICS_DEST_PROPERTY\",\n" +
                    "    \"categoryString\",\n" +
                    "    \"TARGET_LOG\",\n" +
                    "    \"categoryDetail\",\n" +
                    "    \"logFactory.SERVICE_ID\",\n" +
                    "    \"logFactory.TCCL_KEY\",\n" +
                    "    \"logFactory.diagnosticPrefix\",\n" +
                    "    \"logFactory.PRIORITY_KEY\",\n" +
                    "    \"logFactory.FACTORY_DEFAULT\",\n" +
                    "    \"logFactory.FACTORY_PROPERTIES\",\n" +
                    "    \"event.message\",\n" +
                    "    \"logFactory.WEAK_HASHTABLE_CLASSNAME\",\n" +
                    "    \"PROJECT_LOG\"\n" +
                    "  ]\n" +
                    "}");
            if (response instanceof PredictResponse) {
                PredictResponse predictResponse = (PredictResponse) response;
                System.out.println("==========================");
                System.out.println("Result:");
                predictResponse.getData().forEach(item -> {
                    System.out.println(item);
                });
                System.out.println("==========================");
                System.out.println("Runtime: " + predictResponse.getRuntime() + "s");
            }
            socketClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}