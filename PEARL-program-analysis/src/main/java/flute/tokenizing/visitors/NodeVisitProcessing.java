/**
 * 
 */
package flute.tokenizing.visitors;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.WhileStmt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

import flute.tokenizing.excode_data.ControlInfo;
import flute.tokenizing.excode_data.MethodInfo;
import flute.tokenizing.excode_data.MethodInvocInfo;
import flute.tokenizing.excode_data.NodeInfo;

public class NodeVisitProcessing {

	/**
	 * @param args
	 */
	public synchronized static void main(String[] args) {

	}

	public synchronized static NodeInfo addNewFieldAccessNode(MethodInfo curMethodInfo, Stack<NodeInfo> parentNodeStack,
			Stack<ArrayList<NodeInfo>> previousControlFlowNodeStack, long curID, Node n) {

		long ID = curID;
		int nodeType = NodeInfo.CONTROL_TYPE;
		MethodInfo containingMethod = null;
		if (curMethodInfo != null) {
			containingMethod = curMethodInfo;
		}
		// fileInfo.numIfControls++;

		NodeInfo parentNode = null;
		if (!parentNodeStack.isEmpty()) {
			parentNode = parentNodeStack.peek();
		}

		Node parent = n.getParentNode().orElse(null);

		// TODO: should add processing of NodeContent for field;
		Object nodeContent = null;

		NodeInfo nodeInfo = null;

		if (isInvocNode(parent) || isControlNode(parent)) {
			ArrayList<NodeInfo> previousControlNodes = null;
			if (parentNode != null) {
				if (parentNode.previousControlNodes != null) {
					if (parentNode.previousControlNodes.length > 0) {
						previousControlNodes = new ArrayList<NodeInfo>();
						previousControlNodes.addAll(Arrays.asList(parentNode.previousControlNodes));
					}
				}
			}
			ArrayList<NodeInfo> previousDataNodes = null;

			nodeInfo = new NodeInfo(nodeType, ID, containingMethod, parentNode, nodeContent, previousControlNodes,
					previousDataNodes);
			if (parentNode != null) {
				if (NodeInfo.previousControlNodesTmp == null) {
					NodeInfo.previousControlNodesTmp = new ArrayList<NodeInfo>();
				}
				NodeInfo.previousControlNodesTmp.add(nodeInfo);
			}
			if (curMethodInfo != null) {
				if (MethodInfo.nodeTmpList == null) {
					MethodInfo.nodeTmpList = new ArrayList<NodeInfo>(1);
				}
				MethodInfo.nodeTmpList.add(nodeInfo);
			}
		} else {
			ArrayList<NodeInfo> previousControlNodes = null;
			if (previousControlFlowNodeStack.size() > 0) {
				previousControlNodes = new ArrayList<NodeInfo>();
				previousControlNodes.addAll(previousControlFlowNodeStack.peek());
			}

			ArrayList<NodeInfo> previousDataNodes = null;

			nodeInfo = new NodeInfo(nodeType, ID, containingMethod, parentNode, nodeContent, previousControlNodes,
					previousDataNodes);

			ArrayList<NodeInfo> newPreviousControlNodes = new ArrayList<NodeInfo>();
			newPreviousControlNodes.add(nodeInfo);
			previousControlFlowNodeStack.add(newPreviousControlNodes);

			if (curMethodInfo != null) {
				if (MethodInfo.nodeTmpList == null) {
					MethodInfo.nodeTmpList = new ArrayList<NodeInfo>(1);
				}
				MethodInfo.nodeTmpList.add(nodeInfo);
			}
		}

		// TODO: should add processing for parentNodeStack;

		// parentNodeStack.push(nodeInfo);

		if (parentNode != null) {
			parentNode.synchronizeControlDataNodes();
		}
		nodeInfo.synchronizeControlDataNodes();
		return nodeInfo;
	}

