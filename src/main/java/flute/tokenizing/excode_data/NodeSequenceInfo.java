/**
 * 
 */
package flute.tokenizing.excode_data;

import com.github.javaparser.Position;

import java.util.Optional;

public class NodeSequenceInfo implements Comparable<NodeSequenceInfo> {

	public static String alignToken = "\r\n";

	public Position beginPosition;
	public Position endPosition;

	public long nodeSeqID = -1;
	public int attachedTypeId = -1;
	public int attachedVarId = -1;

	public int attachedAccessId = -1;

	public short nodeType = NodeSequenceConstant.UNKNOWN;
	public short startEnd = NodeSequenceConstant.UNKNOWN;
	public short controlType = NodeSequenceConstant.UNKNOWN;

	public MethodInfo methodInfo;
	public TypeInfo typeInfo;
	public NodeInfo nodeInfo;

	// NodeSequenceInfo previousNode;
	String representStr = "";

	public NodeSequenceInfo setPosition(Optional<Position> beginPosition, Optional<Position> endPosition) {
		if (beginPosition != null && beginPosition.isPresent()) this.beginPosition = beginPosition.get();
		if (endPosition != null && endPosition.isPresent()) this.endPosition = endPosition.get();
		return this;
	}

	public NodeSequenceInfo setPosition(Optional<Position> position) {
		return setPosition(position, position);
	}

	/**
	 * @param attachedType
	 *            the attachedType to set
	 */
	public void setAttachedType(String attachedType) {

		if (attachedType != null) {
			String tmp = attachedType.intern();
			if (!NodeSequenceInfoMap.typeIDMap.containsKey(tmp)) {
				int id = NodeSequenceInfoMap.typeIDMap.size();
				NodeSequenceInfoMap.typeIDMap.put(tmp, id);
				NodeSequenceInfoMap.idTypeMap.put(id, tmp);
			}
			this.attachedTypeId = NodeSequenceInfoMap.typeIDMap.get(tmp);

		} else {
			this.attachedTypeId = -1;
		}
	}

	public String getAttachedType() {
	    String attachedType = NodeSequenceInfoMap.idTypeMap.get(this.attachedTypeId);
		//return attachedType != null ? attachedType : "<unk>";
	    return attachedType;
	}

	/**
	 * @param attachedVar
	 *            the attachedVar to set
	 */
	public void setAttachedVar(String attachedVar) {
		if (attachedVar != null) {
			String tmp = attachedVar.intern();
			if (!NodeSequenceInfoMap.varIDMap.containsKey(tmp)) {
				int id = NodeSequenceInfoMap.varIDMap.size();
				NodeSequenceInfoMap.varIDMap.put(tmp, id);
				NodeSequenceInfoMap.idVarMap.put(id, tmp);
			}
			this.attachedVarId = NodeSequenceInfoMap.varIDMap.get(tmp);

		} else {
			this.attachedVarId = -1;
		}
	}

	public String getAttachedVar() {
		return NodeSequenceInfoMap.idVarMap.get(this.attachedVarId);
	}

	/**
	 * @param attachedAccess
	 *            the attachedAccess to set
	 */
	public void setAttachedAccess(String attachedAccess) {
		if (attachedAccess != null) {
			String tmp = attachedAccess.intern();
			if (!NodeSequenceInfoMap.accessIDMap.containsKey(tmp)) {
				int id = NodeSequenceInfoMap.accessIDMap.size();
				NodeSequenceInfoMap.accessIDMap.put(tmp, id);
				NodeSequenceInfoMap.idAccessMap.put(id, tmp);
			}
			this.attachedAccessId = NodeSequenceInfoMap.accessIDMap.get(tmp);

		} else {
			this.attachedAccessId = -1;
		}
	}

	public String getAttachedAccess() {
		return NodeSequenceInfoMap.idAccessMap.get(this.attachedAccessId);
	}

