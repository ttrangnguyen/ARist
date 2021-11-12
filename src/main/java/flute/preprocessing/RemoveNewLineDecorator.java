package flute.preprocessing;

import java.io.File;

public class RemoveNewLineDecorator extends PreprocessDecorator {
    public RemoveNewLineDecorator(Preprocessor preprocessor) {
        super(preprocessor);
    }

    @Override
    public String preprocessFile(File file) {
        return RemoveNewLineDecorator.preprocess(super.preprocessFile(file));
    }

    public static String preprocess(String sourceCode) {
        return sourceCode.replaceAll("((\r\n)|\n)+", " ");
    }
}