	public synchronized static NodeInfo addNewLiteralNode(MethodInfo curMethodInfo, Stack<NodeInfo> parentNodeStack,
			Stack<ArrayList<NodeInfo>> previousControlFlowNodeStack, long curID, Node n) {

		long ID = curID;
		int nodeType = NodeInfo.CONTROL_TYPE;
		MethodInfo containingMethod = null;
		if (curMethodInfo != null) {
			containingMethod = curMethodInfo;
		}
		// fileInfo.numIfControls++;

		NodeInfo parentNode = null;
		if (!parentNodeStack.isEmpty()) {
			parentNode = parentNodeStack.peek();
		}

		Node parent = n.getParentNode().orElse(null);

		// TODO: should add processing of NodeContent for field;
		Object nodeContent = null;

		NodeInfo nodeInfo = null;

		if (isInvocNode(parent) || isControlNode(parent)) {
			ArrayList<NodeInfo> previousControlNodes = null;
			if (parentNode != null) {
				if (parentNode.previousControlNodes != null) {
					if (parentNode.previousControlNodes.length > 0) {
						previousControlNodes = new ArrayList<NodeInfo>();
						previousControlNodes.addAll(Arrays.asList(parentNode.previousControlNodes));
					}
				}
			}

			ArrayList<NodeInfo> previousDataNodes = null;

			nodeInfo = new NodeInfo(nodeType, ID, containingMethod, parentNode, nodeContent, previousControlNodes,
					previousDataNodes);

			if (parentNode != null) {
				if (NodeInfo.previousControlNodesTmp == null) {
					NodeInfo.previousControlNodesTmp = new ArrayList<NodeInfo>();
				}
			}
			if (parentNode != null) {
				NodeInfo.previousControlNodesTmp.add(nodeInfo);
			}
			if (curMethodInfo != null) {
				if (MethodInfo.nodeTmpList == null) {
					MethodInfo.nodeTmpList = new ArrayList<NodeInfo>(1);
				}
				MethodInfo.nodeTmpList.add(nodeInfo);
			}
		} else {
			ArrayList<NodeInfo> previousControlNodes = null;
			if (previousControlFlowNodeStack.size() > 0) {
				previousControlNodes = new ArrayList<NodeInfo>();
				previousControlNodes.addAll(previousControlFlowNodeStack.peek());
			}

			ArrayList<NodeInfo> previousDataNodes = null;

			nodeInfo = new NodeInfo(nodeType, ID, containingMethod, parentNode, nodeContent, previousControlNodes,
					previousDataNodes);

			ArrayList<NodeInfo> newPreviousControlNodes = new ArrayList<NodeInfo>();
			newPreviousControlNodes.add(nodeInfo);
			previousControlFlowNodeStack.add(newPreviousControlNodes);

			if (curMethodInfo != null) {
				if (MethodInfo.nodeTmpList == null) {
					MethodInfo.nodeTmpList = new ArrayList<NodeInfo>(1);
				}
				MethodInfo.nodeTmpList.add(nodeInfo);
			}
		}

		if (parentNode != null) {
			parentNode.synchronizeControlDataNodes();
		}
		nodeInfo.synchronizeControlDataNodes();
		return nodeInfo;
	}

