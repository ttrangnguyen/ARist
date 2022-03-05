package flute.jdtparser.callsequence;

public class MethodCallSequence {
    private MethodCallNode firstNode;
    private MethodCallNode lastNode;

    public MethodCallSequence(MethodCallNode firstNode, MethodCallNode lastNode) {
        this.firstNode = firstNode;
        this.lastNode = lastNode;
    }

    public MethodCallNode getFirstNode() {
        return firstNode;
    }

    public void setFirstNode(MethodCallNode firstNode) {
        this.firstNode = firstNode;
    }

    public MethodCallNode getLastNode() {
        return lastNode;
    }

    public void setLastNode(MethodCallNode lastNode) {
        this.lastNode = lastNode;
    }
}
