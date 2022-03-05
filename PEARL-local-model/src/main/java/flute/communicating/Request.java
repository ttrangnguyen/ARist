package flute.communicating;

import java.util.List;

public class Request {
    public List<String> lex_context;
    public String excode_context;
    public String filePath;
    public String method_name;
    public String class_name;
    public String expected_excode;
    public String expected_lex;
    public List<String> classHierarchy;
    public String methodInvoc;
    public String methodInvocCaller;
    public Boolean ignored;
    public String argType;
    public Integer test_id;
    public Integer argPos;
    public String param_name;
    public String packageName;
}
