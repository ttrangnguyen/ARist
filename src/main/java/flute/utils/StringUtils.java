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
		//Logger.log(isStartUpperCase(" Test"));
		System.out.println(StringUtils.indexOf("abcdbcd", "cd"));
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

	public static int indexOf(String s, String t) {
		int[] next = new int[t.length()];
		int j = -1;
		next[0] = -1;
		for (int i = 1; i < t.length(); ++i) {
			while (j >= 0 && t.charAt(j + 1) != t.charAt(i)) j = next[j];
			if (t.charAt(j + 1) == t.charAt(i)) ++j;
			next[i] = j;
		}

		j = -1;
		for (int i = 1; i < s.length(); ++i) {
			while (j >= 0 && t.charAt(j + 1) != s.charAt(i)) j = next[j];
			if (t.charAt(j + 1) == s.charAt(i)) ++j;
			if (j == t.length() - 1) return i - j;
		}
		return -1;
	}

	public static String getFirstLine(String str) {
		if (str.indexOf('\n') >= 0) {
			str = str.substring(0, str.indexOf('\n'));
		}
		return str;
	}
}
