/**
 * 
 */
package flute.tokenizing.excode_data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import flute.utils.logging.Logger;
import flute.utils.StringUtils;

public class SystemTableCrossProject {
	public HashMap<String, PackageInfo> packageMap = new HashMap<String, PackageInfo>();
	public ArrayList<TypeInfo> typeList = new ArrayList<TypeInfo>(1);
	public ArrayList<FileInfo> fileList = new ArrayList<FileInfo>(1);
	public HashMap<String, ArrayList<MethodInfo>> methodMap = new HashMap<String, ArrayList<MethodInfo>>();
	public int numMethods = 0;

	public HashMap<String, TypeInfo> typeFullMap = new HashMap<String, TypeInfo>();
	public HashMap<String, ArrayList<MethodInfo>> methodFullMap = new HashMap<String, ArrayList<MethodInfo>>();

	HashMap<String, MethodInfo> methodNameMethodInfoMap = new HashMap<String, MethodInfo>();

	public static HashMap<NodeSequenceInfo, Integer> nodeSeqStrDic = new HashMap<NodeSequenceInfo, Integer>();
	public static HashMap<Integer, NodeSequenceInfo> nodeSeqStrRevDic = new HashMap<Integer, NodeSequenceInfo>();

	String methodStartStr = NodeSequenceInfo.getMethodStr(NodeSequenceConstant.START);
	String methodEndStr = NodeSequenceInfo.getMethodStr(NodeSequenceConstant.END);
	int methodStartIdx = -1;
	int methodEndIdx = -1;

	/**
	 * @param args
	 */
	public synchronized static void main(String[] args) {

	}

	public void buildMethodMap() {
		methodMap = new HashMap<String, ArrayList<MethodInfo>>();
		for (TypeInfo typeInfo : typeList) {
			LinkedHashMap<String, ArrayList<MethodInfo>> methodDecMap = new LinkedHashMap<String, ArrayList<MethodInfo>>(
					1);
			if (typeInfo.methodDecMap != null) {
				methodDecMap.putAll(typeInfo.methodDecMap);
			}
			for (String methodName : methodDecMap.keySet()) {
				ArrayList<MethodInfo> methodList = new ArrayList<MethodInfo>();
				methodList.addAll(methodDecMap.get(methodName));
				numMethods += methodList.size();
				if (methodMap.containsKey(methodName)) {
					methodMap.get(methodName).addAll(methodList);
				} else {
					methodMap.put(methodName.intern(), methodList);
				}
			}
		}
	}

	public void buildTypeFullMap() {
		for (String packageName : packageMap.keySet()) {
			PackageInfo packageInfo = packageMap.get(packageName);
			for (TypeInfo typeInfo : packageInfo.typeList) {
				String fullTypeName = typeInfo.getFullName();
				typeFullMap.put(fullTypeName.intern(), typeInfo);
			}
		}
	}

	public void buildMethodFullMap() {
		for (String fullTypeName : typeFullMap.keySet()) {
			TypeInfo typeInfo = typeFullMap.get(fullTypeName);
			HashMap<String, ArrayList<MethodInfo>> methodDecMap = new HashMap<String, ArrayList<MethodInfo>>();
			if (typeInfo.methodDecMap != null) {
				methodDecMap.putAll(typeInfo.methodDecMap);
			}
			for (String methodName : methodDecMap.keySet()) {
				String fullMethodName = fullTypeName + "." + methodName;
				methodFullMap.put(fullMethodName.intern(), methodDecMap.get(methodName));
			}
		}
	}

	public void buildFeasibleTypeListForFiles() {
		for (FileInfo fileInfo : fileList) {
			buildFeasibleTypeList(fileInfo);
		}
	}

