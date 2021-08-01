package flute.preprocessing;

import java.io.File;

public abstract class Decorator extends Preprocessor {
    private Preprocessor preprocessor;

    public Decorator(Preprocessor preprocessor) {
        this.preprocessor = preprocessor;
    }

    @Override
    public String preprocessFile(File file) {
        return preprocessor.preprocessFile(file);
    }
}
