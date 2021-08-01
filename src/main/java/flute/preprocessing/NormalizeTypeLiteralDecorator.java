package flute.preprocessing;

import java.io.File;

public class NormalizeTypeLiteralDecorator extends Decorator {
    public NormalizeTypeLiteralDecorator(Preprocessor preprocessor) {
        super(preprocessor);
    }

    @Override
    public String preprocessFile(File file) {
        return NormalizeTypeLiteralDecorator.preprocess(super.preprocessFile(file));
    }

    public static String preprocess(String sourceCode) {
        return sourceCode.replaceAll("[a-zA-Z_$][a-zA-Z\\d_$]*\\.class", ".class");
    }
}
