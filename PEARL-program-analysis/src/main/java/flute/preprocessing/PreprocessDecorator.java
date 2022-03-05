package flute.preprocessing;

import java.io.File;

public abstract class PreprocessDecorator extends Preprocessor {
    private Preprocessor preprocessor;

    public PreprocessDecorator(Preprocessor preprocessor) {
        this.preprocessor = preprocessor;
    }

    @Override
    public String preprocessFile(File file) {
        return preprocessor.preprocessFile(file);
    }

    @Override
    public String revertFile(File file) {
        return preprocessor.revertFile(file);
    }
}
