package flute.utils.file_processing;

import org.apache.commons.lang3.math.NumberUtils;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaTokenizer{
    public static void main(String[] args) throws IOException {
        List<File> javaFileList = DirProcessor.walkJavaFile("D://zzzz");
        for (File file : javaFileList) {
//            String path = solvePath(file.getName().replace(".java", ".txt"));
//            FileWriter writer = new FileWriter(path);
//            String fileContent = JavaTokenizer.removePackagesAndImports(file.getAbsolutePath());
            StringBuilder fileContent = new StringBuilder();
            try {
                FileInputStream fstream = new FileInputStream(file);
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        fstream));
                String strLine;
                while ((strLine = br.readLine()) != null) {
                    fileContent.append(strLine + "\n");
                }
                ArrayList<String> tokens = JavaTokenizer.tokenize(fileContent.toString());
//            for (String token : tokens) {
//                writer.write(token + "\n");
//            }
                System.out.println(tokens);
//            writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



//        String testLexical = "categoryDetail = event.getTarget().getName();else {categoryString = categoryObject.getClass().getName();}}Log log = getLog(";
//        ArrayList<String> tokens = JavaTokenizer.tokenize(testLexical);
//        System.out.println(tokens);

//        final String namePath = "src/main/python/model/java_names.txt";
//
//        String names = FileProcessor.read(new File(namePath));
//        Tokenizer tokenizer = new Tokenizer("<unk>");
//        tokenizer.fitOnTexts(names.split("\n"));
//        System.out.println(tokenizer.getWordIndex());
//        System.out.println(tokenizer.getWordIndex().size());
    }

    private static String solvePath(String fileName) {
        if (toTest()) {
            return "javaFileTokens/test/" + fileName;
        } else if (toValidate()) {
            return "javaFileTokens/validate/" + fileName;
        } else {
            return "javaFileTokens/train/" + fileName;
        }
    }

    private static boolean toTest() {
        return Math.random() < 0.1;
    }

    private static boolean toValidate() {
        return Math.random() < 0.15;
    }

    public static String removePackagesAndImports(String filePath) {
        StringBuilder fileContent = new StringBuilder();
        try {
            FileInputStream fstream = new FileInputStream(new File(filePath));
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    fstream));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                if (!(strLine.startsWith("package") || strLine.startsWith("import")))
                    fileContent.append(strLine + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileContent.toString();
    }

    public static ArrayList<String> tokenize(String text) throws IOException {
        IScanner scanner = ToolFactory.createScanner(false, false, true, "1.8");
        scanner.setSource(text.replaceAll("[a-zA-Z0-9_.]+\\.class", ".class")
                .replaceAll("\\[.*?]", "[]").toCharArray());

        ArrayList<String> lineTokens = new ArrayList<>();
        List<String> tokens = new ArrayList<>();
        int nextToken;
        while (true) {
            try {
                nextToken = scanner.getNextToken();
//                int ln = scanner.getLineNumber(scanner.getCurrentTokenStartPosition());
                if (nextToken == ITerminalSymbols.TokenNameEOF) break;
            } catch (InvalidInputException e) {
                continue;
            }
            String val = new String(scanner.getCurrentTokenSource());
            if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                lineTokens.add("\"\"");
            } else if (val.startsWith("'") && val.endsWith("'") && val.length() >= 2) {
                lineTokens.add("''");
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
                        } else if (token.matches("(<|>)+")) {
                            split = true;
                            break;
                        } else {
                            break;
                        }
                    }
                    if (split) {
                        for (int i = 0; i < val.length(); i++) {
                            tokens.add(">");
                            lineTokens.add(">");
                        }
                        continue;
                    }
                }
                List<String> valTokens = solveLine(val);
                tokens.addAll(valTokens);
                lineTokens.addAll(valTokens);
            }
        }
        return lineTokens;
    }

    public static List<String> solveLine(String line) {
        List<String> allTokens = new ArrayList<>();
        if (!line.isEmpty()) {
            char c = line.charAt(0);
            if (NumberUtils.isCreatable(line)) {
                allTokens.add("0");
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                allTokens.addAll(tokenizeWord(line));
            } else if (line.length() <= 3) {
                allTokens.add(line);
            } else {
                allTokens.addAll(tokenizeWord(line));
            }
        }
        return allTokens;
    }

    public static ArrayList<String> tokenizeWord(String val) {
        ArrayList<String> tokens = new ArrayList<>();
        for (String word: val.split("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|(?<=[0-9])(?=[A-Z][a-z])|(?<=[a-zA-Z])(?=[0-9])")) {
            if (!word.isEmpty()) {
                if (word.startsWith("'") && word.endsWith("'")) {
                    tokens.add("''");
                } else {
                    String[] strs = splitKeepDelimiters(word, "[\\p{Space}]+|\"\"|[\\p{Punct}\\s]");
                    for (String str: strs) {
                        if (!str.trim().isEmpty()) {
                            tokens.add(str);
                        }
                    }
                }
            }
        }
        return tokens;
    }

    public static String[] splitKeepDelimiters(String word, String regex) {
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
//    public static ArrayList<String> tokenize(String fileContent) throws IOException {
//        ArrayList<String> tokens = new ArrayList<>();
//        try {
//            Reader inputString = new StringReader(fileContent);
//            BufferedReader reader = new BufferedReader(inputString);
//            StreamTokenizer st = new StreamTokenizer(reader);
//
//            st.parseNumbers();
//            st.wordChars('_', '_');
//            st.eolIsSignificant(true);
//            st.ordinaryChars(0, ' ');
//            st.slashSlashComments(true);
//            st.slashStarComments(true);
//            st.ordinaryChar('.');
//
//            int token = st.nextToken();
//            StringBuilder op = new StringBuilder();
//            String operatorStr = "&<>?+-*/%!~^|=";
//            while (token != StreamTokenizer.TT_EOF) {
//                switch (token) {
//                    case StreamTokenizer.TT_NUMBER:
//                        if (!op.toString().equals("")) {
//                            tokens.add(op.toString());
//                            op = new StringBuilder();
//                        }
//                        tokens.add("0");
////                        double num = st.nval;
////                        if (String.valueOf(num).equals("0.0")) tokens.add("0");
////                        else tokens.add(String.valueOf(num));
//                        break;
//                    case StreamTokenizer.TT_WORD:
//                        if (!op.toString().equals("")) {
//                            tokens.add(op.toString());
//                            op = new StringBuilder();
//                        }
//                        tokens.add(st.sval);
//                        break;
//                    case '"':
//                        if (!op.toString().equals("")) {
//                            tokens.add(op.toString());
//                            op = new StringBuilder();
//                        }
//                        String dquoteVal = st.sval;
//                        tokens.add("\"\"");
//                        break;
//                    case '\'':
//                        if (!op.toString().equals("")) {
//                            tokens.add(op.toString());
//                            op = new StringBuilder();
//                        }
//                        tokens.add("''");
//                        break;
//                    case StreamTokenizer.TT_EOL:
//                        if (!op.toString().equals("")) {
//                            tokens.add(op.toString());
//                            op = new StringBuilder();
//                        }
//                        break;
//                    default:
//                        char ch = (char) st.ttype;
//                        if (ch == '\n' || ch == ' ' || ch == '\r') {
//                            if (!op.toString().equals("")) {
//                                tokens.add(op.toString());
//                                op = new StringBuilder();
//                            }
//                        }
//                        else if (operatorStr.indexOf(ch) != -1) {
//                            op.append(ch);
//                        } else {
//                            if (!op.toString().equals("")) {
//                                tokens.add(op.toString());
//                                op = new StringBuilder();
//                            }
//                            tokens.add(String.valueOf(ch));
//                        }
//                        break;
//                }
//                token = st.nextToken();
//            }
//            if (!op.toString().equals("")) {
//                tokens.add(op.toString());
//            }
//        } catch (IOException e) {
//        }
//        return tokens;
//    }
}