package flute.jdtparser.callsequence;

import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class MinimalCFGNode {
    private ASTNode statement;
    private boolean isConditionNode = false;
    private List<MinimalCFGNode> nextNode = new ArrayList<>();

    public MinimalCFGNode(ASTNode statement) {
        this.statement = statement;
    }
    public MinimalCFGNode(ASTNode statement, boolean isConditionNode) {
        this.statement = statement;
        this.isConditionNode = isConditionNode;
    }

    public ASTNode getStatement() {
        return statement;
    }

    public void setStatement(ASTNode statement) {
        this.statement = statement;
    }

    public List<MinimalCFGNode> getNextNode() {
        return nextNode;
    }

    public boolean isConditionNode() {
        return isConditionNode;
    }

    public void addNextNode(MinimalCFGNode nextNode) {
        this.nextNode.add(nextNode);
    }
}
