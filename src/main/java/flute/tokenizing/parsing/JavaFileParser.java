package flute.tokenizing.parsing;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import com.github.javaparser.ast.body.MethodDeclaration;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseStart;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.Statement;
import flute.tokenizing.excode_data.FileInfo;
import flute.tokenizing.excode_data.SystemTableCrossProject;
import flute.tokenizing.excode_data.TypeInfo;
import flute.tokenizing.visitors.MetricsVisitor;

import static com.github.javaparser.Providers.provider;

/**
 * @author ANH
 * This class parses Java source files.
 */
public class JavaFileParser {

	/**
	 * Traverses the given source file and analyzes it.
	 * Updates {@link SystemTableCrossProject#fileList} and {@link SystemTableCrossProject#typeList}
	 * of the given systemTable with extracted file information.
	 * @param visitor
	 * @param file
	 * @param systemTable
	 * @param origDirPath The original directory path.
	 * @see    MetricsVisitor
	 */
	public static void visitFile(MetricsVisitor visitor, File file, SystemTableCrossProject systemTable,
			String origDirPath) {
	    
		FileInfo fileInfo = new FileInfo();
		fileInfo.file = file;
		visitor.init(fileInfo);

		CompilationUnit cu = null;
		try {
			cu = getCU(file);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (cu != null) {
			int lenOrigDirPath = origDirPath.length();
			fileInfo.filePath = new String(file.getAbsolutePath().substring(lenOrigDirPath));
			fileInfo.fileName = file.getName();

			visitor.visit(cu, null);

			systemTable.fileList.add(fileInfo);

			for (TypeInfo typeInfo : fileInfo.typeInfoList) {
				systemTable.typeList.add(typeInfo);
			}

			visitor.resetAll();
		}

	}

	/**
	 * @return a compilation unit from the given file.
	 * @throws ParseException
	 * @see    {@link com.github.javaparser.JavaParser#parse(InputStream)}
	 */
	public static CompilationUnit getCU(File file) throws ParseException {
		// InputStream in = null;
		CompilationUnit cu = null;

		String content = "";
		try {
			// content = new Scanner(file).useDelimiter("\\Z").next();
			content = sonReader(file.getAbsolutePath());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.err.println(file.getName());
			e.printStackTrace();
		}
		// TODO: should make better solution
		// content = content.trim();

		// TODO SON preprocess
		// content = preprocess(content);
		// System.out.println(content);

		// content = content.replaceAll("\\\\\\\\ud", "_ud");
        // content = content.replaceAll("\\\\ud", "_ud");
        // content = content.replaceAll("#", " ");

        InputStream is = new ByteArrayInputStream(content.getBytes());

        try {
            cu = StaticJavaParser.parse(is);
            cu = StaticJavaParser.parse(cu.toString());
			cu = StaticJavaParser.parse(cu.toString());
			cu = StaticJavaParser.parse(cu.toString());
			//JavaParser parser = new JavaParser();
			//cu = parser.parse(ParseStart.COMPILATION_UNIT, provider(content)).getResult().get();
        } catch (Error e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
			e.printStackTrace();
		//} catch (IOException e) {
        //	e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

		return cu;
	}

	/**
	 * @param fileName The file path.
	 * @return the file contents excluding Java Annotation lines.
	 */
	public static String sonReader(String fileName) {
		String content = "";
		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (!line.trim().startsWith("@")) {
					content += (line + "\n");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
	}

	@SuppressWarnings("unused")
    private static String preprocess(String content) {
		content = content.replace("<>", "");
		content = content.replace("\ttry (", "\ttry(");
		content = content.replace(" try (", " try(");
		int start = 0;
		int end = 0;
		while (true) {
			start = content.indexOf("try(");
			if (start == -1)
				break;
			int mid = findClosingParen(content.toCharArray(), start + "try(".length() + 2);
			if (mid == -1)
				break;
			int mid2 = content.indexOf("{", mid);
			if (mid2 == -1)
				break;
			end = findClosingParen2(content.toCharArray(), mid2);
			if (end == -1)
				break;

			String old = content.substring(start, end + 1);
			String newTry = "";
			newTry = content.substring(start + "try(".length(), mid) + ";\n";
			newTry += content.substring(mid2 + 1, end);
			content = content.replace(old, newTry);
		}

		return content;
	}

	@SuppressWarnings("unused")
	public static int findClosingParen(char[] text, int openPos) {
		int closePos = openPos;
		int counter = 1;
		try {
			while (counter > 0) {
				char c = text[++closePos];
				if (c == '(') {
					counter++;
				} else if (c == ')') {
					counter--;
				}
			}
			return closePos;
		} catch (Exception e) {
			return -1;
		}
	}

	@SuppressWarnings("unused")
	public static int findClosingParen2(char[] text, int openPos) {
		int closePos = openPos;
		int counter = 1;
		while (counter > 0) {
			char c = text[++closePos];
			if (c == '{') {
				counter++;
			} else if (c == '}') {
				counter--;
			}
		}
		return closePos;
	}

}