	/**
	 * 
	 * @param fileInfo
	 */
	public void buildFeasibleTypeList(FileInfo fileInfo) {
		LinkedHashSet<String> feasibleTypeList = new LinkedHashSet<String>();
		String packageDec = fileInfo.packageDec;
		ArrayList<String> importList = fileInfo.importList;

		// fileInfo.typeInfoList;
		for (TypeInfo typeInfo : fileInfo.typeInfoList) {
			feasibleTypeList.add(typeInfo.getFullName());
		}

		for (String typeFullName : typeFullMap.keySet()) {
			// TypeInfo typeInfo = typeFullMap.get(typeFullName);
			// if(!(typeInfo.accessModType.equals("private")))
			{
				String tmp = new String(typeFullName.substring(0, typeFullName.lastIndexOf(".")));
				if (tmp.equals(packageDec)) {
					feasibleTypeList.add(typeFullName);
				}
			}
		}

		for (String importDec : importList) {
			String tmp = importDec;
			if (importDec.endsWith(".*")) {
				tmp = importDec.substring(0, importDec.length() - 2);

				for (String typeFullName : typeFullMap.keySet()) {
					TypeInfo typeInfo = typeFullMap.get(typeFullName);

					if (!(typeInfo.accessModType.equals("private"))) {
						if (typeFullName.startsWith(tmp + ".")) {
							feasibleTypeList.add(typeFullName);
						}
					}
				}
			} else {
				for (String typeFullName : typeFullMap.keySet()) {
					TypeInfo typeInfo = typeFullMap.get(typeFullName);

					if (!(typeInfo.accessModType.equals("private"))) {
						if (typeFullName.startsWith(tmp)) {
							feasibleTypeList.add(typeFullName);
						}
					}
				}
				feasibleTypeList.add(importDec);
			}
		}
		fileInfo.feasibleTypeList.addAll(feasibleTypeList);
	}

	public void buildTypeFullVariableMap() {
		for (String typeFullName : typeFullMap.keySet()) {
			TypeInfo typeInfo = typeFullMap.get(typeFullName);

			HashMap<String, String> shortGlobalVariableMap = typeInfo.shortScopeVariableMap;

			FileInfo fileInfo = typeInfo.fileInfo;
			ArrayList<String> feasibleTypeList = fileInfo.feasibleTypeList;
			// Logger.logDebug("typeFullName: " + typeFullName + "\t" + fileInfo.fileName );

			if (shortGlobalVariableMap != null) {
				for (String variableName : shortGlobalVariableMap.keySet()) {
					String shortTypeTmp = shortGlobalVariableMap.get(variableName);

					String shortType = shortTypeTmp;
					if (shortTypeTmp.contains("[")) {
						shortType = new String(shortTypeTmp.substring(0, shortTypeTmp.indexOf("[")));
					}

					ArrayList<String> foundTypeFullList = getFullTypeList(shortType, feasibleTypeList);

					if (typeInfo.fullScopeVariableMap == null) {
						typeInfo.fullScopeVariableMap = new HashMap<String, String>();
					}

					if (foundTypeFullList.size() > 0) {
						typeInfo.fullScopeVariableMap.put(variableName.intern(), foundTypeFullList.get(0));
					} else {
						typeInfo.fullScopeVariableMap.put(variableName.intern(), Constants.UNKNOWN_TYPE);

					}

				}
			}
			// Logger.logDebug(" typeInfo: " + typeInfo);

			LinkedHashMap<String, ArrayList<MethodInfo>> methodDecMap = new LinkedHashMap<String, ArrayList<MethodInfo>>();
			if (typeInfo.methodDecMap != null) {
				methodDecMap.putAll(typeInfo.methodDecMap);
			}
			for (String methodName : methodDecMap.keySet()) {
				ArrayList<MethodInfo> methodList = methodDecMap.get(methodName);
				// Logger.logDebug("\t methodList: " + methodName);

				for (MethodInfo methodInfo : methodList) {
					HashMap<String, String> shortLocalVariableMap = methodInfo.shortScopeVariableMap;
					if (shortLocalVariableMap != null) {
						for (String localVarName : shortLocalVariableMap.keySet()) {
							String shortLocalTypeTmp = shortLocalVariableMap.get(localVarName);
							String shortLocalType = shortLocalTypeTmp;
							if (shortLocalTypeTmp.contains("[")) {
								shortLocalType = shortLocalTypeTmp.substring(0, shortLocalTypeTmp.indexOf("["));
							}
							ArrayList<String> foundLocalTypeFullList = getFullTypeList(shortLocalType,
									feasibleTypeList);
							if (foundLocalTypeFullList.size() != 1) {
								if (!shortLocalType.equals("String")) {
									// Logger.logDebug("\t fileInfo.importList" + fileInfo.importList);
									// Logger.logDebug("\t feasibleTypeList" + feasibleTypeList);
									// Logger.logDebug("\t\t Local Variable: " + shortLocalType + "\t" +
									// foundLocalTypeFullList);
								}
							}

							if (methodInfo.fullScopeVariableMap == null) {
								methodInfo.fullScopeVariableMap = new HashMap<String, String>(1, 0.9f);
							}
							if (foundLocalTypeFullList.size() > 0) {
								methodInfo.fullScopeVariableMap.put(localVarName.intern(),
										foundLocalTypeFullList.get(0));
							} else {
								methodInfo.fullScopeVariableMap.put(localVarName.intern(), Constants.UNKNOWN_TYPE);

							}

						}
					}
					// Logger.logDebug("\t\tmethodInfo: "+methodInfo);

				}
			}
		}
	}

