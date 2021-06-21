package flute.preprocessing;

import java.io.File;

public class RemoveImportDecorator extends Decorator {
    public RemoveImportDecorator(Preprocessor preprocessor) {
        super(preprocessor);
    }

    @Override
    public String preprocessFile(File file) {
        return RemoveImportDecorator.preprocess(super.preprocessFile(file));
    }

    public static String preprocess(String sourceCode) {
        return sourceCode.replaceAll("import[^;]*;", "");
    }
}
