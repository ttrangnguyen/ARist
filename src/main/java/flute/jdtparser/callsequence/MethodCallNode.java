package flute.jdtparser.callsequence;

import org.eclipse.jdt.core.dom.MethodInvocation;

import java.util.ArrayList;
import java.util.List;

public class MethodCallNode {
    private List<MethodCallNode> childNode = new ArrayList<>();

    private MethodInvocation value;

    public MethodCallNode(MethodInvocation value) {
        this.value = value;
    }

    public MethodInvocation getValue() {
        return value;
    }

    public void setValue(MethodInvocation value) {
        this.value = value;
    }

    public List<MethodCallNode> getChildNode() {
        return childNode;
    }

    public void addChildNode(MethodCallNode node) {
        childNode.add(node);
    }
}
