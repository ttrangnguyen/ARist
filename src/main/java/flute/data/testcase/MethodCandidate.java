package flute.data.testcase;

public class MethodCandidate {
    protected String name;
    protected String idName;
    protected boolean isTargetMatched;

    public MethodCandidate(String name, String idName) {
        this.name = name;
        this.idName = idName;
        this.isTargetMatched = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdName() {
        return idName;
    }

    public void setIdName(String idName) {
        this.idName = idName;
    }

    public boolean isTargetMatched() {
        return isTargetMatched;
    }

    public void setTargetMatched(boolean targetMatched) {
        isTargetMatched = targetMatched;
    }
}
