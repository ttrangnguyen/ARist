package flute.testing;

import flute.candidate.Candidate;

public class ScoreInfo {
    public ScoreInfo() {
        totalScore = 0.0;
        lexModelScore = 0.0;
        lexSimScore = 0.0;
    }

    public Candidate candidate;
    public Double totalScore;
    public Double lexModelScore;
    public Double lexSimScore;
    public Integer defRecentness;
    public Integer useRecentness;
}
