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
                    test.setLine(pile.get(0).getLine());
                    test.setCol(pile.get(0).getCol());
                    test.setClassHierarchy(pile.get(0).getClassHierarchy());
                    test.setLex_context(pile.get(0).getLex_context());
                    test.setExcode_context(pile.get(0).getExcode_context());
                    test.setMethodScope_name(pile.get(0).getMethodScope_name());
                    test.setClass_name(pile.get(0).getClass_name());
                    test.setMethodInvocClassQualifiedName(pile.get(0).getMethodInvocClassQualifiedName());
                    test.setFilePath(pile.get(0).getFilePath());
                    test.setPackageName(pile.get(0).getPackageName());
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
                List<String> allParamTypeKeyList = new ArrayList<>();
                List<List<List<Integer>>> allCandidatesLocality = new ArrayList<>();
                List<List<List<Integer>>> allCandidatesScopeDistance = new ArrayList<>();
                List<List<List<Integer>>> allCandidatesLastUsageDistance = new ArrayList<>();
                test.setIgnored(false);
                for (int j = 0; j < pile.size(); ++j) {
                    allNextExcodeList.add(pile.get(j).getNext_excode());
                    allNextLexList.add(pile.get(j).getNext_lex());
                    allParamTypeKeyList.add(pile.get(j).getParamTypeKey());
                    allCandidatesLocality.add(pile.get(j).getCandidates_locality());
                    allCandidatesScopeDistance.add(pile.get(j).getCandidates_scope_distance());
                    allCandidatesLastUsageDistance.add(pile.get(j).getCandidates_last_usage_distance());
                    if (pile.get(j).isIgnored()) {
                        test.setIgnored(true);
                    }
                }
                test.setNext_excode(allNextExcodeList);
                test.setNext_lex(allNextLexList);
                test.setParamTypeKeyList(allParamTypeKeyList);
                test.setCandidates_locality(allCandidatesLocality);
                test.setCandidates_scope_distance(allCandidatesScopeDistance);
                test.setCandidates_last_usage_distance(allCandidatesLastUsageDistance);
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
