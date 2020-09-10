package flute.tokenizing.exe;

import com.google.gson.Gson;
import flute.analysis.config.Config;
import flute.tokenizing.excode_data.ArgRecTest;
import flute.tokenizing.excode_data.ContextInfo;
import flute.tokenizing.excode_data.NodeSequenceInfo;

import java.util.List;

public class ArgRecTestGenerator {
    private JavaExcodeTokenizer tokenizer;
    private Gson gson = new Gson();

    public ArgRecTestGenerator(String projectPath) {
        tokenizer = new JavaExcodeTokenizer(projectPath);
    }

    public List<ArgRecTest> generate(String javaFilePath) {
        List<NodeSequenceInfo> excodes = tokenizer.tokenize(javaFilePath);
        int methodDecPos = -1;
        int methodAccessPos = -1;
        int contextPos = 0;

        int bracesBalance = 0;
        int parenthesesBalance = 0;
        for (int i = 0; i < excodes.size(); ++i) {
            NodeSequenceInfo excode = excodes.get(i);

            if (NodeSequenceInfo.isConstructorOrMethod(excode)) methodDecPos = i;
            if (NodeSequenceInfo.isOPBLK(excode)) bracesBalance += 1;
            else if (NodeSequenceInfo.isCLBLK(excode)) {
                bracesBalance -= 1;
                if (bracesBalance == 0) methodDecPos = -1;
            }

            if (methodDecPos >= 0) {
                if (methodAccessPos >= 0 && parenthesesBalance == 1
                    && (NodeSequenceInfo.isClosePart(excode) || NodeSequenceInfo.isSEPA(excode, ','))) {
                    ContextInfo context = new ContextInfo(excodes, contextPos);

                    ArgRecTest test = new ArgRecTest();
                    test.setExcode_context(context.getContextFromMethodDeclarationAsString());

                    System.out.println(gson.toJson(test));

                    contextPos = i;
                }
            }

            if (NodeSequenceInfo.isMethodAccess(excode)) methodAccessPos = i;
            if (NodeSequenceInfo.isOpenPart(excode)) {
                parenthesesBalance += 1;
                if (methodDecPos >= 0) {
                    if (methodAccessPos >= 0 && parenthesesBalance == 1) {
                        contextPos = i;
                    }
                }
            } else if (NodeSequenceInfo.isClosePart(excode)) {
                parenthesesBalance -= 1;
                if (parenthesesBalance == 0) methodAccessPos = -1;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        ArgRecTestGenerator generator = new ArgRecTestGenerator(Config.REPO_DIR + "sampleproj/");
        List<ArgRecTest> tests = generator.generate(Config.REPO_DIR + "sampleproj/src/Main.java");
    }
}