	public synchronized static NodeInfo addNewAssignNode(String assignType, MethodInfo curMethodInfo,
			Stack<NodeInfo> parentNodeStack, Stack<ArrayList<NodeInfo>> previousControlFlowNodeStack, long curID,
			Node n) {

		long ID = curID;
		int nodeType = NodeInfo.ASSIGN_TYPE;
		MethodInfo containingMethod = null;
		if (curMethodInfo != null) {
			containingMethod = curMethodInfo;
		}
		// fileInfo.numIfControls++;

		NodeInfo parentNode = null;
		if (!parentNodeStack.isEmpty()) {
			parentNode = parentNodeStack.peek();
		}

		Node parent = n.getParentNode().orElse(null);

		// TODO: should add processing of NodeContent for field;
		Object nodeContent = null;

		NodeInfo nodeInfo = null;

		if (isInvocNode(parent) || isControlNode(parent)) {
			ArrayList<NodeInfo> previousControlNodes = null;
			if (parentNode != null) {
				if (parentNode.previousControlNodes != null) {
					if (parentNode.previousControlNodes.length > 0) {
						previousControlNodes = new ArrayList<NodeInfo>();
						previousControlNodes.addAll(Arrays.asList(parentNode.previousControlNodes));
					}
				}
			}
			ArrayList<NodeInfo> previousDataNodes = null;

			nodeInfo = new NodeInfo(nodeType, ID, containingMethod, parentNode, nodeContent, previousControlNodes,
					previousDataNodes);

			if (parentNode != null) {
				if (NodeInfo.previousControlNodesTmp == null) {
					NodeInfo.previousControlNodesTmp = new ArrayList<NodeInfo>();
				}
				NodeInfo.previousControlNodesTmp.add(nodeInfo);
			}
			if (curMethodInfo != null) {
				if (MethodInfo.nodeTmpList == null) {
					MethodInfo.nodeTmpList = new ArrayList<NodeInfo>(1);
				}
				MethodInfo.nodeTmpList.add(nodeInfo);
			}
		} else {
			ArrayList<NodeInfo> previousControlNodes = null;
			if (previousControlFlowNodeStack.size() > 0) {
				previousControlNodes = new ArrayList<NodeInfo>();
				previousControlNodes.addAll(previousControlFlowNodeStack.peek());
			}

			ArrayList<NodeInfo> previousDataNodes = null;

			nodeInfo = new NodeInfo(nodeType, ID, containingMethod, parentNode, nodeContent, previousControlNodes,
					previousDataNodes);

			ArrayList<NodeInfo> newPreviousControlNodes = new ArrayList<NodeInfo>();
			newPreviousControlNodes.add(nodeInfo);
			previousControlFlowNodeStack.add(newPreviousControlNodes);

			if (curMethodInfo != null) {
				if (MethodInfo.nodeTmpList == null) {
					MethodInfo.nodeTmpList = new ArrayList<NodeInfo>(1);
				}
				MethodInfo.nodeTmpList.add(nodeInfo);
			}
		}

		if (parentNode != null) {
			parentNode.synchronizeControlDataNodes();
		}
		nodeInfo.synchronizeControlDataNodes();
		return nodeInfo;
	}

	// TODO: should check this
	public synchronized static NodeInfo addTypeNode(MethodInfo curMethodInfo, Stack<NodeInfo> parentNodeStack,
			Stack<ArrayList<NodeInfo>> previousControlFlowNodeStack, long curID, Node n) {

		long ID = curID;
		int nodeType = NodeInfo.DATA_TYPE;
		MethodInfo containingMethod = null;
		if (curMethodInfo != null) {
			containingMethod = curMethodInfo;
		}
		// fileInfo.numIfControls++;

		NodeInfo parentNode = null;
		if (!parentNodeStack.isEmpty()) {
			parentNode = parentNodeStack.peek();
		}

		Node parent = n.getParentNode().orElse(null);

		// TODO: should add processing of NodeContent for field;
		Object nodeContent = null;

		NodeInfo nodeInfo = null;

		if (isInvocNode(parent) || isControlNode(parent)) {
			ArrayList<NodeInfo> previousControlNodes = null;
			if (parentNode.previousControlNodes != null) {
				if (parentNode.previousControlNodes.length > 0) {
					previousControlNodes = new ArrayList<NodeInfo>();
					previousControlNodes.addAll(Arrays.asList(parentNode.previousControlNodes));
				}
			}

			ArrayList<NodeInfo> previousDataNodes = null;

			nodeInfo = new NodeInfo(nodeType, ID, containingMethod, parentNode, nodeContent, previousControlNodes,
					previousDataNodes);

			if (NodeInfo.previousControlNodesTmp == null) {
				NodeInfo.previousControlNodesTmp = new ArrayList<NodeInfo>();
			}
			NodeInfo.previousControlNodesTmp.add(nodeInfo);
			if (curMethodInfo != null) {
				if (MethodInfo.nodeTmpList == null) {
					MethodInfo.nodeTmpList = new ArrayList<NodeInfo>(1);
				}
				MethodInfo.nodeTmpList.add(nodeInfo);
			}
		} else {
			ArrayList<NodeInfo> previousControlNodes = null;
			if (previousControlFlowNodeStack.size() > 0) {
				previousControlNodes = new ArrayList<NodeInfo>();
				previousControlNodes.addAll(previousControlFlowNodeStack.peek());
			}

			ArrayList<NodeInfo> previousDataNodes = null;

			nodeInfo = new NodeInfo(nodeType, ID, containingMethod, parentNode, nodeContent, previousControlNodes,
					previousDataNodes);

			ArrayList<NodeInfo> newPreviousControlNodes = new ArrayList<NodeInfo>();
			newPreviousControlNodes.add(nodeInfo);
			previousControlFlowNodeStack.add(newPreviousControlNodes);

			if (curMethodInfo != null) {
				if (MethodInfo.nodeTmpList == null) {
					MethodInfo.nodeTmpList = new ArrayList<NodeInfo>(1);
				}
				MethodInfo.nodeTmpList.add(nodeInfo);
			}
		}

		if (parentNode != null) {
			parentNode.synchronizeControlDataNodes();
		}
		nodeInfo.synchronizeControlDataNodes();
		return nodeInfo;
	}

