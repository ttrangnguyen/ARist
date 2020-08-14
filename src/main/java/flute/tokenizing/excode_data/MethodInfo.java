package flute.tokenizing.excode_data;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * This class saves method information.
 */
public class MethodInfo {
	String methodName ;
	TypeInfo typeInfo = null;
	FileInfo fileInfo = null;
	
	public Object parentInfo = null;
	
	public HashMap<String, String> shortScopeVariableMap = null;//new HashMap<String, String>(1,0.9f);
	public HashMap<String, String> fullScopeVariableMap = null;//new HashMap<String, String>(1, 0.9f); 

	
//	ArrayList<String> paramsList = null;
	String[] paramsList = null;
	
	public static ArrayList<MethodInvocInfo> methodInvocTmpList = null;
	public MethodInvocInfo[] methodInvocList = null;

	public static ArrayList<NodeInfo> nodeTmpList = null;
	
	public NodeInfo[] nodeList = null;



	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fileInfo == null) ? 0 : fileInfo.hashCode());
		result = prime * result
				+ ((methodName == null) ? 0 : methodName.hashCode());
		result = prime * result + Arrays.hashCode(paramsList);
		result = prime * result
				+ ((typeInfo == null) ? 0 : typeInfo.hashCode());
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
		MethodInfo other = (MethodInfo) obj;
		if (fileInfo == null) {
			if (other.fileInfo != null)
				return false;
		} else if (!fileInfo.equals(other.fileInfo))
			return false;
		if (methodName == null) {
			if (other.methodName != null)
				return false;
		} else if (!methodName.equals(other.methodName))
			return false;
		if (!Arrays.equals(paramsList, other.paramsList))
			return false;
		if (typeInfo == null) {
			if (other.typeInfo != null)
				return false;
		} else if (!typeInfo.equals(other.typeInfo))
			return false;
		return true;
	}

	/**
     * MethodInfo constructor.
     * @param methodName
     * @param paramsList
     * @param typeInfo
     * @param fileInfo
     */
	public MethodInfo(String methodName, ArrayList<String> paramsList, TypeInfo typeInfo, FileInfo fileInfo) {
		super();
		this.methodName = methodName;
		if (paramsList!=null)
		{
//			this.paramsList = new ArrayList<String>();
//			this.paramsList.addAll(paramsList);
			setParamsList(paramsList);
		}
		this.typeInfo = typeInfo;
		this.fileInfo = fileInfo;
		this.parentInfo = typeInfo;
	}

	public void synchronizeMethodInvocList(){
		if ((methodInvocTmpList!=null) && (methodInvocTmpList.size()>0))
		{
			methodInvocList = new  MethodInvocInfo[methodInvocTmpList.size()];
			for (int i=0; i<methodInvocTmpList.size(); i++){
				methodInvocList[i] = methodInvocTmpList.get(i);
			}
			methodInvocTmpList.clear();
		}
	}
	
	
	public void synchronizeNodeList(){
		if ((nodeTmpList!=null) && (nodeTmpList.size()>0))
		{
			nodeList = new  NodeInfo[nodeTmpList.size()];
			for (int i=0; i<nodeTmpList.size(); i++){
				nodeList[i] = nodeTmpList.get(i);
			}
			nodeTmpList.clear();
		}
	}
	
///**
// * @return the paramsList
// */
//public ArrayList<String> getParamsList() {
//	return paramsList;
//}

/**
 * @return  an array of MethodInvocInfo objects.
 * @see     MethodInvocInfo
 */
public MethodInvocInfo[] getMethodInvocList() {
	return methodInvocList;
}

/**
 * @param paramsList the list of parameters to set.
 */
public void setParamsList(ArrayList<String> paramsList) {
	if ((paramsList!=null)&&(paramsList.size()>0)){
		this.paramsList = new String[paramsList.size()];
		for (int i=0;i<paramsList.size();i++){
			this.paramsList[i] = paramsList.get(i).intern();
		}
	}
}

	public String getShortMethodSignature(){
		
		String name = methodName;
		
		String paramStr = null;
		if (paramsList==null){
			
		}
		else if (paramsList.length<=0){
			
		}
		else
		{
			StringBuffer paramSb = new StringBuffer();
			for (String p:paramsList){
				paramSb.append(p + ",");
			}
			paramStr = paramSb.toString();
			paramStr = new String(paramStr.substring(0,paramStr.length()-1));
		}
		String shortMethodSignature = name + "(" + paramStr + ")";	
		return shortMethodSignature;
	}

	/**
	 * 
	 * @return
	 */
	public String getFullMethodSignature(){
		String fullSignature = typeInfo.getFullName() + "." + getShortMethodSignature();
		return fullSignature;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MethodInfo [methodName=");
		builder.append(methodName);
//		builder.append(", paramsList=");
//		builder.append(paramsList);
//		builder.append(", typeInfo=");
//		builder.append(typeInfo);
//		builder.append(", fileInfo=");
//		builder.append(fileInfo);
		builder.append(", shortLocalVariableMap=");
		builder.append(shortScopeVariableMap);
		builder.append(", fullLocalVariableMap=");
		builder.append(fullScopeVariableMap);
		builder.append(", methodInvocList=");
		builder.append(methodInvocList);
		builder.append("]");
		return builder.toString();
	}


	
	
}
