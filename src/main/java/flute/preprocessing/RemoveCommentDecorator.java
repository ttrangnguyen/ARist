package flute.preprocessing;

import flute.utils.file_processing.CommentRemover;

import java.io.File;

public class RemoveCommentDecorator extends Decorator {
    public RemoveCommentDecorator(Preprocessor preprocessor) {
        super(preprocessor);
    }

    @Override
    public String preprocessFile(File file) {
        return RemoveCommentDecorator.preprocess(super.preprocessFile(file));
    }

    public static String preprocess(String sourceCode) {
        return CommentRemover.removeCommentFromFileStringAfterParsing(sourceCode);
    }
}