	// TODO: should check this
	public synchronized static NodeInfo addVarNode(MethodInfo curMethodInfo, Stack<NodeInfo> parentNodeStack,
			Stack<ArrayList<NodeInfo>> previousControlFlowNodeStack, long curID, Node n) {

		long ID = curID;
		int nodeType = NodeInfo.DATA_TYPE;
		MethodInfo containingMethod = null;
		if (curMethodInfo != null) {
			containingMethod = curMethodInfo;
		}
		// fileInfo.numIfControls++;

		NodeInfo parentNode = null;
		if (!parentNodeStack.isEmpty()) {
			parentNode = parentNodeStack.peek();
		}

		Node parent = n.getParentNode().orElse(null);

		// TODO: should add processing of NodeContent for field;
		Object nodeContent = null;

		NodeInfo nodeInfo = null;

		if (isInvocNode(parent) || isControlNode(parent)) {
			ArrayList<NodeInfo> previousControlNodes = null;
			if (parentNode != null) {
				if (parentNode.previousControlNodes != null) {
					if (parentNode.previousControlNodes.length > 0) {
						previousControlNodes = new ArrayList<NodeInfo>();
						previousControlNodes.addAll(Arrays.asList(parentNode.previousControlNodes));
					}
				}
			}
			ArrayList<NodeInfo> previousDataNodes = null;

			nodeInfo = new NodeInfo(nodeType, ID, containingMethod, parentNode, nodeContent, previousControlNodes,
					previousDataNodes);

			if (parentNode != null) {
				if (NodeInfo.previousControlNodesTmp == null) {
					NodeInfo.previousControlNodesTmp = new ArrayList<NodeInfo>();
				}
				NodeInfo.previousControlNodesTmp.add(nodeInfo);
			}
			if (curMethodInfo != null) {
				if (MethodInfo.nodeTmpList == null) {
					MethodInfo.nodeTmpList = new ArrayList<NodeInfo>(1);
				}
				MethodInfo.nodeTmpList.add(nodeInfo);
			}
		} else {
			ArrayList<NodeInfo> previousControlNodes = null;
			if (previousControlFlowNodeStack.size() > 0) {
				previousControlNodes = new ArrayList<NodeInfo>();
				previousControlNodes.addAll(previousControlFlowNodeStack.peek());
			}

			ArrayList<NodeInfo> previousDataNodes = null;

			nodeInfo = new NodeInfo(nodeType, ID, containingMethod, parentNode, nodeContent, previousControlNodes,
					previousDataNodes);

			ArrayList<NodeInfo> newPreviousControlNodes = new ArrayList<NodeInfo>();
			newPreviousControlNodes.add(nodeInfo);
			previousControlFlowNodeStack.add(newPreviousControlNodes);

			if (curMethodInfo != null) {
				if (MethodInfo.nodeTmpList == null) {
					MethodInfo.nodeTmpList = new ArrayList<NodeInfo>(1);
				}
				MethodInfo.nodeTmpList.add(nodeInfo);
			}
		}

		if (parentNode != null) {
			parentNode.synchronizeControlDataNodes();
		}
		nodeInfo.synchronizeControlDataNodes();
		return nodeInfo;
	}

