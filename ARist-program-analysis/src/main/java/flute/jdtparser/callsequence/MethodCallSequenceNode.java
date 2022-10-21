package flute.jdtparser.callsequence;

public class MethodCallSequenceNode {
    private MethodCallSequenceNode next = null;

    private MethodCallNode methodCallNode;

    public MethodCallNode getMethodCallNode() {
        return methodCallNode;
    }

    public void setMethodCallNode(MethodCallNode methodCallNode) {
        this.methodCallNode = methodCallNode;
    }
}
