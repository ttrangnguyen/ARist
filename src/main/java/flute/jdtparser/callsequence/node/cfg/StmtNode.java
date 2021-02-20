package flute.jdtparser.callsequence.node.cfg;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Statement;

import java.util.ArrayList;
import java.util.List;

public class StmtNode extends MinimalNode{
    private Statement statement;

    public StmtNode(Statement statement) {
        this.statement = statement;
    }

    public ASTNode getStatement() {
        return statement;
    }

    public void setStatement(Statement statement) {
        this.statement = statement;
    }


    @Override
    public String toString() {
        return "[" + nextNode.size() + "] " + statement;
    }
}
