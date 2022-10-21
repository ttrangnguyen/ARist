/**
 * 
 */
package flute.tokenizing.exe;

import java.util.ArrayList;
import java.util.HashMap;

import flute.tokenizing.excode_data.PackageInfo;
import flute.tokenizing.excode_data.SystemTableCrossProject;
import flute.tokenizing.excode_data.TypeInfo;

public class GetDirStructureCrossProject {
	public synchronized static void buildSystemPackageList(SystemTableCrossProject systemTable) {
		ArrayList<TypeInfo> typeList = systemTable.typeList;
		HashMap<String, PackageInfo> packageMap = systemTable.packageMap;
		for (TypeInfo typeInfo : typeList) {
			String packageDec = typeInfo.packageDec;
			if (packageMap.containsKey(packageDec)) {
				(packageMap.get(packageDec)).typeList.add(typeInfo);
			} else {
				PackageInfo packageInfo = new PackageInfo();
				packageInfo.packageDec = packageDec;
				packageInfo.typeList.add(typeInfo);
				if (packageDec == null) {
					packageMap.put(null, packageInfo);
				} else {
					packageMap.put(packageDec.intern(), packageInfo);
				}
			}

		}
	}
}
