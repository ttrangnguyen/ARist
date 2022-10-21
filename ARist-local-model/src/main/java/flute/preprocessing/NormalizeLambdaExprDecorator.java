package flute.preprocessing;

import java.io.File;

public class NormalizeLambdaExprDecorator extends PreprocessDecorator {
    public NormalizeLambdaExprDecorator(Preprocessor preprocessor) {
        super(preprocessor);
    }

    @Override
    public String preprocessFile(File file) {
        return NormalizeLambdaExprDecorator.preprocess(super.preprocessFile(file));
    }

    @Override
    public String preprocessText(String text) {
        return NormalizeLambdaExprDecorator.preprocess(super.preprocessText(text));
    }

    public static String preprocess(String sourceCode, String specialToken) {
        StringBuilder newSourceCode = new StringBuilder();
        int lastIndex = 0;
        for (int i = sourceCode.indexOf("->"); i >= 0; i = sourceCode.indexOf("->", i + 1)) {
            int j = i;
            boolean paramFlag = false;
            int balance = 0;
            while (j >= 0) {
                j--;
                char c = sourceCode.charAt(j);
                if (c == '(') ++balance;
                if (c == ')') --balance;
                if (c != ' ') paramFlag = true;
                if (paramFlag) {
                    boolean validFlag = false;
                    if (c == '(' || c == ')') validFlag = true;
                    if (c == ' ') validFlag = true;
                    if ('a' <= c && c <= 'z') validFlag = true;
                    if ('A' <= c && c <= 'Z') validFlag = true;
                    if ('0' <= c && c <= '9') validFlag = true;
                    if (balance == 1) validFlag = false;
                    if (!validFlag) break;
                }
            }
            newSourceCode.append(sourceCode.substring(lastIndex, j + 1));
            newSourceCode.append(specialToken);
            newSourceCode.append(sourceCode.substring(j + 1, i));
            lastIndex = i;
        }
        newSourceCode.append(sourceCode.substring(lastIndex));
        return newSourceCode.toString();
    }

    /**
     * Note: {@link EmptyStringLiteralDecorator#preprocess} must be used beforehand.
     */
    public static String preprocess(String sourceCode) {
        return preprocess(sourceCode, "<LAMBDA>");
    }

    public static String revertPreprocessing(String sourceCode) {
        return sourceCode.replace("<LAMBDA>", "");
    }

    @Override
    public String revertFile(File file) {
        return NormalizeLambdaExprDecorator.revertPreprocessing(super.revertFile(file));
    }
}
