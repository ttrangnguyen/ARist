package flute.jdtparser.callsequence.node.cfg;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;

public class IfNode extends BreakNode {
    private Expression expression;
    public IfNode(Expression expr) {
       expression = expr;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }
}
