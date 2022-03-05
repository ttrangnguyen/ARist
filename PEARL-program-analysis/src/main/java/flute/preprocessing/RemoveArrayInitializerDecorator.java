package flute.preprocessing;

import java.io.File;

public class RemoveArrayInitializerDecorator extends PreprocessDecorator {
    public RemoveArrayInitializerDecorator(Preprocessor preprocessor) {
        super(preprocessor);
    }

    @Override
    public String preprocessFile(File file) {
        return RemoveArrayInitializerDecorator.preprocess(super.preprocessFile(file));
    }

    /**
     * Note: {@link EmptyStringLiteralDecorator#preprocess} must be used beforehand.
     */
    public static String preprocess(String sourceCode) {
        sourceCode = sourceCode.replaceAll("]\\{[^,;]+,", "],");
        sourceCode = sourceCode.replaceAll("]\\{[^,;]+;", "];");
        sourceCode = sourceCode.replaceAll("] \\{[^,;]+,", "],");
        sourceCode = sourceCode.replaceAll("] \\{[^,;]+;", "];");
        return sourceCode;
    }
}
