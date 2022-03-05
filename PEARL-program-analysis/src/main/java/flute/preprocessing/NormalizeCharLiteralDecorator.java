package flute.preprocessing;

import java.io.File;

public class NormalizeCharLiteralDecorator extends PreprocessDecorator {
    public NormalizeCharLiteralDecorator(Preprocessor preprocessor) {
        super(preprocessor);
    }

    @Override
    public String preprocessFile(File file) {
        return NormalizeCharLiteralDecorator.preprocess(super.preprocessFile(file));
    }

    public static String preprocess(String sourceCode) {
        return sourceCode.replaceAll("'.?'", "0");
    }
}
