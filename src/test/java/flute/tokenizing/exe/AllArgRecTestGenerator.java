package flute.tokenizing.exe;

import flute.jdtparser.ProjectParser;
import flute.tokenizing.excode_data.*;

import java.util.ArrayList;
import java.util.List;

public class AllArgRecTestGenerator extends MultipleArgRecTestGenerator {
    public AllArgRecTestGenerator(String projectPath, ProjectParser projectParser) {
        super(projectPath, projectParser);
    }

    public AllArgRecTestGenerator(ArgRecTestGenerator argRecTestGenerator) {
        super(argRecTestGenerator);
    }

    @Override
    public List<MultipleArgRecTest> generate(List<ArgRecTest> oneArgRecTests) {
        List<MultipleArgRecTest> tests = new ArrayList<>();
        List<ArgRecTest> pile = new ArrayList<>();
        for (int i = 0; i < oneArgRecTests.size(); ++i) {
            ArgRecTest oneArgTest = oneArgRecTests.get(i);
            pile.add(oneArgTest);
            if (i == oneArgRecTests.size() - 1 || oneArgRecTests.get(i + 1).getArgPos() <= 1) {
                MultipleArgRecTest test = new MultipleArgRecTest();
                if (pile.size() > 0) {
                    test.setLex_context(pile.get(0).getLex_context());
                    test.setExcode_context(pile.get(0).getExcode_context());
                    test.setMethodScope_name(pile.get(0).getMethodScope_name());
                    test.setClass_name(pile.get(0).getClass_name());
                    test.setMethodInvocClassQualifiedName(pile.get(0).getMethodInvocClassQualifiedName());
                }

                List<String> paramList = new ArrayList<>();
                for (int j = 0; j < pile.size(); ++j) {
                    if (!"".equals(pile.get(j).getParam_name())) {
                        paramList.add(pile.get(j).getParam_name());
                    }
                }
                test.setParam_list(paramList);

                StringBuilder expectedExcode = new StringBuilder();
                for (int j = 0; j < pile.size(); ++j) {
                    expectedExcode.append(pile.get(j).getExpected_excode());
                    if (j < pile.size() - 1) {
                        expectedExcode.append(' ');
                        expectedExcode.append(NodeSequenceInfo.getSEPA(NodeSequenceConstant.SEPA, ',').toStringSimplest());
                        expectedExcode.append(' ');
                    }
                }
                test.setExpected_excode(expectedExcode.toString());

                StringBuilder expectedLex = new StringBuilder();
                for (int j = 0; j < pile.size(); ++j) {
                    expectedLex.append(pile.get(j).getExpected_lex());
                    if (j < pile.size() - 1) expectedLex.append(", ");
                }
                test.setExpected_lex(expectedLex.toString());

                List<List<String>> allNextExcodeList = new ArrayList<>();
                List<List<List<String>>> allNextLexList = new ArrayList<>();
                List<List<List<Boolean>>> allIsLocalVarList = new ArrayList<>();
                test.setIgnored(false);
                for (int j = 0; j < pile.size(); ++j) {
                    allNextExcodeList.add(pile.get(j).getNext_excode());
                    allNextLexList.add(pile.get(j).getNext_lex());
                    allIsLocalVarList.add(pile.get(j).getIs_local_var());
                    if (pile.get(j).isIgnored()) {
                        test.setIgnored(true);
                    }
                }
                test.setNext_excode(allNextExcodeList);
                test.setNext_lex(allNextLexList);
                test.setIs_local_var(allIsLocalVarList);
                test.setArgRecTestList(pile);
                test.setNumArg(pile.get(pile.size() - 1).getArgPos());

                tests.add(test);

                pile = new ArrayList<>();
            }
        }
        return tests;
    }

    @Override
    void postProcess(List<RecTest> tests) {

    }
}
