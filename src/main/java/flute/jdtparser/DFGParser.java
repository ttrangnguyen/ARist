package flute.jdtparser;

import org.eclipse.jdt.core.dom.*;

public class DFGParser {
    public static boolean checkVariable(VariableDeclarationFragment variable, ASTNode curNode, int curPos) {
        if (variable.getInitializer() != null) return true;
        if (variable.getStartPosition() > curPos) return false;
        Block declareBlock = getDFGBlock(variable);
        Block useBlock = getDFGBlock(curNode);

//        boolean result = isInitialized(variable.getName().toString(), declareBlock, curNode, stopPos);
//        System.out.println(variable.getParent());

//        boolean result = isInitialized(variable.getName().toString(), declareBlock, getDFGBlock(curNode), curPos);
//        if (result) {
//            System.out.println(variable);
//        }

        boolean result = blockLoop(variable.getName().toString(), declareBlock, useBlock, curPos);
        return result;
    }

    public static boolean blockLoop(String variableName, Block curBlock, Block useBlock, int curPos) {
        if (curBlock == null) return false;
        for (Object item : curBlock.statements()) {
            if (item instanceof Statement) {
                Statement statement = (Statement) item;
                if (checkContains(statement, curPos))
                    return blockLoop(variableName, getTopDFGBlock(useBlock, statement), useBlock, curPos);
                if (isInitialized(variableName, statement, curPos)) return true;
            }
        }
        return false;
    }

    public static boolean isInitialized(String variableName, Statement statement, int curPos) {
        if (statement == null || statement.getStartPosition() > curPos) return false;

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
            if (isInitialized(variableName, ifStatement.getThenStatement(), curPos)
                    && (ifStatement.getElseStatement() != null && isInitialized(variableName, ifStatement.getElseStatement(), curPos)))
                return true;
        } else if (statement instanceof DoStatement) {
            return isInitialized(variableName, ((DoStatement) statement).getBody(), curPos);
        } else if (statement instanceof TryStatement) {
            TryStatement tryStatement = (TryStatement) statement;
            if (!isInitialized(variableName, tryStatement.getBody(), curPos)) return false;
            for (Object catchObject : tryStatement.catchClauses()) {
                if (catchObject instanceof CatchClause) {
                    if (!isInitialized(variableName, ((CatchClause) catchObject).getBody(), curPos)) {
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
                    if (isInitialized(variableName, statementItem, curPos)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }


    public static boolean checkContains(ASTNode parent, int pos) {
        if (parent.getStartPosition() <= pos
                && (parent.getStartPosition() + parent.getLength()) > pos)
            return true;

        return false;
    }

    public static Block getTopDFGBlock(Block curUseBlock, Statement curStatement) {
        if (curUseBlock == null) return null;
        if (curUseBlock.getParent() == curStatement) {
            return curUseBlock;
        } else {
            return getTopDFGBlock(getDFGBlock(curUseBlock), curStatement);
        }
    }

    public static Block getDFGBlock(ASTNode astNode) {
        if (astNode == null) return null;
        ASTNode parentNode = astNode.getParent();
        if (parentNode instanceof Block) {
            return (Block) parentNode;
        } else return getDFGBlock(parentNode);
    }
}
