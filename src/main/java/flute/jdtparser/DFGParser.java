package flute.jdtparser;

import org.eclipse.jdt.core.dom.*;

public class DFGParser {
    public static boolean checkVariable(VariableDeclarationFragment variable, FileParser fileParser, int startPos, int stopPos) {
        Block block = getDFGBlock(variable);
        boolean result = isInitialized(variable.getName().toString(), block);
        if (result) {
            System.out.println(variable);
        }
        return isInitialized(variable.getName().toString(), block);
    }

    public static boolean isInitialized(String variableName, Statement statement) {
        if (statement instanceof ExpressionStatement) {
            ExpressionStatement expressionStatement = (ExpressionStatement) statement;
            if (expressionStatement.getExpression() instanceof Assignment) {
                Assignment assignment = (Assignment) expressionStatement.getExpression();
                if (assignment.getLeftHandSide() instanceof SimpleName) {
                    if (assignment.getLeftHandSide().toString().equals(variableName)) return true;
                }
            }
        } else if (statement instanceof IfStatement) {
            IfStatement ifStatement = (IfStatement) statement;
            if (isInitialized(variableName, ifStatement.getThenStatement())
                    && (isInitialized(variableName, ifStatement.getElseStatement()) || ifStatement.getElseStatement() == null))
                return true;
        } else if (statement instanceof DoStatement) {
            return isInitialized(variableName, ((DoStatement) statement).getBody());
        } else if (statement instanceof TryStatement) {
            TryStatement tryStatement = (TryStatement) statement;
            if (!isInitialized(variableName, tryStatement.getBody())) return false;
            for (Object catchObject : tryStatement.catchClauses()) {
                if (catchObject instanceof CatchClause) {
                    if (!isInitialized(variableName, ((CatchClause) catchObject).getBody())) {
                        return false;
                    }
                }
            }
            return true;
        } else if (statement instanceof Block) {
            Block block = (Block) statement;
            for (Object item : block.statements()) {
                if (item instanceof Statement) {
                    Statement statementItem = (Statement) item;
                    if (isInitialized(variableName, statementItem)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    public static Block getDFGBlock(ASTNode astNode) {
        if (astNode == null) return null;
        ASTNode parentNode = astNode.getParent();
        if (parentNode instanceof Block) {
            return (Block) parentNode;
        } else return getDFGBlock(parentNode);
    }
}
