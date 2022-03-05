package flute.preprocessing;

import java.io.File;

public class NormalizeTypeLiteralDecorator extends PreprocessDecorator {
    public NormalizeTypeLiteralDecorator(Preprocessor preprocessor) {
        super(preprocessor);
    }

    @Override
    public String preprocessFile(File file) {
        return NormalizeTypeLiteralDecorator.preprocess(super.preprocessFile(file));
    }

    public static String preprocess(String sourceCode) {
        return sourceCode.replaceAll(NAME_REGEX+"\\.class", ".class");
    }
}
