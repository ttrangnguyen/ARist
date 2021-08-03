package flute.utils.file_processing;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class JavaTokenizer{
    public static void main(String[] args) throws IOException {
        List<File> javaFileList = DirProcessor.walkJavaFile("D://zzzz");
        for (File file : javaFileList) {
//            String path = solvePath(file.getName().replace(".java", ".txt"));
//            FileWriter writer = new FileWriter(path);
            String fileContent = JavaTokenizer.removePackagesAndImports(file.getAbsolutePath());
            ArrayList<String> tokens = JavaTokenizer.tokenize(fileContent);
//            for (String token : tokens) {
//                writer.write(token + "\n");
//            }
            System.out.println(tokens);
//            writer.close();
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

    public static ArrayList<String> tokenize(String fileContent) throws IOException {
        ArrayList<String> tokens = new ArrayList<>();
        try {
            Reader inputString = new StringReader(fileContent);
            BufferedReader reader = new BufferedReader(inputString);
            StreamTokenizer st = new StreamTokenizer(reader);

            st.parseNumbers();
            st.wordChars('_', '_');
            st.eolIsSignificant(true);
            st.ordinaryChars(0, ' ');
            st.slashSlashComments(true);
            st.slashStarComments(true);
            st.ordinaryChar('.');

            int token = st.nextToken();
            StringBuilder op = new StringBuilder();
            String operatorStr = "&<>?+-*/%!~^|=";
            while (token != StreamTokenizer.TT_EOF) {
                switch (token) {
                    case StreamTokenizer.TT_NUMBER:
                        if (!op.toString().equals("")) {
                            tokens.add(op.toString());
                            op = new StringBuilder();
                        }
                        tokens.add("0");
//                        double num = st.nval;
//                        if (String.valueOf(num).equals("0.0")) tokens.add("0");
//                        else tokens.add(String.valueOf(num));
                        break;
                    case StreamTokenizer.TT_WORD:
                        if (!op.toString().equals("")) {
                            tokens.add(op.toString());
                            op = new StringBuilder();
                        }
                        tokens.add(st.sval);
                        break;
                    case '"':
                        if (!op.toString().equals("")) {
                            tokens.add(op.toString());
                            op = new StringBuilder();
                        }
                        String dquoteVal = st.sval;
                        tokens.add("\"\"");
                        break;
                    case '\'':
                        if (!op.toString().equals("")) {
                            tokens.add(op.toString());
                            op = new StringBuilder();
                        }
                        tokens.add("''");
                        break;
                    case StreamTokenizer.TT_EOL:
                        if (!op.toString().equals("")) {
                            tokens.add(op.toString());
                            op = new StringBuilder();
                        }
                        break;
                    default:
                        char ch = (char) st.ttype;
                        if (ch == '\n' || ch == ' ' || ch == '\r') {
                            if (!op.toString().equals("")) {
                                tokens.add(op.toString());
                                op = new StringBuilder();
                            }
                        }
                        else if (operatorStr.indexOf(ch) != -1) {
                            op.append(ch);
                        } else {
                            if (!op.toString().equals("")) {
                                tokens.add(op.toString());
                                op = new StringBuilder();
                            }
                            tokens.add(String.valueOf(ch));
                        }
                        break;
                }
                token = st.nextToken();
            }
            if (!op.toString().equals("")) {
                tokens.add(op.toString());
            }
        } catch (IOException e) {
        }
        return tokens;
    }
}