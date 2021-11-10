package flute.preprocessing;

import flute.config.Config;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NormalizeCompoundDecorator extends Decorator {
    public NormalizeCompoundDecorator(Preprocessor preprocessor) {
        super(preprocessor);
    }

    @Override
    public String preprocessFile(File file) {
        return NormalizeCompoundDecorator.preprocess(super.preprocessFile(file));
    }

    private static boolean isCompound(String param) {
        param = RemoveNewLineDecorator.preprocess(param);

        if (param.matches("[a-zA-Z\\d_$.()\\[\\]\"' ]*")) return false;

        // Number literal
        if (param.matches("-?\\d*")) return false;

        // Lambda expression
        if (param.contains("->")) return false;

        // Method reference
        if (param.contains("::")) return false;
        return true;
    }

    /**
     * Note: {@link EmptyStringLiteralDecorator#preprocess} must be used beforehand.
     * Note: Do not use {@link NormalizeLambdaExprDecorator#preprocess} before this.
     */
    public static String preprocess(String sourceCode) {
        Matcher m = Pattern.compile("\\(|\\)|,|[^\\(\\),]+").matcher(sourceCode);
        Stack<String> stack = new Stack<>();
        Stack<String> stackClose = new Stack<>();
        while (m.find()) {
            String s = m.group();
            if (s.compareTo(")") == 0) {
                List<String> listClose = new ArrayList();
                List<String> restoredListClose = new ArrayList<>();
                boolean notMethodCallFlag = false;
                while (true) {
                    String top = stack.pop();
                    if (top.indexOf(';') >= 0) notMethodCallFlag = true;
                    if (top.trim().matches("^[a-zA-Z\\d_$.\\[\\]<>? ]+ [a-zA-Z\\d_$]+$")) notMethodCallFlag = true;
                    listClose.add(top);
                    if (stack.empty()|| top.compareTo("(") == 0) break;
                }
                notMethodCallFlag |= !(stack.empty() || (stack.peek().substring(stack.peek().length() - 1).matches("[a-zA-Z\\d_$]")));
                boolean compoundFlag = false;
                for (String t: listClose) {
                    if (!notMethodCallFlag) {
                        if (t.compareTo(",") == 0 || t.compareTo("(") == 0) {
                            if (compoundFlag) restoredListClose.add("<COMPOUND>");
                            compoundFlag = false;
                        } else {
                            compoundFlag |= isCompound(t);
                        }
                    }
                    restoredListClose.add(t);
                    if (t.compareTo(")") == 0) {
                        restoredListClose.add(stackClose.pop());
                    }
                }
                StringBuilder sb = new StringBuilder();
                for (int i = restoredListClose.size() - 1; i >= 0; --i) {
                    sb.append(restoredListClose.get(i));
                }
                stackClose.push(sb.toString());
            }
            stack.add(s);
        }
        StringBuilder sb = new StringBuilder();
        int cnt = 0;
        for (String s: stack) {
            if (s.compareTo(")") == 0) {
                sb.append(stackClose.get(cnt++));
            }
            sb.append(s);
        }
        return sb.toString();
    }

    @Override
    public String revertFile(File file) {
        return NormalizeCompoundDecorator.revertPreprocessing(super.revertFile(file));
    }

    public static String revertPreprocessing(String sourceCode) {
        return sourceCode.replace("<COMPOUND>", "");
    }

    public static void main(String[] args) {
        String inputFolder = Config.STORAGE_DIR + "repositories/oneproj/";
        String outputFolder = Config.STORAGE_DIR + "out/";

        Preprocessor preprocessor = new Preprocessor();
        preprocessor = new EmptyStringLiteralDecorator(preprocessor);
        preprocessor = new NormalizeCompoundDecorator(preprocessor);
        //preprocessor.preprocessProjects(new File(outputFolder), new File(outputFolder), true);

        preprocessor.preprocessProjects(new File(inputFolder), new File(outputFolder));
    }
}
