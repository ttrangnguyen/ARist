package flute.jdtparser.callsequence;

import flute.jdtparser.FileParser;
import flute.jdtparser.callsequence.expr.ThisExpressionCustom;
import flute.jdtparser.callsequence.node.cfg.*;
import org.eclipse.jdt.core.dom.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class FileNode {
    private FileParser fileParser;

    public FileNode(FileParser fileParser) {
        this.fileParser = fileParser;
    }

    List<MinimalNode> rootNodeList = new ArrayList<>();
    List<Set<IBinding>> trackingNodeList = new ArrayList<>();

    int methodId = 0;

    public MinimalNode parseMinimalCFG(Statement statement, MinimalNode parentNode) {
        return parseMinimalCFG(statement, parentNode, true);
    }

    public MinimalNode parseMinimalCFG(ASTNode statement, MinimalNode parentNode, boolean isMainLoop) {
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
        } else if (statement instanceof WhileStatement) {
            WhileStatement whileStatement = (WhileStatement) statement;
            MinimalNode conditionNode = new IfNode(whileStatement.getExpression());
            parentNode.addNextNode(conditionNode);
            parseMinimalCFG(whileStatement.getBody(), conditionNode, false);
            parseMinimalCFG(null, conditionNode, false);
        } else if (statement instanceof DoStatement) {
            DoStatement doStatement = (DoStatement) statement;
            MinimalNode doBody = parseMinimalCFG(doStatement.getBody(), parentNode, false);

            curNode = doBody;

            MinimalNode conditionNode = new IfNode(doStatement.getExpression());
            doBody.addNextNode(conditionNode);
            parseMinimalCFG(doStatement.getBody(), conditionNode, false);
            parseMinimalCFG(null, conditionNode, false);
        } else if (statement instanceof ForStatement) {
            ForStatement forStatement = (ForStatement) statement;

            AtomicReference<MinimalNode> lastNode = new AtomicReference<>();
            forStatement.initializers().forEach(expr -> {
                ASTNode astNode = (ASTNode) expr;
                lastNode.set(parseMinimalCFG(astNode, parentNode, false));
            });

            if (lastNode.get() == null)
                lastNode.set(parentNode);

            curNode = lastNode.get();

            MinimalNode conditionNode = new IfNode(forStatement.getExpression());
            lastNode.get().addNextNode(conditionNode);

            MinimalNode forBody = parseMinimalCFG(forStatement.getBody(), conditionNode, false);
            parseMinimalCFG(null, conditionNode, false);

            forStatement.updaters().forEach(expr -> {
                ASTNode astNode = (ASTNode) expr;
                lastNode.set(parseMinimalCFG(astNode, forBody, false));
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

    public static IBinding genBindingKey(MethodInvocation methodInvocation) {
        Expression expr = methodInvocation.getExpression();
        IBinding bindingKey = null;
        if (expr instanceof SimpleName) {
            SimpleName simpleName = (SimpleName) expr;
            bindingKey = simpleName.resolveBinding();
        } else {
            if (expr == null) {
                ITypeBinding resolveType = methodInvocation.resolveMethodBinding() == null
                        ? null : methodInvocation.resolveMethodBinding().getDeclaringClass();
                bindingKey = ThisExpressionCustom.create(resolveType);
            } else if (expr instanceof ThisExpression) {
                bindingKey = ThisExpressionCustom.create(expr.resolveTypeBinding());
            } else
                bindingKey = expr.resolveTypeBinding();
        }
        return bindingKey;
    }

    public void parse() {
        fileParser.getCu().accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration methodDeclaration) {
                StartNode startNode = new StartNode();
                rootNodeList.add(startNode);
                parseMinimalCFG(methodDeclaration.getBody(), startNode);
                trackingNodeList.add(new HashSet<>());

                methodDeclaration.accept(new ASTVisitor() {
                    @Override
                    public boolean visit(MethodInvocation methodInvocation) {
                        IBinding bindingKey = genBindingKey(methodInvocation);

                        trackingNodeList.get(methodId).add(bindingKey);
                        return super.visit(methodDeclaration);
                    }
                });

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
