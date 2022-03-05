package flute.communicating;

import java.util.List;

public class PredictionDetail {
    public List<String> predictions;
    public List<Double> lexModelScores;
    public List<Double> lexSimScores;
    public List<Integer> defRecentness;
    public List<Integer> useRecentness;
    public String answer;
    public Double runtime;
    public Integer test_id;
    public Double ranking_time;
    public Double ps_time;
    public Integer n_cands;
}

