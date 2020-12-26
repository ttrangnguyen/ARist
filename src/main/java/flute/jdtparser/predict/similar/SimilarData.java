package flute.jdtparser.predict.similar;

import java.util.List;

public class SimilarData {
    private String expectedOutput;
    private float expectedOutputSimilarly;
    private String argName;
    private List<String> candidates;
    private List<Float> candidatesSimilarly;

    public float getExpectedOutputSimilarly() {
        return expectedOutputSimilarly;
    }

    public void setExpectedOutputSimilarly(float expectedOutputSimilarly) {
        this.expectedOutputSimilarly = expectedOutputSimilarly;
    }

    public List<Float> getCandidatesSimilarly() {
        return candidatesSimilarly;
    }

    public void setCandidatesSimilarly(List<Float> candidatesSimilarly) {
        this.candidatesSimilarly = candidatesSimilarly;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    public String getArgName() {
        return argName;
    }

    public void setArgName(String argName) {
        this.argName = argName;
    }

    public List<String> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<String> candidates) {
        this.candidates = candidates;
    }
}
