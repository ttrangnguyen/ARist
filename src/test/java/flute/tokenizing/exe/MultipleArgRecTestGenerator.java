package flute.tokenizing.exe;

import com.github.javaparser.ast.expr.MethodCallExpr;
import flute.jdtparser.ProjectParser;
import flute.tokenizing.excode_data.ArgRecTest;
import flute.tokenizing.excode_data.MultipleArgRecTest;
import flute.tokenizing.excode_data.NodeSequenceInfo;
import flute.tokenizing.excode_data.RecTest;

import java.util.List;

public abstract class MultipleArgRecTestGenerator extends MethodCallRecTestGenerator {
    private ArgRecTestGenerator argRecTestGenerator;

    public MultipleArgRecTestGenerator(String projectPath, ProjectParser projectParser) {
        super(projectPath, projectParser);
        argRecTestGenerator = new ArgRecTestGenerator(projectPath, projectParser);
    }

    public MultipleArgRecTestGenerator(ArgRecTestGenerator argRecTestGenerator) {
        super(argRecTestGenerator.getTokenizer().getProject().getAbsolutePath(), argRecTestGenerator.getProjectParser());
        this.argRecTestGenerator = argRecTestGenerator;
    }

    @Override
    List<? extends RecTest> generateFromMethodCall(List<NodeSequenceInfo> excodes, int methodCallStartIdx, int methodCallEndIdx,
                                                   MethodCallExpr methodCall, String contextMethodCall, String methodName) {

        List<ArgRecTest> tests = (List<ArgRecTest>) argRecTestGenerator.generateFromMethodCall(excodes, methodCallStartIdx, methodCallEndIdx,
                methodCall, contextMethodCall, methodName);

        return generate(tests);
    }

    public abstract List<MultipleArgRecTest> generate(List<ArgRecTest> oneArgRecTests);
}
