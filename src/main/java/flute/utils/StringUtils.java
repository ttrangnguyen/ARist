/**
 * 
 */
package flute.utils;

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
}
