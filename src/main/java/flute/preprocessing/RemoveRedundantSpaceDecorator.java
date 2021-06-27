package flute.preprocessing;

import java.io.File;

public class RemoveRedundantSpaceDecorator extends Decorator {
    public RemoveRedundantSpaceDecorator(Preprocessor preprocessor) {
        super(preprocessor);
    }

    @Override
    public String preprocessFile(File file) {
        return RemoveRedundantSpaceDecorator.preprocess(super.preprocessFile(file));
    }

    public static String preprocess(String sourceCode) {
        sourceCode = RemoveIndentDecorator.preprocess(sourceCode);
        sourceCode = sourceCode.replaceAll(" \\{", "{");
        sourceCode = sourceCode.replaceAll(" = ", "=");
        sourceCode = sourceCode.replaceAll(", ", ",");
        sourceCode = sourceCode.replaceAll("\\( ", "(");
        return sourceCode;
    }

    public static void main(String[] args) {
        System.out.println(RemoveRedundantSpaceDecorator.preprocess("if (chosenPrey == null)   Cat.super.position.reach(chosenPrey.position);"));
    }
}
