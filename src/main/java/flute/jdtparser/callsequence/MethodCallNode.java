package flute.jdtparser.callsequence;

import flute.data.MethodInvocationModel;

import java.util.ArrayList;
import java.util.List;

public class MethodCallNode {
    private List<MethodCallNode> childNode = new ArrayList<>();

    private MethodInvocationModel value;

    public MethodCallNode(MethodInvocationModel value) {
        this.value = value;
    }

    public MethodInvocationModel getValue() {
        return value;
    }

    public void setValue(MethodInvocationModel value) {
        this.value = value;
    }

    public List<MethodCallNode> getChildNode() {
        return childNode;
    }

    public void addChildNode(MethodCallNode node) {
        childNode.add(node);
    }

    public MethodCallNode copy() {
        return new MethodCallNode(getValue());
    }
}
