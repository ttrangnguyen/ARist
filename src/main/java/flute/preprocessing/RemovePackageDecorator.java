package flute.preprocessing;

import java.io.File;

public class RemovePackageDecorator extends PreprocessDecorator {
    public RemovePackageDecorator(Preprocessor preprocessor) {
        super(preprocessor);
    }

    @Override
    public String preprocessFile(File file) {
        return RemovePackageDecorator.preprocess(super.preprocessFile(file));
    }

    public static String preprocess(String sourceCode) {
        return sourceCode.replaceAll("package[^;]*;", "");
    }
}
