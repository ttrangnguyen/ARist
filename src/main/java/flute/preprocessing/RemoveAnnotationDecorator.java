package flute.preprocessing;

import java.io.File;

public class RemoveAnnotationDecorator extends PreprocessDecorator {
    public RemoveAnnotationDecorator(Preprocessor preprocessor) {
        super(preprocessor);
    }

    @Override
    public String preprocessFile(File file) {
        return RemoveAnnotationDecorator.preprocess(super.preprocessFile(file));
    }

    public static String preprocess(String sourceCode) {
        return sourceCode.replaceAll("@Override ", "");
    }
}