	public synchronized static NodeInfo addNewControlNode(MethodInfo curMethodInfo, Stack<NodeInfo> parentNodeStack,
			Stack<ArrayList<NodeInfo>> previousControlFlowNodeStack, long curID, int ControlType) {
		long ID = curID;
		int nodeType = NodeInfo.CONTROL_TYPE;
		MethodInfo containingMethod = null;
		if (curMethodInfo != null) {
			containingMethod = curMethodInfo;
		}
		// fileInfo.numIfControls++;

		NodeInfo parentNode = null;
		if (!parentNodeStack.isEmpty()) {
			parentNode = parentNodeStack.peek();
		}

		ControlInfo nodeContent = new ControlInfo(ControlType);
		ArrayList<NodeInfo> previousControlNodes = null;
		if (previousControlFlowNodeStack.size() > 0) {
			previousControlNodes = new ArrayList<NodeInfo>();
			previousControlNodes.addAll(previousControlFlowNodeStack.peek());
		}

		ArrayList<NodeInfo> previousDataNodes = null;

		NodeInfo nodeInfo = new NodeInfo(nodeType, ID, containingMethod, parentNode, nodeContent, previousControlNodes,
				previousDataNodes);

		if (curMethodInfo != null) {
			if (MethodInfo.nodeTmpList == null) {
				MethodInfo.nodeTmpList = new ArrayList<NodeInfo>(1);
			}
			MethodInfo.nodeTmpList.add(nodeInfo);
		}

		parentNodeStack.push(nodeInfo);

		ArrayList<NodeInfo> newPreviousControlNodes = new ArrayList<NodeInfo>();
		newPreviousControlNodes.add(nodeInfo);
		previousControlFlowNodeStack.add(newPreviousControlNodes);

		if (parentNode != null) {
			parentNode.synchronizeControlDataNodes();
		}
		nodeInfo.synchronizeControlDataNodes();
		return nodeInfo;
	}

	public synchronized static void removeControlNodeInfo(NodeInfo nodeInfo, Stack<NodeInfo> parentNodeStack,
			Stack<ArrayList<NodeInfo>> previousControlFlowNodeStack) {

		parentNodeStack.pop();

		if (previousControlFlowNodeStack.size() == 0) {
			return;
		}

		boolean isFound = false;
		for (ArrayList<NodeInfo> nodeList : previousControlFlowNodeStack) {
			if (nodeList.get(0).ID == nodeInfo.ID) {
				isFound = true;
				break;
			}
		}

		if (isFound) {
			while (previousControlFlowNodeStack.peek().get(0).ID != nodeInfo.ID) {
				previousControlFlowNodeStack.pop();

			}
		}
	}

