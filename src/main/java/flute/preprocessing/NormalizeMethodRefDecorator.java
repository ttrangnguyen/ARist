package flute.preprocessing;

import java.io.File;

public class NormalizeMethodRefDecorator extends Decorator {
    public NormalizeMethodRefDecorator(Preprocessor preprocessor) {
        super(preprocessor);
    }

    @Override
    public String preprocessFile(File file) {
        return NormalizeMethodRefDecorator.preprocess(super.preprocessFile(file));
    }

    /**
     * List::get -> ::
     * List::new -> List::new (unchanged)
     */
    public static String preprocess(String sourceCode) {
        sourceCode = sourceCode.replaceAll("::new", "::-new");
        sourceCode = sourceCode.replaceAll(NAME_REGEX+"::"+NAME_REGEX, "::");
        sourceCode = sourceCode.replaceAll("::-new", "::new");
        return sourceCode;
    }
}
