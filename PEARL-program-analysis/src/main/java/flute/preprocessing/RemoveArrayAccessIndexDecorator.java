package flute.preprocessing;

import java.io.File;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemoveArrayAccessIndexDecorator extends PreprocessDecorator {
    public RemoveArrayAccessIndexDecorator(Preprocessor preprocessor) {
        super(preprocessor);
    }

    @Override
    public String preprocessFile(File file) {
        return RemoveArrayAccessIndexDecorator.preprocess(super.preprocessFile(file));
    }

    public static String preprocess(String sourceCode) {
        Matcher m = Pattern.compile("\\[|\\]|[^\\[\\]]+").matcher(sourceCode);
        Stack<String> stack = new Stack<>();
        while (m.find()) {
            String s = m.group();
            if (s.compareTo("]") == 0) {
                while (true) {
                    if (stack.empty()|| stack.pop().compareTo("[") == 0) break;
                }
            }
            stack.add(s);
        }
        StringBuilder sb = new StringBuilder();
        for (String s: stack) {
            if (s.compareTo("]") == 0) {
                sb.append('[');
            }
            sb.append(s);
        }
        return sb.toString();
    }
}
