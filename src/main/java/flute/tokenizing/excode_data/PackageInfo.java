/**
 * 
 */
package flute.tokenizing.excode_data;

import java.util.ArrayList;

public class PackageInfo {
	public String packageDec ;
	public ArrayList<TypeInfo> typeList = new ArrayList<TypeInfo>(1);
	
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
		builder.append("PackageInfo [packageDec=");
		builder.append(packageDec);
		builder.append(", typeList=");
		builder.append(typeList);
		builder.append("]\r\n");
		return builder.toString();
	}

	
}
