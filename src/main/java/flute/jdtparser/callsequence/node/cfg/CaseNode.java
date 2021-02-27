package flute.jdtparser.callsequence.node.cfg;

import org.eclipse.jdt.core.dom.Expression;

public class CaseNode extends MinimalNode {
    private Expression expression;
    public CaseNode(Expression expr) {
       expression = expr;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }
}