	public NodeSequenceInfo(long nodeSeqID, long sameControlSeqID, NodeSequenceInfo previousNode, short nodeType,
			short startEnd, short controlType, MethodInfo methodInfo, TypeInfo typeInfo) {
		// this.nodeSeqID = nodeSeqID;
		// this.sameControlSeqID = sameControlSeqID;
		// this.previousNode = previousNode;
		this.nodeType = nodeType;
		this.startEnd = startEnd;
		this.controlType = controlType;
		// if (this.combinedInfo==null){
		// this.combinedInfo = new CombinedInfo();
		// }
		this.methodInfo = methodInfo;
		this.typeInfo = typeInfo;
		// combinedInfo = CombinedInfo.intern(combinedInfo);

		this.representStr = toStringSimple().intern();
	}

	public NodeSequenceInfo(long nodeSeqID, long sameControlSeqID, NodeSequenceInfo previousNode, short nodeType,
			short startEnd, short controlType, String attachedType, String attachedVar, MethodInfo methodInfo,
			TypeInfo typeInfo) {
		// this.nodeSeqID = nodeSeqID;
		// this.sameControlSeqID = sameControlSeqID;
		// this.previousNode = previousNode;
		this.nodeType = nodeType;
		this.startEnd = startEnd;
		this.controlType = controlType;
		setAttachedType(attachedType);
		setAttachedVar(attachedVar);
		// if (this.combinedInfo==null){
		// this.combinedInfo = new CombinedInfo();
		// }
		this.methodInfo = methodInfo;
		this.typeInfo = typeInfo;
		// combinedInfo = CombinedInfo.intern(combinedInfo);

		this.representStr = toStringSimple().intern();

	}

	public NodeSequenceInfo(long nodeSeqID, long sameControlSeqID, NodeSequenceInfo previousNode, short nodeType,
			short startEnd, short controlType, String attachedType, String attachedVar, String attachedAccess,
			MethodInfo methodInfo, TypeInfo typeInfo, NodeInfo nodeInfo) {
		// this.nodeSeqID = nodeSeqID;
		// this.sameControlSeqID = sameControlSeqID;
		// this.previousNode = previousNode;
		this.nodeType = nodeType;
		this.startEnd = startEnd;
		this.controlType = controlType;
		setAttachedType(attachedType);
		setAttachedVar(attachedVar);
		setAttachedAccess(attachedAccess);
		// if (this.combinedInfo==null){
		// this.combinedInfo = new CombinedInfo();
		// }
		this.methodInfo = methodInfo;
		this.typeInfo = typeInfo;
		// combinedInfo = CombinedInfo.intern(combinedInfo);

		this.nodeInfo = nodeInfo;

		this.representStr = toStringSimple().intern();

	}
	
	public NodeSequenceInfo clone() {
	    return new NodeSequenceInfo(nodeSeqID, -1, null, nodeType, startEnd, controlType, getAttachedType(),
	            getAttachedVar(), getAttachedAccess(), methodInfo, typeInfo, nodeInfo);
	}

