package flute.jdtparser.callsequence.node.cfg;

import java.util.ArrayList;
import java.util.List;

public class MinimalNode {
    protected List<MinimalNode> nextNode = new ArrayList<>();

    public List<MinimalNode> getNextNode() {
        return nextNode;
    }

    public void addNextNode(MinimalNode nextNode) {
        this.nextNode.add(nextNode);
    }

    public MinimalNode() {}
}