	/**
	 * 
	 * @param shortType
	 * @param feasibleTypeList
	 * @return
	 */
	public ArrayList<String> getFullTypeList(String shortType, ArrayList<String> feasibleTypeList) {
		ArrayList<String> foundTypeFullList = new ArrayList<String>();

		for (String feasibleType : feasibleTypeList) {
			if (feasibleType.endsWith("." + shortType)) {
				foundTypeFullList.add(feasibleType);
			} else if (feasibleType.equals(shortType)) {
				foundTypeFullList.add(feasibleType);
			}
		}
		if (foundTypeFullList.size() == 0) {
			if (isQualifiedName(shortType)) {
				foundTypeFullList.add(shortType);
			}
		}

		return foundTypeFullList;
	}

	public void buildMethodInvocListForMethods() {
		for (TypeInfo typeInfo : typeList) {
			HashMap<String, String> fullGlobalVariableMap = typeInfo.fullScopeVariableMap;

			// Logger.logDebug("typeInfo:" + typeInfo.typeName );//+
			// "\tfullGlobalVariableMap:" + fullGlobalVariableMap);
			LinkedHashMap<String, ArrayList<MethodInfo>> methodDecMap = typeInfo.methodDecMap;
			String fullTypeName = typeInfo.getFullName();
			if (methodDecMap != null) {
				for (String methodDecName : methodDecMap.keySet()) {
					ArrayList<MethodInfo> methodList = methodDecMap.get(methodDecName);
					for (MethodInfo methodInfo : methodList) {
						HashMap<String, String> fullLocalVariableMap = methodInfo.fullScopeVariableMap;
						// Logger.logDebug("\tmethodInfo: " + methodInfo.methodName +
						// "\tfullLocalVariableMap:" + fullLocalVariableMap);
						MethodInvocInfo[] methodInvocList = methodInfo.getMethodInvocList();
						// Logger.logDebug( "\t\tmethodInfo.nodeList: " + methodInfo.nodeList);
						if (methodInvocList != null) {
							for (MethodInvocInfo methodInvoc : methodInvocList) {
								String typeName = fullTypeName;
								if (!methodInvoc.isInner) {
									String varName = methodInvoc.varName;

									if ((fullLocalVariableMap != null) && (fullLocalVariableMap.containsKey(varName))) {
										typeName = fullLocalVariableMap.get(varName);
									} else if ((fullGlobalVariableMap != null)
											&& (fullGlobalVariableMap.containsKey(varName))) {
										typeName = fullGlobalVariableMap.get(varName);
									} else {
										typeName = "_UNFOUNDTYPE_";
									}

								}
								methodInvoc.methodQuanlifiedName = (typeName + "." + methodInvoc.methodName).intern();

								ArrayList<String> paramTypes = new ArrayList<String>();
								if (methodInvoc.parameterList != null) {
									for (String paramName : methodInvoc.parameterList) {
										String paramType = "_UNFOUNDTYPE_";
										if (fullLocalVariableMap != null) {
											if (fullLocalVariableMap.containsKey(paramName)) {
												paramType = fullLocalVariableMap.get(paramName);
											} else if ((fullGlobalVariableMap != null)
													&& (fullGlobalVariableMap.containsKey(paramName))) {
												paramType = fullGlobalVariableMap.get(paramName);
											}
										}
										paramTypes.add(paramType);
									}
								}
								if (paramTypes.size() > 0) {
									// methodInvoc.paramTypeList = new ArrayList<String>();
									// methodInvoc.paramTypeList.addAll(paramTypes);
									for (int i = 0; i < paramTypes.size(); i++) {
										methodInvoc.paramTypeList[i] = paramTypes.get(i);
									}
								}
								// Logger.logDebug("\t\t"+methodInvoc.methodQuanlifiedName + "(" +
								// methodInvoc.paramTypeList +")");
							}
						}
					}
				}
			}
		}
	}