	private static String getStringType(short type) {
		String tmp = null;
		switch (type) {
		case NodeSequenceConstant.UNKNOWN:
			tmp = "UNKNOWN";
			break;
		case NodeSequenceConstant.CLASS:
			tmp = "CLASS";
			break;
		case NodeSequenceConstant.METHOD:
			tmp = "METHOD";
			break;
		case NodeSequenceConstant.CONSTRUCTOR:
			tmp = "CONSTRUCTOR";
			break;
		case NodeSequenceConstant.CONTROL:
			tmp = "CONTROL";
			break;
		case NodeSequenceConstant.METHODACCESS:
			tmp = "M_ACCESS";
			break;
		case NodeSequenceConstant.FIELDACCESS:
			tmp = "F_ACCESS";
			break;
		case NodeSequenceConstant.BREAK:
			tmp = "BREAK";
			break;
		case NodeSequenceConstant.CONTINUE:
			tmp = "CONTINUE";
			break;
		case NodeSequenceConstant.TYPE:
			tmp = "TYPE";
			break;
		case NodeSequenceConstant.VAR:
			tmp = "VAR";
			break;
		
		case NodeSequenceConstant.OPERATOR:
			tmp = "OPERATOR";
			break;
		case NodeSequenceConstant.UOPERATOR:
			tmp = "UOPERATOR";
			break;
		case NodeSequenceConstant.LITERAL:
			tmp = "LITERAL";
			break;
		case NodeSequenceConstant.ASSIGN:
			tmp = "ASSIGN";
			break;
		case NodeSequenceConstant.CAST:
			tmp = "CAST";
			break;
		case NodeSequenceConstant.FIELD:
		    tmp = "FIELD";
		    break;
		case NodeSequenceConstant.IF:
			tmp = "IF";
			break;
		case NodeSequenceConstant.ELSE:
			tmp = "ELSE";
			break;
		case NodeSequenceConstant.COND:
			tmp = "COND";
			break;
		case NodeSequenceConstant.WHILE:
			tmp = "WHILE";
			break;
		case NodeSequenceConstant.FOR:
			tmp = "FOR";
			break;
		case NodeSequenceConstant.FOREACH:
			tmp = "FOREACH";
			break;
		case NodeSequenceConstant.DO:
			tmp = "DO";
			break;
		case NodeSequenceConstant.SWITCH:
			tmp = "SWITCH";
			break;
		case NodeSequenceConstant.CASE:
			tmp = "CASE";
			break;
		case NodeSequenceConstant.CASE_DEFAULT:
		    tmp = "CASE_DEFAULT";
		    break;
		case NodeSequenceConstant.TRY:
			tmp = "TRY";
			break;
		case NodeSequenceConstant.CATCH:
			tmp = "CATCH";
			break;
		case NodeSequenceConstant.FINALLY:
			tmp = "FINALLY";
			break;
		case NodeSequenceConstant.STATIC:
			tmp = "STATIC";
			break;
		case NodeSequenceConstant.SYNC:
			tmp = "SYNC";
			break;
		case NodeSequenceConstant.EXPR:
			tmp = "EXPR";
			break;
		case NodeSequenceConstant.EXPL_CONSTR:
			tmp = "EXPL_CONSTR";
			break;
		case NodeSequenceConstant.CONSTR:
			tmp = "CONSTR";
			break;
		case NodeSequenceConstant.PARAM:
            tmp = "PARAM";
            break;
		case NodeSequenceConstant.NPARAM:
			tmp = "NPARAM";
			break;
		case NodeSequenceConstant.START:
			tmp = "START";
			break;
		case NodeSequenceConstant.END:
			tmp = "END";
			break;
		case NodeSequenceConstant.RETURN:
            tmp = "RETURN";
            break;
		case NodeSequenceConstant.THROW:
			tmp = "THROW";
			break;
		case NodeSequenceConstant.YIELD:
            tmp = "YIELD";
            break;
		default:
			break;
		}

		return tmp;
	}

	private String toStringClass() {
		StringBuilder builder = new StringBuilder();

		// if (startEnd == NodeSequenceConstant.END){
		// if (alignToken.contains("\t"))
		// {
		// alignToken = new String(alignToken.substring(0,
		// alignToken.lastIndexOf("\t")));
		// }
		// }
		builder.append(alignToken + "CLASS{" + getStringType(startEnd) + "}");
		// builder.append(alignToken + "CLASS ");
		// if (startEnd == NodeSequenceConstant.START){
		// alignToken += "\t";
		// }
		return builder.toString();
	}
	
	private String toStringEnum() {
        StringBuilder builder = new StringBuilder();
        builder.append(alignToken + "ENUM{" + getStringType(startEnd) + "}");
        return builder.toString();
    }

	private String toStringMethodConstructor() {
		// StringBuilder builder = new StringBuilder();

		// if (startEnd == NodeSequenceConstant.END){
		// alignToken = new String(alignToken.substring(0,
		// alignToken.lastIndexOf("\t")));
		// }
		// builder.append(alignToken + getMethodStr(startEnd));
		// builder.append(alignToken);
		// if (startEnd == NodeSequenceConstant.START){
		// alignToken += "\t";
		// }
		return alignToken + "CONSTRUCTOR";
	}

