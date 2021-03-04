package flute.jdtparser.callsequence;

import flute.data.MethodInvocationModel;
import flute.jdtparser.FileParser;
import flute.jdtparser.callsequence.expr.SuperExpressionCustom;
import flute.jdtparser.callsequence.expr.ThisExpressionCustom;
import flute.jdtparser.callsequence.node.ast.ASTCustomNode;
import flute.jdtparser.callsequence.node.ast.CaseBlock;
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

    public MinimalNode parseMinimalCFG(ASTCustomNode statement, MinimalNode parentNode, boolean isMainLoop) {
        MinimalNode curNode = parentNode;
        if (statement instanceof CaseBlock) {
            CaseBlock block = (CaseBlock) statement;
            for (int i = 0; i < block.statements().size(); i++) {
                curNode = parseMinimalCFG((Statement) block.statements().get(i), curNode, false);
            }
            if (block.statements().size() == 0) {
                curNode = parseMinimalCFG((ASTNode) null, curNode, false);
            }
        }
        return curNode;
    }

    public MinimalNode parseMinimalCFG(ASTNode statement, MinimalNode parentNode, boolean isMainLoop) {
        MinimalNode curNode = parentNode;
        if (statement instanceof Block) {
            Block block = (Block) statement;
            for (int i = 0; i < block.statements().size(); i++) {
                curNode = parseMinimalCFG((Statement) block.statements().get(i), curNode, false);
            }
            if (block.statements().size() == 0) {
                curNode = parseMinimalCFG((ASTNode) null, curNode, false);
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
            parseMinimalCFG(tryStatement.getBody(), tryNode, false);
            tryStatement.catchClauses().forEach(catchClause -> {
                parseMinimalCFG(((CatchClause) catchClause).getBody(), tryNode, false);
            });
        } else if (statement instanceof SwitchStatement) {
            SwitchStatement switchStatement = (SwitchStatement) statement;
            SwitchNode switchNode = new SwitchNode(switchStatement.getExpression());
            parentNode.addNextNode(switchNode);
            Utils.parseCaseBlock(switchStatement).forEach(caseBlock -> {
                CaseNode caseNode = new CaseNode(caseBlock.getExpression());
                switchNode.addNextNode(caseNode);
                parseMinimalCFG(caseBlock, caseNode, false);
            });
        } else if (statement instanceof WhileStatement) {
            WhileStatement whileStatement = (WhileStatement) statement;
            MinimalNode conditionNode = new IfNode(whileStatement.getExpression());
            parentNode.addNextNode(conditionNode);
            parseMinimalCFG(whileStatement.getBody(), conditionNode, false);
            parseMinimalCFG((ASTNode) null, conditionNode, false);
        } else if (statement instanceof DoStatement) {
            DoStatement doStatement = (DoStatement) statement;
            MinimalNode doBody = parseMinimalCFG(doStatement.getBody(), parentNode, false);

            curNode = doBody;

            MinimalNode conditionNode = new IfNode(doStatement.getExpression());
            doBody.addNextNode(conditionNode);
            parseMinimalCFG(doStatement.getBody(), conditionNode, false);
            parseMinimalCFG((ASTNode) null, conditionNode, false);
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
            parseMinimalCFG((ASTNode) null, conditionNode, false);

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
                if ((node.getNextNode().get(i) instanceof BreakNode)) {
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
        //auto gc
        ThisExpressionCustom.gc();
        SuperExpressionCustom.gc();

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

    public static IBinding genBindingKey(SuperMethodInvocation superMethodInvocation) {
        if (superMethodInvocation.resolveMethodBinding() == null) return null;
        IBinding bindingKey = SuperExpressionCustom.create(superMethodInvocation.resolveMethodBinding().getDeclaringClass());
        return bindingKey;
    }

    public static IBinding genBindingKey(MethodInvocationModel methodInvocationModel) {
        ASTNode orgNode = methodInvocationModel.getOrgASTNode();
        if (orgNode instanceof MethodInvocation) {
            return genBindingKey((MethodInvocation) orgNode);
        } else if (orgNode instanceof SuperMethodInvocation) {
            return genBindingKey((SuperMethodInvocation) orgNode);
        }
        return null;
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
                        return super.visit(methodInvocation);
                    }

                    @Override
                    public boolean visit(SuperMethodInvocation superMethodInvocation) {
                        IBinding bindingKey = genBindingKey(superMethodInvocation);
                        trackingNodeList.get(methodId).add(bindingKey);
                        return super.visit(superMethodInvocation);
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

    public List<MinimalNode> getRootNodeList() {
        return rootNodeList;
    }

    public List<Set<IBinding>> getTrackingNodeList() {
        return trackingNodeList;
    }
}