	public void buildMethodDics() {
		for (TypeInfo typeInfo : typeList) {
			LinkedHashMap<String, ArrayList<MethodInfo>> methodDecMap = typeInfo.methodDecMap;
			if (methodDecMap != null) {
				for (String methodName : methodDecMap.keySet()) {
					ArrayList<MethodInfo> methodList = methodDecMap.get(methodName);
					for (MethodInfo methodInfo : methodList) {
						methodNameMethodInfoMap.put(methodInfo.getFullMethodSignature().intern(), methodInfo);
					}

				}
			}
		}

		// Logger.log("methodNameMethodInfoMap size: " +
		// methodNameMethodInfoMap.size());
		// Logger.logDebug("\r\n\r\n\r\n method List: ");
		// for (String methodFullName:methodNameMethodInfoMap.keySet() ){
		// // Logger.logDebug(methodFullName);
		// }
	}

	public void buildNodeSeqDic() {
		@SuppressWarnings("unused")
        int count = 0;

		// for (FileInfo fileInfo:this.fileList){
		// for(NodeSequenceInfo nodeSequence:fileInfo.nodeSequenceList){
		// count++;
		//// String tmp = nodeSequence.toStringSimple().intern();
		// Logger.log("count nodeSet: " + count);
		//
		// nodeSeqStrSet.add(nodeSequence);
		// if (nodeSequence.isScopeConstruct()){
		// TrainingTestingTools.beginEndLoopStr.add(nodeSequence);
		// }
		// }
		// }
		//
		// Logger.log("Total nodeSequences: " + count);
		// Logger.log("nodeSeqStrSet size: " + nodeSeqStrSet.size());

		Integer curIdx = 0;
		// int countFile = 0;
		for (FileInfo fileInfo : this.fileList) {
			// for (NodeSequenceInfo nodeSeq:nodeSeqStrSet)
			// Logger.log("count File f: " + countFile);
			// countFile++;
			for (NodeSequenceInfo nodeSeq : fileInfo.nodeSequenceList) {
				if (nodeSeqStrDic.containsKey(nodeSeq)) {
					continue;
				}
				nodeSeqStrDic.put(nodeSeq, curIdx);
				nodeSeqStrRevDic.put(curIdx, nodeSeq);

				if (methodStartIdx < 0) {
					if (nodeSeq.toStringSimple().equals(methodStartStr)) {
						methodStartIdx = curIdx;
					}
				}

				if (methodEndIdx < 0) {
					if (nodeSeq.toStringSimple().equals(methodEndStr)) {
						methodEndIdx = curIdx;
					}
				}
				curIdx++;
			}
		}

//		Logger.log("nodeSeqStrDic size: " + nodeSeqStrDic.size());
	}

	public void getTypeVarNodeSequence() {
		for (FileInfo fileInfo : fileList) {
			List<NodeSequenceInfo> nodeSequenceList = fileInfo.nodeSequenceList;
			for (NodeSequenceInfo nodeSequence : nodeSequenceList) {
				String attachedVar = nodeSequence.getAttachedVar();

				if (attachedVar != null) {
					MethodInfo methodInfo = nodeSequence.methodInfo;
					TypeInfo typeInfo = nodeSequence.typeInfo;
					// MethodInfo methodInfo = nodeSequence.combinedInfo.methodInfo;
					// TypeInfo typeInfo = nodeSequence.combinedInfo.typeInfo;
					String attachedType = nodeSequence.getAttachedType();
					if (attachedType == null) {
						if (methodInfo != null) {
							if (methodInfo.shortScopeVariableMap != null) {
								if (methodInfo.shortScopeVariableMap.containsKey(attachedVar)) {
									attachedType = methodInfo.shortScopeVariableMap.get(attachedVar);
								}
							}
						}

						Object parentInfo = null;
						if (methodInfo != null) {
							parentInfo = methodInfo.parentInfo;
						} else {
							parentInfo = typeInfo;
						}

						while (attachedType == null) {

							if (parentInfo instanceof TypeInfo) {
								TypeInfo tmp = (TypeInfo) parentInfo;
								if (tmp.shortScopeVariableMap != null) {
									if (tmp.shortScopeVariableMap.containsKey(attachedVar)) {
										attachedType = tmp.shortScopeVariableMap.get(attachedVar);
									}
								}

								parentInfo = tmp.parentInfo;
							} else if (parentInfo instanceof MethodInfo) {
								MethodInfo tmp = (MethodInfo) parentInfo;
								if (tmp.shortScopeVariableMap != null) {
									if (tmp.shortScopeVariableMap.containsKey(attachedVar)) {
										attachedType = tmp.shortScopeVariableMap.get(attachedVar);
									}
								}

								parentInfo = tmp.parentInfo;
							}

							if (parentInfo == null) {
								break;
							}
						}

						if (attachedType == null) {
							int nodeType = nodeSequence.nodeType;
							if ((nodeType == NodeSequenceConstant.VAR)
									|| (nodeType == NodeSequenceConstant.FIELDACCESS)) {
								if (StringUtils.isStartUpperCase(attachedVar)) {
								    if (attachedVar.endsWith(".class")) {
                                        attachedType = "Class";
                                    }
								    else if (attachedVar.matches("^[a-zA-Z]+$")) {
								        attachedType = attachedVar;
								    }
								}
							} else if (nodeType == NodeSequenceConstant.METHODACCESS) {
								if (attachedVar.isEmpty()) {
									attachedType = typeInfo.typeName;
								}
							}
						}

						// if (attachedType == null){
						//
						// String classStr = typeInfo.getFullName();
						//
						// String methodStr = null ;
						// if (methodInfo!=null){
						// methodStr = methodInfo.getFullMethodSignature();
						// }
						// // if (nodeSequence.nodeType==NodeSequenceInfo.VAR)
						// {
						// Logger.logDebug("cannot Find!!" + nodeSequence +"\t" + attachedType+ "\t" +
						// attachedVar + "\t" + classStr + "\t" + methodStr);
						// }
						//
						// }
					}

					nodeSequence.setAttachedType(attachedType);

				}
			}
		}

	}

