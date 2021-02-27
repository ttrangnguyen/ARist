package flute.jdtparser.callsequence.node.cfg;

public class TryNode extends BreakNode {
    public TryNode() {
        super();
    }

    @Override
    public String toString() {
        return "[" + super.nextNode.size() + "] TryNode";
    }
}
