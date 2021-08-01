package flute.preprocessing;

import java.io.File;

public class NormalizeLambdaExprDecorator extends Decorator {
    public NormalizeLambdaExprDecorator(Preprocessor preprocessor) {
        super(preprocessor);
    }

    @Override
    public String preprocessFile(File file) {
        return NormalizeLambdaExprDecorator.preprocess(super.preprocessFile(file));
    }

    public static String preprocess(String sourceCode) {
        StringBuilder newSourceCode = new StringBuilder();
        int lastIndex = 0;
        for (int i = sourceCode.indexOf("->"); i >= 0; i = sourceCode.indexOf("->", i + 1)) {
            int j = i;
            int balance = 0;
            while (j >= 0) {
                --j;
                if (sourceCode.charAt(j) == '(') {
                    ++balance;
                }
                if (sourceCode.charAt(j) == ')') --balance;
                if (balance == 1) break;
                if (sourceCode.charAt(j) == ',') break;
                if (sourceCode.charAt(j) == ' ') break;
                if (sourceCode.charAt(j) == '=') break;
                if (sourceCode.charAt(j + 1) == '(') break;
            }
            newSourceCode.append(sourceCode.substring(lastIndex, j + 1));
            newSourceCode.append("<LAMBDA>");
            newSourceCode.append(sourceCode.substring(j + 1, i));
            lastIndex = i;
        }
        newSourceCode.append(sourceCode.substring(lastIndex));
        return newSourceCode.toString();
    }
}
