package flute.data.testcase;

public class Candidate {
    protected String excode;
    protected String name;

    public Candidate(String excode, String name) {
        this.excode = excode;
        this.name = name;
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
}
