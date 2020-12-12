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
                    "    [\n" +
                    "      \"VAR(LogFactory) F_ACCESS(String,FACTORY_DEFAULT)\",\n" +
                    "      \"VAR(LogFactory) F_ACCESS(String,FACTORY_PROPERTY)\",\n" +
                    "      \"VAR(String)\",\n" +
                    "      \"VAR(LogFactory) F_ACCESS(String,PRIORITY_KEY)\",\n" +
                    "      \"VAR(LogFactory) F_ACCESS(String,FACTORY_PROPERTIES)\",\n" +
                    "      \"VAR(CommonsLoggingListener) F_ACCESS(String,PROJECT_LOG)\",\n" +
                    "      \"VAR(LogFactory) F_ACCESS(String,DIAGNOSTICS_DEST_PROPERTY)\",\n" +
                    "      \"VAR(LogFactory) F_ACCESS(String,HASHTABLE_IMPLEMENTATION_PROPERTY)\",\n" +
                    "      \"VAR(LogFactory) F_ACCESS(String,TCCL_KEY)\",\n" +
                    "      \"VAR(CommonsLoggingListener) F_ACCESS(String,TARGET_LOG)\"\n" +
                    "    ],\n" +
                    "    [\n" +
                    "      \"VAR(LogFactory) F_ACCESS(String,FACTORY_DEFAULT)\",\n" +
                    "      \"VAR(String)\"\n" +
                    "    ],\n" +
                    "    [\n" +
                    "      \"VAR(LogFactory) F_ACCESS(String,FACTORY_DEFAULT)\",\n" +
                    "      \"VAR(String)\"\n" +
                    "    ]\n" +
                    "  ],\n" +
                    "  \"next_lex\": [\n" +
                    "    [\n" +
                    "      [\n" +
                    "        \"logFactory.FACTORY_DEFAULT\"\n" +
                    "      ],\n" +
                    "      [\n" +
                    "        \"logFactory.FACTORY_PROPERTY\"\n" +
                    "      ],\n" +
                    "      [\n" +
                    "        \"categoryDetail\",\n" +
                    "        \"categoryString\",\n" +
                    "        \"TARGET_LOG\",\n" +
                    "        \"PROJECT_LOG\"\n" +
                    "      ],\n" +
                    "      [\n" +
                    "        \"logFactory.PRIORITY_KEY\"\n" +
                    "      ],\n" +
                    "      [\n" +
                    "        \"logFactory.FACTORY_PROPERTIES\"\n" +
                    "      ],\n" +
                    "      [\n" +
                    "        \"this.PROJECT_LOG\"\n" +
                    "      ],\n" +
                    "      [\n" +
                    "        \"logFactory.DIAGNOSTICS_DEST_PROPERTY\"\n" +
                    "      ],\n" +
                    "      [\n" +
                    "        \"logFactory.HASHTABLE_IMPLEMENTATION_PROPERTY\"\n" +
                    "      ],\n" +
                    "      [\n" +
                    "        \"logFactory.TCCL_KEY\"\n" +
                    "      ],\n" +
                    "      [\n" +
                    "        \"this.TARGET_LOG\"\n" +
                    "      ]\n" +
                    "    ],\n" +
                    "    [\n" +
                    "      [\n" +
                    "        \"logFactory.FACTORY_DEFAULT\"\n" +
                    "      ],\n" +
                    "      [\n" +
                    "        \"categoryString\"\n" +
                    "      ]\n" +
                    "    ],\n" +
                    "    [\n" +
                    "      [\n" +
                    "        \"logFactory.FACTORY_PROPERTY\"\n" +
                    "      ],\n" +
                    "      [\n" +
                    "        \"categoryString\"\n" +
                    "      ]\n" +
                    "    ]\n" +
                    "  ],\n" +
                    "  \"method_name\": \"StrongMethod\",\n" +
                    "  \"class_name\": \"StrongClass\",\n" +
                    "  \"expected_excode\": \"VAR(String) SEPA(,) VAR(String) SEPA(,) VAR(String)\",\n" +
                    "  \"expected_lex\": \"categoryString, categoryString, categoryString\"\n" +
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