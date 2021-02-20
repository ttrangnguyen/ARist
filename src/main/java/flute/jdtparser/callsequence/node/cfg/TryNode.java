package flute.jdtparser.callsequence.node.cfg;

public class TryNode extends MinimalNode {
    public TryNode() {
        super();
    }

    @Override
    public String toString() {
        return "[" + super.nextNode.size() + "] TryNode";
    }
}