	private String toStringMethod() {
		return alignToken + representStr;
	}
	
	private String toStringField() {
	    return alignToken + representStr;
	}

	public static String getMethodStr(short startEnd) {
		return "METHOD " + getStringType(startEnd);
	}

	private String toStringControl() {
		StringBuilder builder = new StringBuilder();

		// if (startEnd == NodeSequenceConstant.END){
		// alignToken = new String(alignToken.substring(0,
		// alignToken.lastIndexOf("\t")));
		// }
		// builder.append(alignToken + getStringType(controlType) + " " +
		// getStringType(startEnd));

		if (startEnd == NodeSequenceConstant.START) {
			// builder.append(alignToken + getStringType(controlType));
		} else if (startEnd == NodeSequenceConstant.END) {
			// builder.append(alignToken + "END");
			// builder.append(alignToken);
		}
		// if (startEnd == NodeSequenceConstant.START){
		// alignToken += "\t";
		// }
		builder.append(alignToken + getStringType(controlType));
		return builder.toString();
	}

	int numOfArgs = 0;

	public void setNumOfArgs(int numOfAgrs) {
		this.numOfArgs = numOfAgrs;
	}

	public int getNumOfArgs() {
		return numOfArgs;
	}

	String retType = "NONE";

	public void setRetType(String retType) {
		this.retType = retType;
	}

	public String getRetType() {
		return retType;
	}

	private String toStringMethodAccess() {
		StringBuilder builder = new StringBuilder();
		// getAttachedAccess returns method name
		// builder.append(alignToken + "M_ACCESS(" + getAttachedType() + "," +
		// getAttachedAccess() + "," + retType + ","
		// + numOfArgs + ") ");
		String type = getAttachedType();
        if (type == null) type = "<unk>";
		
		builder.append(
				alignToken + "M_ACCESS(" + type + "," + getAttachedAccess() + "," + numOfArgs + ")");

		return builder.toString();
	}

	private String toStringConstructorCall() {
		StringBuilder builder = new StringBuilder();

		// builder.append(alignToken + "C_CALL(" + getAttachedVar() +"," +
		// getAttachedAccess() + ") ");
		// builder.append(alignToken + "C_CALL(" + getAttachedType() +"," +
		// getAttachedAccess() + ") ");

		builder.append(alignToken + "C_CALL(" + getAttachedType() + "," + getAttachedAccess() + ")");

		return builder.toString();
	}

	private String toStringFieldAccess() {
		StringBuilder builder = new StringBuilder();
		String type = getAttachedType();
		if (type == null) type = "<unk>";
		
		builder.append(alignToken + "F_ACCESS(" + type + "," + getAttachedAccess() + ")");
		return builder.toString();
	}

	private String toStringAssign() {
		StringBuilder builder = new StringBuilder();
		builder.append(alignToken + "ASSIGN(" + getAttachedType() + ")");
		return builder.toString();
	}

	private String toStringLiteral() {
		StringBuilder builder = new StringBuilder();
		// builder.append(alignToken + "LIT(" + getStringType(controlType) +"," +
		// getAttachedType() + ")" );
		builder.append(alignToken + "LIT(" + getAttachedType() + ")");
		return builder.toString();
	}

	private String toStringType() {
		StringBuilder builder = new StringBuilder();
		builder.append(alignToken + "TYPE(" + getAttachedType() + ")");
		return builder.toString();
	}

	private String toStringVar() {
		StringBuilder builder = new StringBuilder();
		String type = getAttachedType();
		if (type == null) type = "<unk>"; 
		
		String varName = getAttachedVar();
		builder.append(alignToken + "VAR(" + type + "," + varName + ")");
		// builder.append(alignToken + "VAR(" + type + ", var)");
		// builder.append(alignToken + "VAR(" + type + ")");
		return builder.toString();
	}

	private String toStringOperator() {
		StringBuilder builder = new StringBuilder();
		builder.append(alignToken + "OP(" + getAttachedType() + ")");
		return builder.toString();
	}

