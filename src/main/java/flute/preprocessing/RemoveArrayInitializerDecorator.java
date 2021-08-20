package flute.preprocessing;

import java.io.File;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemoveArrayInitializerDecorator extends Decorator {
    public RemoveArrayInitializerDecorator(Preprocessor preprocessor) {
        super(preprocessor);
    }

    @Override
    public String preprocessFile(File file) {
        return RemoveArrayInitializerDecorator.preprocess(super.preprocessFile(file));
    }

    public static String preprocess(String sourceCode) {
        sourceCode = sourceCode.replaceAll("]\\{[^,;]+,", "],");
        sourceCode = sourceCode.replaceAll("]\\{[^,;]+;", "];");
        return sourceCode;
    }
}