	public synchronized static NodeInfo addNewInvocNode(MethodInfo curMethodInfo, Stack<NodeInfo> parentNodeStack,
			Stack<ArrayList<NodeInfo>> previousControlFlowNodeStack, long curID, MethodInvocInfo methodInvoc, Node n) {
		long ID = curID;
		int nodeType = NodeInfo.METHODINVOC_TYPE;
		MethodInfo containingMethod = null;
		if (curMethodInfo != null) {
			containingMethod = curMethodInfo;
		}
		// fileInfo.numIfControls++;

		NodeInfo parentNode = null;
		if (!parentNodeStack.isEmpty()) {
			parentNode = parentNodeStack.peek();
		}

		Node parent = n.getParentNode().orElse(null);
		MethodInvocInfo nodeContent = methodInvoc;

		// If the method Invoc belongs to another method Invoc or control, it should
		// place previous the other

		NodeInfo nodeInfo = null;

		if (isInvocNode(parent) || isControlNode(parent)) {
			ArrayList<NodeInfo> previousControlNodes = null;
			// Logger.log("node ad: " + n + "\tparent ad: " + parent + "\tptype: " +
			// parent.getClass() +"\n\t" + parentNode);
			if (parentNode != null) {
				if (parentNode.previousControlNodes != null) {
					if (parentNode.previousControlNodes.length > 0) {
						previousControlNodes = new ArrayList<NodeInfo>();
						previousControlNodes.addAll(Arrays.asList(parentNode.previousControlNodes));
					}
				}
			}
			ArrayList<NodeInfo> previousDataNodes = null;

			nodeInfo = new NodeInfo(nodeType, ID, containingMethod, parentNode, nodeContent, previousControlNodes,
					previousDataNodes);

			if (parentNode != null) {
				if (NodeInfo.previousControlNodesTmp == null) {
					NodeInfo.previousControlNodesTmp = new ArrayList<NodeInfo>();
				}
				NodeInfo.previousControlNodesTmp.add(nodeInfo);
			}
			if (curMethodInfo != null) {
				if (MethodInfo.nodeTmpList == null) {
					MethodInfo.nodeTmpList = new ArrayList<NodeInfo>(1);
				}
				MethodInfo.nodeTmpList.add(nodeInfo);
			}
		} else {
			// Logger.log("node ad: " + n );

			ArrayList<NodeInfo> previousControlNodes = null;
			if (previousControlFlowNodeStack.size() > 0) {
				previousControlNodes = new ArrayList<NodeInfo>();
				previousControlNodes.addAll(previousControlFlowNodeStack.peek());
			}

			ArrayList<NodeInfo> previousDataNodes = null;

			nodeInfo = new NodeInfo(nodeType, ID, containingMethod, parentNode, nodeContent, previousControlNodes,
					previousDataNodes);

			ArrayList<NodeInfo> newPreviousControlNodes = new ArrayList<NodeInfo>();
			newPreviousControlNodes.add(nodeInfo);
			previousControlFlowNodeStack.add(newPreviousControlNodes);

			if (curMethodInfo != null) {
				if (MethodInfo.nodeTmpList == null) {
					MethodInfo.nodeTmpList = new ArrayList<NodeInfo>(1);
				}
				MethodInfo.nodeTmpList.add(nodeInfo);
			}
		}

		parentNodeStack.push(nodeInfo);
		// Logger.log("nodeContent: " + nodeContent);
		//
		// Logger.log("nodeInfo: " + nodeInfo);
		//
		// Logger.log("pNodeStack: " + parentNodeStack);

		if (parentNode != null) {
			parentNode.synchronizeControlDataNodes();
		}
		nodeInfo.synchronizeControlDataNodes();
		return nodeInfo;
	}

	public synchronized static void removeInvocNodeInfo(Node n, NodeInfo nodeInfo, Stack<NodeInfo> parentNodeStack,
			Stack<ArrayList<NodeInfo>> previousControlFlowNodeStack) {

		// Logger.log("cur MethodExpr: " + n);
		// Logger.log("parentNodeStack: " + parentNodeStack);
		// Logger.log("parentNodeStack size: " + parentNodeStack.size());

		parentNodeStack.pop();

		// while(previousControlFlowNodeStack.peek().get(0).ID!=nodeInfo.ID){
		// previousControlFlowNodeStack.pop();
		// }
	}

	public synchronized static boolean isInvocNode(Node n) {
		boolean isInvoc = false;
		if (n instanceof MethodCallExpr) {
			isInvoc = true;
		} else if (n instanceof ObjectCreationExpr) {
			isInvoc = true;
		}
		return isInvoc;
	}

	public synchronized static boolean isControlNode(Node n) {
		boolean isControl = false;
		if (n instanceof ConditionalExpr) {
			isControl = true;
		} else if (n instanceof ForStmt) {
			isControl = true;
		} else if (n instanceof ForEachStmt) {
			isControl = true;
		} else if (n instanceof DoStmt) {
			isControl = true;
		} else if (n instanceof WhileStmt) {
			isControl = true;
		} else if (n instanceof IfStmt) {
			isControl = true;
		} else if (n instanceof SwitchStmt) {
			isControl = true;
		}
		return isControl;
	}

}
