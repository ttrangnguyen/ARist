package flute.jdtparser.callsequence;

import org.eclipse.jdt.core.dom.Statement;

import java.util.ArrayList;
import java.util.List;

public class MinimalCFGNode {
    private Statement statement;
    private List<MinimalCFGNode> nextNode = new ArrayList<>();

    public MinimalCFGNode(Statement statement) {
        this.statement = statement;
    }

    public Statement getStatement() {
        return statement;
    }

    public void setStatement(Statement statement) {
        this.statement = statement;
    }

    public List<MinimalCFGNode> getNextNode() {
        return nextNode;
    }

    public void addNextNode(MinimalCFGNode nextNode) {
        this.nextNode.add(nextNode);
    }
}
