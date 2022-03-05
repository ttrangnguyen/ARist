package flute.tokenizing.exe;

import flute.jdtparser.ProjectParser;
import flute.tokenizing.excode_data.ArgRecTest;
import flute.tokenizing.excode_data.MultipleArgRecTest;
import flute.tokenizing.excode_data.RecTest;

import java.util.ArrayList;
import java.util.List;

public class SingleArgRecTestGenerator extends MultipleArgRecTestGenerator {
    public SingleArgRecTestGenerator(String projectPath, ProjectParser projectParser) {
        super(projectPath, projectParser);
    }

    public SingleArgRecTestGenerator(ArgRecTestGenerator argRecTestGenerator) {
        super(argRecTestGenerator);
    }

    @Override
    public List<MultipleArgRecTest> generate(List<ArgRecTest> oneArgRecTests) {
        List<MultipleArgRecTest> tests = new ArrayList<>();
        for (ArgRecTest test: oneArgRecTests) {
            tests.add(test.toSingleArgRecTest());
        }
        return tests;
    }

    @Override
    void postProcess(List<RecTest> tests) {

    }
}
