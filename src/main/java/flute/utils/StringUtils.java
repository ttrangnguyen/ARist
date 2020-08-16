/**
 *
 */
package flute.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import flute.utils.logging.Logger;

/**
 * @author ANH
 *
 */
public class StringUtils {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Logger.log(isStartUpperCase(" Test"));
	}

	public static int countNumLines(String str){
		Scanner scanner = new Scanner(str);

		int count = 0;
		while (scanner.hasNextLine()) {
			scanner.nextLine();
			count++;
		}
		scanner.close();

		return count;
	}

	public static boolean isStartUpperCase(String str){
		boolean startUpperCase = false;
		if((str == null)||(str.length()==0)){
			startUpperCase = false;
		}
		else{
			String tmp = str;//str.trim();
			if (tmp.matches("[A-Z].*")){
				startUpperCase = true;
			}
		}
		return startUpperCase;
	}

	public static ArrayList<String> concat(ArrayList<String> arrayList, String candidate) {
		ArrayList<String> newArrayList = new ArrayList<>(arrayList);
		newArrayList.add(candidate);
		return newArrayList;
	}

	public static String concat(String str1, String str2) {
		return str1 + " " + str2;
	}

	public static ArrayList<String> splitToArrayList(String str, String regex) {
		return new ArrayList<>(Arrays.asList(str.split(regex)));
	}
}
