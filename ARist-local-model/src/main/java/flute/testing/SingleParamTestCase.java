package flute.testing;

import com.google.gson.Gson;
import flute.candidate.Candidate;
import flute.communicating.SingleParamRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class SingleParamTestCase extends TestCase {
    public String requestString;
    public List<Integer> lexContext;
    public List<Integer> excodeContext;
    public HashMap<String, HashSet<Candidate>> lexicalCands;
    public HashSet<String> excodeCands;
    public List<String> paramNameIndices;
    public List<String> paramTypeNameIndices;
    public List<ScoreInfo> scores;
    public List<Integer> realLexicalParamIndices;
    public List<Integer> realExcodeParamIndices;
    public SingleParamRequest request;
    public String normalizedExpectedLex;
    public Integer numberOfCands;
    public Double psCheckTime;
    public Double rankingTime;
    public Integer totalPsCands;
    public Integer reducedPsCands;

    public SingleParamTestCase(String request) {
        requestString = request;
        lexContext = new ArrayList<>();
        excodeContext = new ArrayList<>();
        lexicalCands = new HashMap<>();
        excodeCands = new HashSet<>();
        paramNameIndices = new ArrayList<>();
        scores = new ArrayList<>();
        realLexicalParamIndices = new ArrayList<>();
        realExcodeParamIndices = new ArrayList<>();
        Gson gson = new Gson();
        this.request = gson.fromJson(request, SingleParamRequest.class);
        normalizedExpectedLex = SingleParamTester.getNormalizedAnswer(request);
    }
}
