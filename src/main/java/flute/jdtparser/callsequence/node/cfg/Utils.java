package flute.jdtparser.callsequence.node.cfg;

import flute.data.MethodInvocationModel;
import flute.jdtparser.callsequence.FileNode;
import flute.jdtparser.callsequence.MethodCallNode;
import flute.jdtparser.callsequence.node.ast.CaseBlock;
import flute.utils.logging.Logger;
import org.eclipse.jdt.core.dom.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Utils {
    public static List<MethodInvocationModel> extractNode(MinimalNode minimalNode) {
        List<MethodInvocationModel> methodInvocationList = new ArrayList<>();
        if (minimalNode instanceof StmtNode) {
            StmtNode stmtNode = (StmtNode) minimalNode;
            return visitMethodCall(stmtNode.getStatement());
        } else if (minimalNode instanceof IfNode) {
            IfNode ifNode = (IfNode) minimalNode;
            return visitMethodCall(ifNode.getExpression());
        } else if (minimalNode instanceof SwitchNode) {
            SwitchNode switchNode = (SwitchNode) minimalNode;
            return visitMethodCall(switchNode.getExpression());
        } else if (minimalNode instanceof CaseNode) {
            CaseNode caseNode = (CaseNode) minimalNode;
            return visitMethodCall(caseNode.getExpression());
        }
        return methodInvocationList;
    }

    private static List<MethodInvocationModel> visitMethodCall(ASTNode astNode) {
        List<MethodInvocationModel> methodInvocationList = new ArrayList<>();

        if (astNode != null)
            astNode.accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodInvocation methodInvocation) {
                    methodInvocationList.add(new MethodInvocationModel(methodInvocation));
                    return super.visit(methodInvocation);
                }

                @Override
                public boolean visit(SuperMethodInvocation superMethodInvocation) {
                    methodInvocationList.add(new MethodInvocationModel(superMethodInvocation));
                    return super.visit(superMethodInvocation);
                }
            });
        return methodInvocationList;
    }

    public static String nodeToString(MethodInvocationModel methodInvocation) {
        return methodInvocation.getName() + "(" + methodInvocation.arguments().size() + ")";
    }

    public static String nodeToString(IMethodBinding methodInvocation) {
        if (methodInvocation == null) return "null";
        String result;
        String identifierName = String.join(".",
                methodInvocation.getDeclaringClass().getQualifiedName(),
                methodInvocation.getName());
        String paramTypes = "";
        for (ITypeBinding param : methodInvocation.getParameterTypes()) {
            if (paramTypes.length() == 0) {
                paramTypes = param.getQualifiedName();
            } else {
                paramTypes = String.join(",", paramTypes, param.getQualifiedName());
            }
        }
        result = identifierName + "(" + paramTypes + ")";

        result += methodInvocation.getReturnType().getQualifiedName();

        return result;
    }

    public static List<CaseBlock> parseCaseBlock(SwitchStatement switchStatement) {
        List<CaseBlock> caseBlocks = new ArrayList<>();
        AtomicReference<CaseBlock> caseBlock = new AtomicReference<>();
        switchStatement.statements().forEach(stmt -> {
            Statement statement = (Statement) stmt;
            if (statement instanceof SwitchCase) {
                if (caseBlock.get() != null) caseBlocks.add(caseBlock.get());
                SwitchCase switchCase = (SwitchCase) statement;
                caseBlock.set(new CaseBlock(switchCase.getExpression()));
            } else {
                caseBlock.get().statements().add(statement);
            }
        });
        return caseBlocks;
    }

    public static MethodCallNode visitMinimalNode(MinimalNode minimalNode) {
        List<MethodInvocationModel> methodInvocationList = Utils.extractNode(minimalNode);
        MethodCallNode root = null;
        MethodCallNode lastNode = null;
        for (MethodInvocationModel methodInvocation : methodInvocationList) {
            MethodCallNode methodCallNode = new MethodCallNode(methodInvocation);
            if (lastNode == null) {
                root = methodCallNode;
                lastNode = methodCallNode;
            } else {
                lastNode.addChildNode(methodCallNode);
                lastNode = methodCallNode;
            }
        }
        for (MinimalNode childNode : minimalNode.getNextNode()) {
            MethodCallNode methodCallNode = Utils.visitMinimalNode(childNode);
            if (methodCallNode != null) {
                if (lastNode == null) {
                    root = methodCallNode;
                    lastNode = methodCallNode;
                } else {
                    lastNode.addChildNode(methodCallNode);
                }
            }
        }
        return root;
    }

    public static List<List<String>> visitMethodCallNode(MethodCallNode node) {
        if (node.getValue() != null) {
//            System.out.println(node.getValue().resolveMethodBinding().getDeclaringClass().getQualifiedName());
            return visitMethodCallNode(node, new Stack<>());
        } else {
//            System.out.println(node.getValue().resolveMethodBinding().getDeclaringClass().getQualifiedName());
            List<List<String>> methodCallSequences = new ArrayList<>();
            for (MethodCallNode childNode : node.getChildNode()) {
                methodCallSequences.addAll(visitMethodCallNode(childNode, new Stack<>()));
            }
            return methodCallSequences;
        }
//        System.out.println();
    }

    private static List<List<String>> visitMethodCallNode(MethodCallNode node, Stack<MethodCallNode> stack) {
        List<List<String>> methodCallSequences = new ArrayList<>();
        stack.push(node);
        if (node.getChildNode().size() == 0) {
            List<String> methodCallSequence = new ArrayList<>();
            for (MethodCallNode methodCallNode : stack) {
                methodCallSequence.add(
                        methodCallNode.getValue().resolveMethodBinding() != null ?
                                Utils.nodeToString(methodCallNode.getValue().resolveMethodBinding()) :
                                Utils.nodeToString(methodCallNode.getValue())
                );
            }
            methodCallSequences.add(methodCallSequence);
        }
        for (MethodCallNode childNode : node.getChildNode()) {
            methodCallSequences.addAll(visitMethodCallNode(childNode, stack));
        }
        stack.pop();
        return methodCallSequences;
    }

    public static Map<IBinding, MethodCallNode> groupMethodCallNodeByTrackingNode(MethodCallNode node) {
        Map<IBinding, MethodCallNode> map = new HashMap<>();
        if (node == null) return map;
        map.put(FileNode.genBindingKey(node.getValue()), node.copy());
        for (MethodCallNode childNode : node.getChildNode()) {
            Map<IBinding, MethodCallNode> childMap = groupMethodCallNodeByTrackingNode(childNode);
            for (IBinding id : childMap.keySet()) {
                if (!map.containsKey(id)) {
                    map.put(id, new MethodCallNode(null));
                }

                if (childMap.get(id).getValue() != null) {
                    map.get(id).addChildNode(childMap.get(id));
                } else {
                    for (MethodCallNode descendantNode : childMap.get(id).getChildNode()) {
                        map.get(id).addChildNode(descendantNode);
                    }
                }
            }
        }

        for (IBinding id : map.keySet())
            if (map.get(id).getValue() == null && map.get(id).getChildNode().size() < 2) {
                map.put(id, map.get(id).getChildNode().get(0));
            }

        return map;
    }
}
