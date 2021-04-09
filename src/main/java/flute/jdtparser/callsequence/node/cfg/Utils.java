package flute.jdtparser.callsequence.node.cfg;

import flute.config.Config;
import flute.data.MethodInvocationModel;
import flute.jdtparser.callsequence.FileNode;
import flute.jdtparser.callsequence.MethodCallNode;
import flute.jdtparser.callsequence.expr.SuperExpressionCustom;
import flute.jdtparser.callsequence.expr.ThisExpressionCustom;
import flute.jdtparser.callsequence.node.ast.CaseBlock;
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

    private static List<List<String>> visitMethodCallNode(MethodCallNode node, Stack<String> stack) {
        stack.push(node.getValue().resolveMethodBinding() != null ?
                Utils.nodeToString(node.getValue().resolveMethodBinding()) :
                Utils.nodeToString(node.getValue()));

        List<List<String>> methodCallSequences = new ArrayList<>();
        if (node.getChildNode().size() == 0) {
            methodCallSequences.add(stack);
        }
        for (MethodCallNode childNode : node.getChildNode()) {
            methodCallSequences.addAll(visitMethodCallNode(childNode, stack));
        }
        stack.pop();
        return methodCallSequences;
    }

    public static String getOrgPackage(IBinding keyBinding) {
        if (keyBinding == null) return "";
        if (keyBinding instanceof ITypeBinding) {
            ITypeBinding typeBinding = (ITypeBinding) keyBinding;
            return getOrgPackage(typeBinding);
        } else if (keyBinding instanceof IVariableBinding) {
            IVariableBinding variableBinding = (IVariableBinding) keyBinding;
            return getOrgPackage(variableBinding.getType());
        } else if (keyBinding instanceof ThisExpressionCustom) {
            ThisExpressionCustom thisExpressionCustom = (ThisExpressionCustom) keyBinding;
            return getOrgPackage(thisExpressionCustom.getDeclaringClass());
        } else if (keyBinding instanceof ThisExpressionCustom) {
            ThisExpressionCustom thisExpressionCustom = (ThisExpressionCustom) keyBinding;
            return getOrgPackage(thisExpressionCustom.getDeclaringClass());
        } else if (keyBinding instanceof SuperExpressionCustom) {
            SuperExpressionCustom superExpressionCustom = (SuperExpressionCustom) keyBinding;
            if (superExpressionCustom.getDeclaringClass() != null) {
                return getOrgPackage(superExpressionCustom.getDeclaringClass().getSuperclass());
            }
        }
        return "";
    }

    public static String getOrgPackage(ITypeBinding typeBinding) {
        if (typeBinding == null || typeBinding.getPackage() == null) return "";
        return typeBinding.getPackage().getName();
    }

    public static Map<IBinding, MethodCallNode> groupMethodCallNodeByTrackingNode(MethodCallNode node) {
        Map<IBinding, MethodCallNode> tracking2TreeMap = new HashMap<>();
        if (node == null) return tracking2TreeMap;

        Map<IBinding, List<MethodCallNode>> tracking2ListMap = new HashMap<>();
        groupMethodCallNodeByTrackingNode(node, tracking2ListMap);

        for (IBinding id: tracking2ListMap.keySet()) {
            List<MethodCallNode> methodCallNodes = tracking2ListMap.get(id);
            Stack<MethodCallNode> stack = new Stack<>();
            // Add a virtual root node
            stack.push(new MethodCallNode(null));
            stack.peek().setArriveId(Integer.MIN_VALUE);
            stack.peek().setLeaveId(Integer.MAX_VALUE);

            for (MethodCallNode methodCallNode: methodCallNodes) {
                while (!stack.peek().isAscendanceOf(methodCallNode)) {
                    stack.pop().uniqueChildNode();
                }
                stack.peek().addChildNode(methodCallNode);
                stack.add(methodCallNode);
            }
            while (stack.size() > 1) stack.pop().uniqueChildNode();
            stack.peek().uniqueChildNode();
            if (stack.peek().getChildNode().size() == 1) {
                stack.push(stack.peek().getChildNode().get(0));
            }
            tracking2TreeMap.put(id, stack.peek());
        }
        return tracking2TreeMap;
    }

    private static void groupMethodCallNodeByTrackingNode(MethodCallNode node, Map<IBinding, List<MethodCallNode>> trackingMap) {
        MethodCallNode copy = node.copy();
        copy.markArrive();

        IBinding id = FileNode.genBindingKey(node.getValue());
        if (!trackingMap.containsKey(id)) {
            // Add a virtual root node
            trackingMap.put(id, new ArrayList<>());
        }
        trackingMap.get(id).add(copy);
        for (MethodCallNode childNode : node.getChildNode()) {
            groupMethodCallNodeByTrackingNode(childNode, trackingMap);
        }

        copy.markLeave();
    }

    public static boolean checkTargetAPI(String packageName) {
        for (String targetAPI : Config.TEST_APIS) {
            {
                if (packageName.startsWith(targetAPI)) {
                    return true;
                }
            }
        }
        return false;
    }
}
