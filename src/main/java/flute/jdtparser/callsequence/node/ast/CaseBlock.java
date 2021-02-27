package flute.jdtparser.callsequence.node.ast;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Statement;

import java.util.ArrayList;
import java.util.List;

public class CaseBlock extends ASTCustomNode {
    private List<Statement> statements = new ArrayList();
    private Expression expression;

    public CaseBlock(Expression expression) {
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    public List<Statement> statements() {
        return statements;
    }
}
