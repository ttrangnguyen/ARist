package flute.jdtparser.callsequence.node.cfg;

import org.eclipse.jdt.core.dom.Expression;

public class SwitchNode extends BreakNode {
    private Expression expression;
    public SwitchNode(Expression expr) {
       expression = expr;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }
}
