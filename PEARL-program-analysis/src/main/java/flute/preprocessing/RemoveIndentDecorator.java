package flute.preprocessing;

import java.io.File;

public class RemoveIndentDecorator extends PreprocessDecorator {
    public RemoveIndentDecorator(Preprocessor preprocessor) {
        super(preprocessor);
    }

    @Override
    public String preprocessFile(File file) {
        return RemoveIndentDecorator.preprocess(super.preprocessFile(file));
    }

    public static String preprocess(String sourceCode) {
        return sourceCode.trim().replaceAll("( |\t)+", " ").replaceAll("\n ", "\n");
    }
}
