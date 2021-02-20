package flute.jdtparser.callsequence;

import flute.jdtparser.FileParser;
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

    List<MinimalCFGNode> rootNodeList = new ArrayList<>();
    int methodId = 0;

    public MinimalCFGNode parseMinimalCFG(ASTNode statement, MinimalCFGNode parentNode) {
        return parseMinimalCFG(statement, parentNode, true);
    }

    //statement is statement or expression
    public MinimalCFGNode parseMinimalCFG(ASTNode statement, MinimalCFGNode parentNode, boolean isMainLoop) {
        MinimalCFGNode curNode = parentNode;
        if (statement instanceof Block) {
            Block block = (Block) statement;
            for (int i = 0; i < block.statements().size(); i++) {
                curNode = parseMinimalCFG((Statement) block.statements().get(i), curNode, false);
            }
        } else if (statement instanceof IfStatement) {
            IfStatement ifStatement = (IfStatement) statement;
            MinimalCFGNode conditionNode = parseMinimalCFG(ifStatement.getExpression(), parentNode, false);
            parseMinimalCFG(ifStatement.getThenStatement(), conditionNode, false);
            parseMinimalCFG(ifStatement.getElseStatement(), conditionNode, false);
        } else if (statement instanceof TryStatement) {
            TryStatement tryStatement = (TryStatement) statement;
            parseMinimalCFG(tryStatement.getBody(), parentNode);
            tryStatement.catchClauses().forEach(catchClause -> {
                parseMinimalCFG(((CatchClause) catchClause).getBody(), parentNode);
            });
        } else {
            if (rootNodeList.size() - 1 < methodId) rootNodeList.add(parentNode);
            if (parentNode.getStatement() == null) {
                parentNode.setStatement(statement);
            } else {
                curNode = new MinimalCFGNode(statement);
                parentNode.addNextNode(curNode);
            }
        }
        if(isMainLoop){
            flatNode(rootNodeList.get(methodId));
        }
        return curNode;
    }

    public void flatNode(MinimalCFGNode node){
        if(node.getNextNode().size() == 1){
            node = node.getNextNode().get(0);
            flatNode(node);
        }else{
            for (int i = 0; i < node.getNextNode().size() - 1; i++) {
                MinimalCFGNode finalNode = node;
                lastNode(node.getNextNode().get(i)).forEach(endNode->{
                    endNode.addNextNode(finalNode.getNextNode().get(finalNode.getNextNode().size()-1));
                });
            }
            node.getNextNode().remove(node.getNextNode().size() - 1);
        }
    }

    /**
     * Todo
     */
    public List<MinimalCFGNode> lastNode(MinimalCFGNode node){
        return null;
    }

    public void parse() {
        fileParser.getCu().accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration methodDeclaration) {
                MinimalCFGNode rootNode = parseMinimalCFG(methodDeclaration.getBody(), new MinimalCFGNode(null));
                methodId++;
                return super.visit(methodDeclaration);
            }
        });
        System.out.println("a");
    }

    public FileParser getFileParser() {
        return fileParser;
    }

    public void setFileParser(FileParser fileParser) {
        this.fileParser = fileParser;
    }
}
