package flute.communicating;

import java.util.List;

public class MultipleParamRequest extends Request{
    public List<List<String>> next_excode;
    public List<List<List<String>>> next_lex;
    public List<String> param_list;

    public String excode_context_no_method;
    public List<String> method_candidate_excode;
}
