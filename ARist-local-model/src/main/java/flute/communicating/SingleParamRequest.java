package flute.communicating;

import java.util.List;

public class SingleParamRequest extends Request {
    public List<List<Boolean>> is_local_var;
    public List<List<Integer>> candidates_last_usage_distance;
    public List<List<Integer>> candidates_scope_distance;
    public List<String> next_excode;
    public List<List<String>> next_lex;
    public String methodInvocClassQualifiedName;
    public String slp_first_token;
    public String paramTypeKey;
    public String paramTypeName;
}