	private String toStringUOperator() {
		StringBuilder builder = new StringBuilder();
		builder.append(alignToken + "UOP(" + getAttachedType() + ")");
		return builder.toString();
	}

	private String toStringBreakCastContinue() {
		StringBuilder builder = new StringBuilder();
		builder.append(alignToken + getStringType(nodeType) + "(" + getAttachedType() + ")");
		return builder.toString();
	}

	public String toStringSimple() {
		String tmp = this.toString();
		tmp = tmp.replaceAll("\r\n", "");
		tmp = tmp.replaceAll("\t", "");
		tmp = tmp.trim();
		return tmp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	// TODO SONNV
	@Override
	public String toString() {
		String tmp = null;
		if (nodeType == NodeSequenceConstant.CLASS) {
			tmp = toStringClass();
		} else if ((nodeType == NodeSequenceConstant.ENUM)) {
		    tmp = toStringEnum();
		} else if ((nodeType == NodeSequenceConstant.METHOD)) {
            tmp = toStringMethod();
		} else if ((nodeType == NodeSequenceConstant.CONSTRUCTOR)) {
			tmp = toStringMethodConstructor();
		} else if ((nodeType == NodeSequenceConstant.CONTROL)) {
			tmp = toStringControl();
		} else if ((nodeType == NodeSequenceConstant.METHODACCESS)) {
			tmp = toStringMethodAccess();
		} else if ((nodeType == NodeSequenceConstant.CONSTRUCTORCALL)) {
			tmp = toStringConstructorCall();
		} else if ((nodeType == NodeSequenceConstant.FIELDACCESS)) {
			tmp = toStringFieldAccess();
		} else if ((nodeType == NodeSequenceConstant.BREAK) || (nodeType == NodeSequenceConstant.CAST)
                || (nodeType == NodeSequenceConstant.CONTINUE)) {
            tmp = toStringBreakCastContinue();
		} else if ((nodeType == NodeSequenceConstant.TYPE)) {
            tmp = toStringType();
		} else if ((nodeType == NodeSequenceConstant.VAR)) {
            tmp = toStringVar();
		} else if ((nodeType == NodeSequenceConstant.OPERATOR)) {
            tmp = toStringOperator();
        } else if ((nodeType == NodeSequenceConstant.UOPERATOR)) {
            tmp = toStringUOperator();
		} else if ((nodeType == NodeSequenceConstant.LITERAL)) {
            tmp = toStringLiteral();
		} else if ((nodeType == NodeSequenceConstant.ASSIGN)) {
			tmp = toStringAssign();
		} else if ((nodeType == NodeSequenceConstant.FIELD)) {
		    tmp = toStringField();
		} else if ((nodeType == NodeSequenceConstant.CATCH)) {
            tmp = toStringCatch();
		} else if ((nodeType == NodeSequenceConstant.START)) {
            tmp = toStringSTSTM();
        } else if ((nodeType == NodeSequenceConstant.END)) {
            tmp = toStringENSTM();
        } else if ((nodeType == NodeSequenceConstant.NODE_PART)) {
            tmp = toStringAGM_MT();
		} else if ((nodeType == NodeSequenceConstant.OPBK)) {
			tmp = toStringOPBLK();
		} else if ((nodeType == NodeSequenceConstant.CLBK)) {
			tmp = toStringCLBLK();
		} else if ((nodeType == NodeSequenceConstant.SEPA)) {
			tmp = toStringSEPA();
		} else if ((nodeType == NodeSequenceConstant.RETURN)) {
			tmp = toStringReturn();
		} else if ((nodeType == NodeSequenceConstant.YIELD)) {
		    tmp = toStringYield();
		} else if ((nodeType == NodeSequenceConstant.CONDITIONAL_EXPR)) {
			tmp = toStringConExpr();
		}
		return tmp;
	}

	private String toStringCatch() {
		return "";
	}

	private String toStringReturn() {
		return "RETURN";
	}
	
	private String toStringYield() {
        return "YIELD";
    }

	public boolean isVarNode() {
		return (nodeType == NodeSequenceConstant.VAR);
	}

	private String toStringVarRecommend() {
		StringBuilder builder = new StringBuilder();

		// Logger.log(" methodInfo.shortScopeVariableMap: " +
		// methodInfo.shortScopeVariableMap);
		// Logger.log(" typeInfo.var: " + typeInfo.shortScopeVariableMap);
		// Logger.log( "VAR(" + getAttachedType() + "," + "var"+ ")" );

		String type = null;
		if (getAttachedType() != null) {
			type = getAttachedType().trim();
		}

		String varName = getAttachedVar();

		builder.append(alignToken + "VAR(" + type + "," + varName + ")");

		return builder.toString();
	}

	public String toStringRecommend() {
		String tmp = null;
		if (nodeType == NodeSequenceConstant.CLASS) {
			tmp = toStringClass();
		} else if ((nodeType == NodeSequenceConstant.ENUM)) {
		    tmp = toStringEnum();
		} else if ((nodeType == NodeSequenceConstant.CONSTRUCTOR) || (nodeType == NodeSequenceConstant.METHOD)) {
			tmp = toStringMethodConstructor();
		} else if ((nodeType == NodeSequenceConstant.CONTROL)) {
			tmp = toStringControl();
		} else if ((nodeType == NodeSequenceConstant.METHODACCESS)) {
			tmp = toStringMethodAccess();
		} else if ((nodeType == NodeSequenceConstant.CONSTRUCTORCALL)) {
			tmp = toStringConstructorCall();
		} else if ((nodeType == NodeSequenceConstant.FIELDACCESS)) {
			tmp = toStringFieldAccess();
		} else if ((nodeType == NodeSequenceConstant.TYPE)) {
            tmp = toStringType();
		} else if ((nodeType == NodeSequenceConstant.VAR)) {
            tmp = toStringVarRecommend();
		} else if ((nodeType == NodeSequenceConstant.OPERATOR)) {
            tmp = toStringOperator();
		} else if ((nodeType == NodeSequenceConstant.UOPERATOR)) {
            tmp = toStringUOperator();
		} else if ((nodeType == NodeSequenceConstant.LITERAL)) {
            tmp = toStringLiteral();
		} else if ((nodeType == NodeSequenceConstant.ASSIGN)) {
			tmp = toStringAssign();
		} else if ((nodeType == NodeSequenceConstant.BREAK) || (nodeType == NodeSequenceConstant.CAST)
				|| (nodeType == NodeSequenceConstant.CONTINUE)) {
			tmp = toStringBreakCastContinue();
		} else if ((nodeType == NodeSequenceConstant.FIELD)) {
		    tmp = toStringField();
		} else if ((nodeType == NodeSequenceConstant.CONDITIONAL_EXPR)) {
			tmp = toStringConExpr();
		}
		return tmp;
	}

	public String toStringRecommendSimple() {
		String tmp = this.toStringRecommend();
		tmp = tmp.replaceAll("\r\n", "");
		tmp = tmp.replaceAll("\t", "");
		tmp = tmp.trim();
		return tmp;
	}

	public boolean equalsRecommend(Object obj) {
		String representStrTmp = toStringRecommendSimple();
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NodeSequenceInfo other = (NodeSequenceInfo) obj;
		if (representStrTmp == null) {
			if (other.toStringRecommendSimple() != null)
				return false;
		} else if (!representStrTmp.equals(other.toStringRecommendSimple()))
			return false;
		return true;
	}

	public boolean isScopeConstruct() {
		boolean scopeConstruct = false;
		//
		// if (
		// (nodeType == NodeSequenceConstant.CLASS)
		// ||(nodeType == NodeSequenceConstant.METHOD)
		// ||(nodeType == NodeSequenceConstant.CONTROL)
		//
		// )
		// {
		// if ((startEnd == NodeSequenceConstant.START)||(startEnd ==
		// NodeSequenceConstant.END))
		// {
		// scopeConstruct = true;
		// }
		// }
		return scopeConstruct;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		representStr = toStringSimple();

		final int prime = 31;
		int result = 1;
		result = prime * result + ((representStr == null) ? 0 : representStr.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		representStr = toStringSimple();
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NodeSequenceInfo other = (NodeSequenceInfo) obj;
		if (representStr == null) {
			if (other.representStr != null)
				return false;
		} else if (!representStr.equals(other.representStr))
			return false;
		return true;
	}

	@Override
	public int compareTo(NodeSequenceInfo o) {
		Integer tmpCur = hashCode();
		Integer tmpO = o.hashCode();
		return tmpCur.compareTo(tmpO);
	}

	public NodeSequenceInfo() {
		this.representStr = "";
	}

	public static NodeSequenceInfo getOPBLK() {
		NodeSequenceInfo op = new NodeSequenceInfo();
		op.nodeType = NodeSequenceConstant.OPBK;
		return op;
	}

	public static NodeSequenceInfo getCLBLK() {
		NodeSequenceInfo op = new NodeSequenceInfo();
		op.nodeType = NodeSequenceConstant.CLBK;
		return op;
	}

	public static NodeSequenceInfo getSTSTM(short stm) {
		NodeSequenceInfo op = new NodeSequenceInfo();
		op.nodeType = NodeSequenceConstant.START;
		op.representStr = "STSTM{" + getStringType(stm) + "}";
		return op;
	}

	public static NodeSequenceInfo getENSTM(short stm) {
		NodeSequenceInfo op = new NodeSequenceInfo();
		op.nodeType = NodeSequenceConstant.END;
		op.representStr = "ENSTM{" + getStringType(stm) + "}";
		return op;
	}

	public static NodeSequenceInfo getPartNode(short endMt, boolean isOpen) {
		NodeSequenceInfo op = new NodeSequenceInfo();
		op.nodeType = NodeSequenceConstant.NODE_PART;
		if (endMt == NodeSequenceConstant.CASE_PART)
			op.representStr = "CASE_PART";
		else {
			if (isOpen)
				op.representStr = "OPEN_PART";
			else
				op.representStr = "CLOSE_PART";
		}
		return op;
	}

	private String toStringAGM_MT() {
		return alignToken + representStr;
	}

	private String toStringSTSTM() {
		return alignToken + representStr;
	}

	private String toStringENSTM() {
		return alignToken + representStr;
	}

	private String toStringOPBLK() {
		return alignToken + "OPBLK";
	}

	private String toStringCLBLK() {
		return alignToken + "CLBLK";
	}

	private String toStringSEPA() {
		return alignToken + representStr;
	}

	private String toStringConExpr() {
		return alignToken + "CEXP";
	}

	public static NodeSequenceInfo getStartMethod(String type) {
		NodeSequenceInfo op = new NodeSequenceInfo();
		op.nodeType = NodeSequenceConstant.METHOD;
		op.representStr = "METHOD{" + type + "}";
		return op;
	}

	public static NodeSequenceInfo getEndMethod() {
		NodeSequenceInfo op = new NodeSequenceInfo();
		op.nodeType = NodeSequenceConstant.METHOD;
		op.representStr = "ENDMETHOD";
		return op;
	}
	
	public static NodeSequenceInfo getFieldDeclaration() {
        NodeSequenceInfo op = new NodeSequenceInfo();
        op.nodeType = NodeSequenceConstant.FIELD;
        op.representStr = "FIELD_DECLARE";
        return op;
    }

	public static NodeSequenceInfo getSEPA(short sepa, char type) {
		NodeSequenceInfo op = new NodeSequenceInfo();
		op.nodeType = NodeSequenceConstant.SEPA;
		op.representStr = "SEPA" + "(" + type + ")";
		return op;
	}

	public static NodeSequenceInfo getConditionalExpr() {
		NodeSequenceInfo op = new NodeSequenceInfo();
		op.nodeType = NodeSequenceConstant.CONDITIONAL_EXPR;
		op.representStr = "CEXP";
		return op;
	}
}
