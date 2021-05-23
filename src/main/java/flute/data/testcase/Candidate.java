package flute.data.testcase;

public class Candidate {
    protected String excode;
    protected String name;
    protected boolean isTargetMatched;

    public Candidate(String excode, String name) {
        this.excode = excode;
        this.name = name;
        this.isTargetMatched = false;
    }

    public String getExcode() {
        return excode;
    }

    public void setExcode(String excode) {
        this.excode = excode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isTargetMatched() {
        return isTargetMatched;
    }

    public void setTargetMatched(boolean targetMatched) {
        isTargetMatched = targetMatched;
    }
}
