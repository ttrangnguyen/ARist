/**
 * 
 */
package tokenizing.excode_data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class TypeInfo {

	public String packageDec ;
	public String typeName ;
	public String accessModType = "private";
//	public ArrayList<TypeInfo> extendTypeList = null;
//	public ArrayList<TypeInfo> childTypeList = null;  

	public ArrayList<String> extendTypeStrList = null;
	public ArrayList<String> implementTypeStrList = null;
	public LinkedHashMap<String, ArrayList<MethodInfo>> methodDecMap = null;//new LinkedHashMap<String, ArrayList<MethodInfo>>();
	public ArrayList<MethodInvocInfo> methodInvocList = null;// new ArrayList<MethodInvocInfo>(1);
	
	public HashMap<String, String> shortScopeVariableMap = null;//new HashMap<String, String>(1);
	public HashMap<String, String> fullScopeVariableMap = null;//new HashMap<String, String>(1);

	public FileInfo fileInfo = null;
	public Object parentInfo = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		

	}
	
	
	public String getFullName(){
		return packageDec +"." + typeName;
	}

	

	

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
				+ ((typeName == null) ? 0 : typeName.hashCode());
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
		TypeInfo other = (TypeInfo) obj;
		if (fileInfo == null) {
			if (other.fileInfo != null)
				return false;
		} else if (!fileInfo.equals(other.fileInfo))
			return false;
		if (typeName == null) {
			if (other.typeName != null)
				return false;
		} else if (!typeName.equals(other.typeName))
			return false;
		return true;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TypeInfo [packageDec=");
		builder.append(packageDec);
		builder.append(", typeName=");
		builder.append(typeName);
//		builder.append(", typePath=");
//		builder.append(typePath);
//		builder.append(", classType=");
//		builder.append(accessModType);
//		builder.append(", extendTypeStrList=");
//		builder.append(extendTypeStrList);
//		builder.append(", implementTypeStrList=");
//		builder.append(implementTypeStrList);
//		builder.append(", methodDecMap=");
//		builder.append(methodDecMap);
//		builder.append(", methodInvocList=");
//		builder.append(methodInvocList);
		builder.append(", shortGlobalVariableMap=");
		builder.append(shortScopeVariableMap);
		builder.append(", fullGlobalVariableMap=");
		builder.append(fullScopeVariableMap);
		builder.append("]");
		return builder.toString();
	}
	
	
	
}
