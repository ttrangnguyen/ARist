package slp.core.lexing.code;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import flute.config.Config;
import flute.config.ModelConfig;
import flute.preprocessing.EmptyStringLiteralDecorator;
import flute.preprocessing.NormalizeCompoundDecorator;
import flute.preprocessing.NormalizeLambdaExprDecorator;
import org.apache.commons.lang3.math.NumberUtils;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import slp.core.lexing.Lexer;

public class JavaLexer implements Lexer {

	@Override
	public Stream<String> lexLine(String line) {
		return tokenizeLines(line).get(0).stream();
	}
	
	@Override
	public Stream<Stream<String>> lexText(String text) {
		return tokenizeLines(text).stream().map(List::stream);
	}

	private List<String> normalize(String line) {
		List<String> allTokens = new ArrayList<>();
		if (!line.isEmpty()) {
			char c = line.charAt(0);
			if (NumberUtils.isCreatable(line)) {
				allTokens.add("0");
			} else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
				allTokens.addAll(tokenizeWord(line));
			} else if (line.length() <= 3) {
				// i.e. >>> and <<<
				allTokens.add(line);
			} else {
				allTokens.addAll(tokenizeWord(line));
			}
		}
		return allTokens;
	}

	public List<List<String>> tokenizeLines(String text) {
		IScanner scanner = ToolFactory.createScanner(false, false, true, "1.8");
		if (Config.mode == Config.Mode.TEST) {
			scanner.setSource(text.toCharArray());
		} else {
			text = EmptyStringLiteralDecorator.preprocess(text);
			text = NormalizeCompoundDecorator.preprocess(text, "<COMPOUND>");
			text = NormalizeLambdaExprDecorator.preprocess(text, "<LAMBDA>");
			scanner.setSource(text.replaceAll("[a-zA-Z0-9_.]+\\.class", ".class")
					.replaceAll("\\[.*?]", "[]").toCharArray());
		}

		List<List<String>> lineTokens = new ArrayList<>();
		List<String> tokens = new ArrayList<>();
		lineTokens.add(new ArrayList<>());
		int nextToken;
		int line = 1;
		while (true) {
			try {
				nextToken = scanner.getNextToken();
				int ln = scanner.getLineNumber(scanner.getCurrentTokenStartPosition());
				if (ln > line) {
					for (int i = line + 1; i <= ln; i++) lineTokens.add(new ArrayList<>());
					line = ln;
				}
				if (nextToken == ITerminalSymbols.TokenNameEOF) break;
			} catch (InvalidInputException e) {
				continue;
			}
			String val = new String(scanner.getCurrentTokenSource());
			if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
				lineTokens.get(lineTokens.size() - 1).add("\"\"");
			}
			else if (val.startsWith("'") && val.endsWith("'") && val.length() >= 2) {
				lineTokens.get(lineTokens.size() - 1).add("''");
			}
			// For Java, we have to add heuristic check regarding breaking up >>
			else {
				if (val.matches(">>+")) {
					boolean split = false;
					for (int i = tokens.size() - 1; i >= 0; i--) {
						String token = tokens.get(i);
						if (token.matches("[,\\.\\?\\[\\]]") || Character.isUpperCase(token.charAt(0))
								|| token.equals("extends") || token.equals("super")
								|| token.matches("(byte|short|int|long|float|double)")) {
						}
						else if (token.matches("(<|>)+")) {
							split = true;
							break;
						}
						else {
							break;
						}
					}
					if (split) {
						for (int i = 0; i < val.length(); i++) {
							tokens.add(">");
							lineTokens.get(lineTokens.size() - 1).add(">");
						}
						continue;
					}
				}
				List<String> normalized = normalize(val);
				tokens.addAll(normalized);

				lineTokens.get(lineTokens.size() - 1).addAll(normalized);
				List<String> lastLine = lineTokens.get(lineTokens.size() - 1);
				if (lastLine.size() > 2) {
					String l3 = lastLine.get(lastLine.size() - 3);
					if (!l3.equals("<")) continue;
					String l2 = lastLine.get(lastLine.size() - 2);
					if (!ModelConfig.specialTokens.contains(l2)) continue;
					String l1 = lastLine.get(lastLine.size() - 1);
					if (!l1.equals(">")) continue;
					lastLine.remove(lastLine.size()-1);
					lastLine.remove(lastLine.size()-1);
					lastLine.remove(lastLine.size()-1);
					lastLine.add(l3+l2+l1);
				}
			}
		}
		return lineTokens;
	}

	public ArrayList<String> tokenizeWord(String val) {
		if (ModelConfig.tokenizedType == ModelConfig.TokenizedType.FULL_TOKEN) return tokenizeWordFullToken(val);
		else return tokenizeWordSubToken(val);
	}

	public ArrayList<String> tokenizeWordFullToken(String val) {
		return new ArrayList<>(Collections.singletonList(val));
	}

	public ArrayList<String> tokenizeWordSubToken(String val) {
		ArrayList<String> tokens = new ArrayList<>();
		for (String word: val.split("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|(?<=[0-9])(?=[A-Z][a-z])|(?<=[a-zA-Z])(?=[0-9])")) {
			if (!word.isEmpty()) {
				String[] strs = splitKeepDelimiters(word, "[\\p{Space}]+|\"\"|[\\p{Punct}\\s]");
				for (String str: strs) {
					if (!str.trim().isEmpty()) {
						tokens.add(str);
					}
				}
			}
		}
		return tokens;
	}

	public String[] splitKeepDelimiters(String word, String regex) {
		Pattern pattern = Pattern.compile(regex);
		int lastMatch = 0;
		LinkedList<String> splitted = new LinkedList<>();
		Matcher m = pattern.matcher(word);
		while (m.find()) {
			splitted.add(word.substring(lastMatch, m.start()));
			splitted.add(m.group());
			lastMatch = m.end();
		}
		splitted.add(word.substring(lastMatch));
		return splitted.toArray(new String[splitted.size()]);
	}

	private static final String ID_REGEX = "[a-zA-Z_$][a-zA-Z\\d_$]*";
	private static final String HEX_REGEX = "0x([0-9a-fA-F]+_)*[0-9a-fA-F]+[lLfFdD]?";
	private static final String BIN_REGEX = "0b([01]+_)*[01]+[lL]";
	private static final String IR_REGEX = "([0-9]+_)*[0-9]+[lLfFdD]?";
	// A: nrs before and after dot, B: nrs only before dot, C nrs only after, D: only E as indicator
	private static final String DBL_REGEXA = "[0-9]+\\.[0-9]+([eE][-+]?[0-9]+)?[fFdD]?";
	private static final String DBL_REGEXB = "[0-9]+\\.([eE][-+]?[0-9]+)?[fFdD]?";
	private static final String DBL_REGEXC = "\\.[0-9]+([eE][-+]?[0-9]+)?[fFdD]?";
	private static final String DBL_REGEXD = "[0-9]+[eE][-+]?[0-9]+[fFdD]?";

	public static boolean isID(String token) {
		return !isKeyword(token) && token.matches(ID_REGEX);
	}

	public static boolean isNR(String token) {
		return token.matches("(" + HEX_REGEX + "|" + IR_REGEX + "|" + BIN_REGEX +
				"|" + DBL_REGEXA + "|" + DBL_REGEXB + "|" + DBL_REGEXC + "|" + DBL_REGEXD + ")");
	}

	public static boolean isSTR(String token) {
		return token.matches("\".+\"");
	}

	public static boolean isChar(String token) {
		return token.matches("'.+'");
	}

	public static boolean isKeyword(String token) {
		return JavaLexer.KEYWORD_SET.contains(token);
	}

	public static final String[] KEYWORDS = { "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
			"class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally",
			"float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
			"new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super",
			"switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
			"true", "false", "null" };

	public static final Set<String> KEYWORD_SET = new HashSet<>(Arrays.asList(KEYWORDS));
}
