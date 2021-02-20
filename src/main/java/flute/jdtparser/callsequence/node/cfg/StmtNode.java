package flute.jdtparser.callsequence.node.cfg;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Statement;

import java.util.ArrayList;
import java.util.List;

public class StmtNode extends MinimalNode{
    private ASTNode statement;

    public StmtNode(ASTNode statement) {
        this.statement = statement;
    }

    public ASTNode getStatement() {
        return statement;
    }

    public void setStatement(ASTNode statement) {
        this.statement = statement;
    }


    @Override
    public String toString() {
        return "[" + nextNode.size() + "] " + statement;
    }
}
