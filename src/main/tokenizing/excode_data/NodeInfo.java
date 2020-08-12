/**
 * 
 */
package tokenizing.excode_data;

import java.util.ArrayList;

public class NodeInfo {
	
	public static final int METHODINVOC_TYPE = 0;
	public static final int CONTROL_TYPE = 1;
	public static final int DATA_TYPE = 2;
	public static final int ASSIGN_TYPE = 3;

	
	public int nodeType = METHODINVOC_TYPE; 
	public long ID = 0;
	
	public MethodInfo containingMethod;
	public NodeInfo parentNode;
	
	public Object nodeContent;
	
	public NodeInfo[] previousControlNodes;
	public NodeInfo[] previousDataNodes;

	public static ArrayList<NodeInfo> previousControlNodesTmp = new ArrayList<NodeInfo>();
//	public static ArrayList<NodeInfo> previousDataNodesTmp = new ArrayList<NodeInfo>();

	public NodeInfo() {
	}
	
	

	public NodeInfo(int nodeType, long ID, MethodInfo containingMethod,
			NodeInfo parentNode, Object nodeContent,
			ArrayList<NodeInfo> previousControlNodes,
			ArrayList<NodeInfo> previousDataNodes) {
		super();
		this.nodeType = nodeType;
		this.ID = ID;
		this.containingMethod = containingMethod;
		this.parentNode = parentNode;
		this.nodeContent = nodeContent;
		
		if (previousControlNodes!=null)
		{
			previousControlNodesTmp = previousControlNodes;
		}
//		if (previousDataNodes!=null)
//		{
//			previousDataNodesTmp = previousDataNodes;
//		}
		synchronizeControlDataNodes();
	}


	public void synchronizeControlDataNodes(){
		synchronizeControlNodes();
//		synchronizeDataNodes();
	}

	public void synchronizeControlNodes(){
		if ((previousControlNodesTmp!=null)&&(previousControlNodesTmp.size()>0)){
			previousControlNodes = new NodeInfo[previousControlNodesTmp.size()];
			for (int i=0; i<previousControlNodesTmp.size();i++){
				previousControlNodes[i] = previousControlNodesTmp.get(i);
			}
		}
		previousControlNodesTmp.clear();
	}
	
//	public void synchronizeDataNodes(){
//		if ((previousDataNodesTmp!=null)&&(previousDataNodesTmp.size()>0)){
//			previousDataNodes = new NodeInfo[previousDataNodesTmp.size()];
//			for (int i=0; i<previousDataNodesTmp.size();i++){
//				previousDataNodes[i] = previousDataNodesTmp.get(i);
//			}
//		}
//		previousDataNodesTmp.clear();
//	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}



	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("\r\n\tNodeInfo [nodeType=");
		builder.append(nodeType);
		builder.append(", ID=");
		builder.append(ID);

		
		builder.append(", nodeContent=");
		if (nodeContent instanceof ControlInfo){
			builder.append((ControlInfo)nodeContent);
		}
		else if (nodeContent instanceof MethodInvocInfo){
			builder.append(((MethodInvocInfo)nodeContent).methodName);
		}
		builder.append(", containingMethod=");
		if (containingMethod!=null)
		{
			builder.append(containingMethod.getFullMethodSignature());
		}
		builder.append(", parentNode=");
		builder.append(parentNode);
//		builder.append(", previousControlNodes=");
//		builder.append(previousControlNodes);
//		builder.append(", previousDataNodes=");
//		builder.append(previousDataNodes);
		builder.append("]");
		return builder.toString();
	}



	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (ID ^ (ID >>> 32));
		return result;
	}



	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NodeInfo other = (NodeInfo) obj;
		if (ID != other.ID)
			return false;
		return true;
	}

}
