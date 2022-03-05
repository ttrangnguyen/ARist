package flute.preprocessing;

import java.io.File;

public class EmptyStringLiteralDecorator extends PreprocessDecorator {
    public EmptyStringLiteralDecorator(Preprocessor preprocessor) {
        super(preprocessor);
    }

    @Override
    public String preprocessFile(File file) {
        return EmptyStringLiteralDecorator.preprocess(super.preprocessFile(file));
    }

    @Override
    public String preprocessText(String text) {
        return NormalizeLambdaExprDecorator.preprocess(super.preprocessText(text));
    }

    public static String preprocess(String sourceCode) {
        StringBuilder sb = new StringBuilder();
        boolean insideStringLiteral = false;
        for (String s: sourceCode.split("\\\\\"", -1)) {
            String[] t = s.replace("'\"'", "''").split("\"", -1);
            for (int i = 0; i < t.length; ++i) {
                if (!insideStringLiteral) sb.append(t[i].replace("''", "'\"'"));
                if (i < t.length - 1) {
                    sb.append('"');
                    insideStringLiteral = !insideStringLiteral;
                }
            }
        }
        return sb.toString();
    }
}
