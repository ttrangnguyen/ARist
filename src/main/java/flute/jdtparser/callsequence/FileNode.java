package flute.jdtparser.callsequence;

import flute.jdtparser.FileParser;
import flute.jdtparser.callsequence.node.cfg.*;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FileNode {
    private FileParser fileParser;

    public FileNode(FileParser fileParser) {
        this.fileParser = fileParser;
    }

    private HashMap<IBinding, MethodCallNode> callSequence = new HashMap<>();
    private HashMap<IBinding, MethodCallNode> lastNode = new HashMap<>();


    public void addToSequence(MethodInvocation methodInvocation) {
        Expression expr = methodInvocation.getExpression();
        IBinding bindingKey = null;
        if (expr instanceof SimpleName) {
            SimpleName simpleName = (SimpleName) expr;
            bindingKey = simpleName.resolveBinding();
        } else {
            if (expr == null) {
                bindingKey = methodInvocation.resolveMethodBinding().getDeclaringClass();
            } else
                bindingKey = expr.resolveTypeBinding();
        }
        if (callSequence.get(bindingKey) == null) {
            MethodCallNode rootNode = new MethodCallNode(methodInvocation);
            callSequence.put(bindingKey, rootNode);
            lastNode.put(bindingKey, rootNode);
        } else {
            MethodCallNode nextNode = new MethodCallNode(methodInvocation);
            lastNode.get(bindingKey).addChildNode(nextNode);
            lastNode.put(bindingKey, nextNode);
        }
    }

    private void parseStmt(Statement statement) {
        if (statement instanceof Block) {
            Block block = (Block) statement;
            block.statements().forEach(stmt -> {
                parseStmt((Statement) stmt);
            });
        } else if (!(statement instanceof IfStatement
                || statement instanceof TryStatement)) {
            statement.accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodInvocation methodInvocation) {
                    addToSequence(methodInvocation);
                    return super.visit(methodInvocation);
                }
            });
        } else if (statement instanceof TryStatement) {
            TryStatement tryStatement = (TryStatement) statement;
            parseStmt(tryStatement.getBody());
        }
//        else if (statement instanceof IfStatement) {
//            IfStatement ifStatement = (IfStatement) statement;
//            parseStmt(ifStatement.getThenStatement());
//        }
    }

    List<MinimalNode> rootNodeList = new ArrayList<>();
    int methodId = 0;

    public MinimalNode parseMinimalCFG(Statement statement, MinimalNode parentNode) {
        return parseMinimalCFG(statement, parentNode, true);
    }

    public MinimalNode parseMinimalCFG(Statement statement, MinimalNode parentNode, boolean isMainLoop) {
        MinimalNode curNode = parentNode;
        if (statement instanceof Block) {
            Block block = (Block) statement;
            for (int i = 0; i < block.statements().size(); i++) {
                curNode = parseMinimalCFG((Statement) block.statements().get(i), curNode, false);
            }
            if (block.statements().size() == 0) {
                curNode = parseMinimalCFG(null, curNode, false);
            }
        } else if (statement instanceof IfStatement) {
            IfStatement ifStatement = (IfStatement) statement;
            MinimalNode conditionNode = new IfNode(ifStatement.getExpression());
            parentNode.addNextNode(conditionNode);
            parseMinimalCFG(ifStatement.getThenStatement(), conditionNode, false);
            parseMinimalCFG(ifStatement.getElseStatement(), conditionNode, false);
        } else if (statement instanceof TryStatement) {
            TryNode tryNode = new TryNode();
            parentNode.addNextNode(tryNode);

            TryStatement tryStatement = (TryStatement) statement;
            parseMinimalCFG(tryStatement.getBody(), tryNode);
            tryStatement.catchClauses().forEach(catchClause -> {
                parseMinimalCFG(((CatchClause) catchClause).getBody(), tryNode);
            });
        } else {
            if ((parentNode instanceof StmtNode) && ((StmtNode) parentNode).getStatement() == null) {
                ((StmtNode) parentNode).setStatement(statement);
            } else {
                curNode = new StmtNode(statement);
                parentNode.addNextNode(curNode);
            }
        }
        if (isMainLoop) {
            flatNode(rootNodeList.get(methodId));
        }
        return curNode;
    }

    public void flatNode(MinimalNode node) {
        if (node.getNextNode().size() == 1) {
            node = node.getNextNode().get(0);
            flatNode(node);
        } else if (node.getNextNode().size() >= 2) {
            int loopSize = node.getNextNode().size() - 2;
            for (int i = loopSize; i >= 0; i--) {
                if ((node.getNextNode().get(i) instanceof TryNode) || (node.getNextNode().get(i) instanceof IfNode)) {
                    MinimalNode finalNode = node;
                    int finalI = i;
                    lastNode(node.getNextNode().get(i)).forEach(endNode -> {
                        endNode.addNextNode(finalNode.getNextNode().get(finalI + 1));
                    });
                    node.getNextNode().remove(i + 1);
                } else flatNode(node.getNextNode().get(i));
            }
        }
    }

    public List<MinimalNode> lastNode(MinimalNode node) {
        List<MinimalNode> result = new ArrayList<>();
        lastNode(node, result);
        return result;
    }

    public void lastNode(MinimalNode node, List<MinimalNode> result) {
        if (node.getNextNode().size() > 0) {
            node.getNextNode().forEach(nextNode -> {
                lastNode(nextNode, result);
            });
        } else {
            result.add(node);
        }
    }

    public void parse() {
        fileParser.getCu().accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration methodDeclaration) {
                StartNode startNode = new StartNode();
                rootNodeList.add(startNode);
                parseMinimalCFG(methodDeclaration.getBody(), startNode);
                methodId++;
                return super.visit(methodDeclaration);
            }
        });
    }

    public FileParser getFileParser() {
        return fileParser;
    }

    public void setFileParser(FileParser fileParser) {
        this.fileParser = fileParser;
    }
}