	/**
	 * Logs all source files' list of code tokens to a file.
	 */
	public void buildMethodInvocListForFiles() {
		//int minN = 1;
		//int maxN = GlobalConfiguration.maxNGram;
		for (FileInfo fileInfo : this.fileList) {
			// for (TypeInfo typeInfo:fileInfo.typeInfoList){
			// Logger.logDebug("type's var list: " + typeInfo.typeName + "\t"+
			// typeInfo.shortScopeVariableMap);
			// for (String methodName:typeInfo.methodDecMap.keySet()){
			// for (MethodInfo methodInfo:typeInfo.methodDecMap.get(methodName)){
			// Logger.logDebug("method's var list: " +methodInfo.methodName + "\t"+
			// methodInfo.shortScopeVariableMap);
			//
			// }
			// }
			// }
			Logger.logDebug("fileInfo :" + fileInfo.filePath);
			for (NodeSequenceInfo nodeSequenceInfo: fileInfo.nodeSequenceList) {
			    Logger.logDebug(nodeSequenceInfo.toStringSimple());
			}

			fileInfo.nodeSequenceIdxList = getIdxList(fileInfo.nodeSequenceList);
			Logger.logDebug("Sequence of Idx: " + fileInfo.nodeSequenceIdxList +'\n');
			// for (int N=minN; N<=maxN; N++)
			// {
			// getNGramIdx(fileInfo.nodeSequenceIdxList, N, false);
			// }

		}

		// Logger.log("nGramCountMap size: " + nGramCountMap.size());

		// buildNGramCountRevMap();

		// int reqLen = 4;
		// displayTopNGram(50, reqLen);

	}

	public List<String> getNodeSeqListFromIdx(List<Integer> nodeSeqIdxList) {
		List<String> nodeSeqList = new ArrayList<String>();
		for (Integer nodeSeqIdx : nodeSeqIdxList) {
			NodeSequenceInfo tmp = nodeSeqStrRevDic.get(nodeSeqIdx);
			nodeSeqList.add(tmp.representStr);
		}
		return nodeSeqList;
	}

	public List<Integer> getIdxList(ArrayList<NodeSequenceInfo> nodeSequenceList) {
		List<Integer> nodeSequenceIdxList = new ArrayList<Integer>();
		for (NodeSequenceInfo nodeSequence : nodeSequenceList) {
			Integer idx = nodeSeqStrDic.get(nodeSequence);
			nodeSequenceIdxList.add(idx);
		}
		return nodeSequenceIdxList;
	}

	/**
	 * 
	 * @param name
	 * @return
	 */

	public boolean isQualifiedName(String name) {
		boolean isQual = false;
		if (name.contains(".")) {
			int idx = name.indexOf(".");
			if ((idx > 0) && (idx < name.length() - 1)) {
				isQual = true;
			}
		}
		return isQual;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SystemTable [packageList=");
		builder.append(packageMap);
		builder.append(", typeList=");
		builder.append(typeList);
		builder.append(", fileList=");
		builder.append(fileList);
		builder.append("]");
		return builder.toString();
	}

}