package flute.jdtparser.callsequence.node.cfg;

public class StartNode extends MinimalNode {
    public StartNode() {
        super();
    }

    @Override
    public String toString() {
        return "[" + super.nextNode.size() + "] StartNode";
    }
}
