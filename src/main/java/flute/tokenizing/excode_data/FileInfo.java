/**
 * 
 */
package flute.tokenizing.excode_data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ANH
 * This class saves information about source file.
 */
public class FileInfo {
	public String filePath ;
	public String fileName ;
	public String packageDec ;
	public File file;
	public ArrayList<TypeInfo> typeInfoList = new ArrayList<TypeInfo>(1);
	public ArrayList<String> importList = new ArrayList<String>(1);
	
	public ArrayList<String> feasibleTypeList = new ArrayList<String>(1);
	public int numBranches = 0;
	public int numIfControls = 0;
	public int numSwitchControls = 0;
	public int numWhileControls = 0;
	public int numForControls = 0;
	public int numForEachControls = 0;
	public int numDoControls = 0;
	public int numTrys = 0;
	public int numCatches = 0;
	public int numThrows = 0;
	public int numBlocks = 0;
	public int numMethodDecs = 0;
	public int numWildcards = 0;
	public int numJavadocs = 0;
	public int numBlockComments = 0;
	public int numLineComments = 0;
	public int numFanIn = 0;
	public int numFanOut = 0;
	
	public int numStatements = 0;
	
	public ArrayList<NodeSequenceInfo> nodeSequenceList = new ArrayList<NodeSequenceInfo>(1);
	public List<Integer> nodeSequenceIdxList ;//= new ArrayList<Integer>(1);

	
	public ArrayList<NodeSequenceInfo> getNodeSequenceList() {
		ArrayList<NodeSequenceInfo> results = new ArrayList<NodeSequenceInfo>();
		for (NodeSequenceInfo info : nodeSequenceList) {
			if(!info.toString().trim().isEmpty()) {
//				System.out.println(info);
				results.add(info);
			}
		}
		return results;
	}
	
//	public int numAnnots = 0;


	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((filePath == null) ? 0 : filePath.hashCode());
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
		FileInfo other = (FileInfo) obj;
		if (filePath == null) {
			if (other.filePath != null)
				return false;
		} else if (!filePath.equals(other.filePath))
			return false;
		return true;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("FileInfo [filePath=");
		builder.append(filePath);
		builder.append(", fileName=");
		builder.append(fileName);
//		builder.append(", packageDec=");
//		builder.append(packageDec);
//		builder.append(", typeInfoList=");
//		builder.append(typeInfoList);
//		builder.append(", importList=");
//		builder.append(importList);
		builder.append(", feasibleTypeList=");
		builder.append(feasibleTypeList);
//		builder.append(", numBranches=");
//		builder.append(numBranches);
//		builder.append(", numIfControls=");
//		builder.append(numIfControls);
//		builder.append(", numSwitchControls=");
//		builder.append(numSwitchControls);
//		builder.append(", numWhileControls=");
//		builder.append(numWhileControls);
//		builder.append(", numForControls=");
//		builder.append(numForControls);
//		builder.append(", numForEachControls=");
//		builder.append(numForEachControls);
//		builder.append(", numDoControls=");
//		builder.append(numDoControls);
//		builder.append(", numTrys=");
//		builder.append(numTrys);
//		builder.append(", numCatches=");
//		builder.append(numCatches);
//		builder.append(", numThrows=");
//		builder.append(numThrows);
//		builder.append(", numBlocks=");
//		builder.append(numBlocks);
//		builder.append(", numMethodDecs=");
//		builder.append(numMethodDecs);
//		builder.append(", numWildcards=");
//		builder.append(numWildcards);
//		builder.append(", numJavadocs=");
//		builder.append(numJavadocs);
//		builder.append(", numBlockComments=");
//		builder.append(numBlockComments);
//		builder.append(", numLineComments=");
//		builder.append(numLineComments);
//		builder.append(", numFanIn=");
//		builder.append(numFanIn);
//		builder.append(", numFanOut=");
//		builder.append(numFanOut);
//		builder.append(", numStatements=");
//		builder.append(numStatements);
		builder.append("]");
		return builder.toString();
	}

}
