package flute.testing;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import flute.candidate.Modifier;
import flute.communicating.PredictionDetail;
import flute.communicating.PublicStaticEfficiencyData;
import flute.communicating.RecentClassEfficiencyData;
import flute.config.ModelConfig;
import flute.config.ProjectConfig;
import flute.config.TestConfig;
import flute.feature.ps.PsClass;
import flute.candidate.Candidate;
import flute.lexing.SLPJavaLexer;
import flute.matching.CandidateMatcher;
import flute.modeling.LexicalSimilaritySolver;
import flute.modeling.ModelManager;
import flute.modeling.SingleParamExcodeManager;
import flute.modeling.SingleParamLexicalManager;
import slp.core.lexing.code.JavaLexer;
import slp.core.modeling.dynamic.CacheModel;
import slp.core.modeling.mix.MixModel;
import slp.core.modeling.runners.ModelRunner;
import slp.core.translating.Vocabulary;
import slp.core.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SingleParamTestManager {
    SingleParamLexicalManager singleParamLexicalManager;
    SingleParamExcodeManager singleParamExcodeManager;
    public SingleParamTestCase testCase;
    public HashMap<String, HashMap<PsClass, List<Candidate>>> psCandsMapByType = new HashMap<>();
    public HashMap<PsClass, HashMap<String, List<Candidate>>> psCandsMapByClass = new HashMap<>();
    public HashMap<String, List<Candidate>> publicStaticMemberHM = new HashMap<>();
    public List<Candidate> psMemberList = new ArrayList<>();
    public HashSet<PsClass> recentPsClasses;
    public String lastFilePath = "";

    public BufferedReader localModelPredictions;

    public SingleParamTestManager() {
        if (ModelConfig.USE_PS) {
            initPublicStaticMemberHM();
        }
        singleParamLexicalManager = new SingleParamLexicalManager(ModelManager.READING);
        if (ModelConfig.USE_EXCODE) {
            singleParamExcodeManager = new SingleParamExcodeManager(ModelManager.READING);
        }
        if (ModelConfig.USE_NGRAM_GLOBAL) {
            if (!ProjectConfig.CUGLM) {
                String localModelPredictionPath = TestConfig.predictionDetailPath.replace("/result/", "/result-local/");
                try {
                    localModelPredictions = new BufferedReader(new FileReader(localModelPredictionPath));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                String localModelPredictionPath = TestConfig.predictionDetailPath.replace("cuglm", "cuglm-local");
                try {
                    localModelPredictions = new BufferedReader(new FileReader(localModelPredictionPath));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void initTestCase(String request) {
        testCase = new SingleParamTestCase(request);
    }

    public void resolveTestFilePath(String currentFilePath) {
        if (!currentFilePath.equals(lastFilePath)) {
            recentPsClasses = new HashSet<>();
            String testFileNoExtension = currentFilePath.substring(0, currentFilePath.length() - 4);
            if (ModelConfig.USE_LEXICAL) {
                String testFilePath = testFileNoExtension + "java";
                File testFile = new File(testFilePath);
                singleParamLexicalManager.modelRunner.getModel().notify(testFile);
                if (ModelConfig.CROSS_VALIDATION_PER_FILE) singleParamLexicalManager.modelRunner.forgetFile(testFile);
            }
            if (ModelConfig.USE_EXCODE) {
                String testFilePath = testFileNoExtension + "jexcode";
                File testFile = new File(testFilePath);
                singleParamExcodeManager.modelRunner.getModel().notify(testFile);
                if (ModelConfig.CROSS_VALIDATION_PER_FILE) singleParamExcodeManager.modelRunner.forgetFile(testFile);
            }
            lastFilePath = currentFilePath;
        }
    }

    public void resolveContext() {
        if (ModelConfig.USE_LEXICAL) {
            resolveLexicalContext();
        }
        if (ModelConfig.USE_EXCODE) {
            resolveExcodeContext();
        }
    }

    private void resolveLexicalContext() {
        Vocabulary vocabulary = singleParamLexicalManager.modelRunner.getVocabulary();
        testCase.lexContext = vocabulary.toIndices(testCase.request.lex_context);
        testCase.lexContext = testCase.lexContext.subList(
                Math.max(testCase.lexContext.size() - ModelConfig.NGRAM, 0), testCase.lexContext.size());
    }

    private void resolveExcodeContext() {
        List<String> contextStringTokens = Arrays.asList(testCase.request.excode_context.split(" "));
        Vocabulary vocabulary = singleParamExcodeManager.modelRunner.getVocabulary();
        testCase.excodeContext = vocabulary.toIndices(contextStringTokens);
    }

    public void resolveParamName() {
        JavaLexer lexer = new JavaLexer();
        if (testCase.request.param_name != null) {
            testCase.paramNameIndices = lexer.tokenizeLines(testCase.request.param_name).get(0);
        }
    }

    public void resolveParamTypeName() {
        JavaLexer lexer = new JavaLexer();
        if (testCase.request.paramTypeName != null) {
            testCase.paramTypeNameIndices = lexer.tokenizeLines(testCase.request.paramTypeName).get(0);
        }
    }

    public void resolveRealParam() {
        if (ModelConfig.USE_LEXICAL) {
            resolveRealLexicalParam();
        }
        if (ModelConfig.USE_EXCODE) {
            resolveRealExcodeParam();
        }
    }

    private void resolveRealLexicalParam() {
        JavaLexer lexer = new JavaLexer();
        Vocabulary vocabulary = singleParamLexicalManager.modelRunner.getVocabulary();
        List<String> paramTokens = lexer.tokenizeLines(testCase.request.expected_lex).get(0);
        testCase.realLexicalParamIndices = vocabulary.toIndices(paramTokens);
    }

    private void resolveRealExcodeParam() {
        Vocabulary vocabulary = singleParamExcodeManager.modelRunner.getVocabulary();
        List<String> paramTokens = Arrays.asList(testCase.request.expected_excode.split(" "));
        testCase.realExcodeParamIndices = vocabulary.toIndices(paramTokens);
    }

    public void addCandsMultipleParam(List<List<String>> excodeCands, List<List<List<String>>> lexicalCands) {
        for (int paramPos = 0; paramPos < excodeCands.size(); ++paramPos) {
            for (int excodePos = 0; excodePos < excodeCands.get(paramPos).size(); ++excodePos) {
                String excodeCand = excodeCands.get(paramPos).get(excodePos);
                if (testCase.excodeCands.contains(excodeCand)) {
                    testCase.lexicalCands.get(excodeCand).
                            addAll(lexicalCands.get(paramPos).get(excodePos).
                                    stream().map(Candidate::new).collect(Collectors.toSet()));
                } else {
                    testCase.excodeCands.add(excodeCand);
                    testCase.lexicalCands.put(excodeCand, new HashSet<>(
                            lexicalCands.get(paramPos).get(excodePos).
                            stream().map(Candidate::new).collect(Collectors.toSet())));
                }
            }
        }
    }

    public void addCandsSingleParam() {
        for (int excodePos = 0; excodePos < testCase.request.next_excode.size(); ++excodePos) {
            String excodeCand = testCase.request.next_excode.get(excodePos);
            HashSet<Candidate> hs = new HashSet<>();
            for (int i = 0; i < testCase.request.next_lex.get(excodePos).size(); ++i) {
                String lexCand = testCase.request.next_lex.get(excodePos).get(i);
                Candidate candidate = new Candidate(lexCand);
                candidate.defRecentness = testCase.request.candidates_scope_distance.get(excodePos).get(i);
                try {
                    candidate.useRecentness = testCase.request.candidates_last_usage_distance.get(excodePos).get(i);
                } catch (Exception ignored) {
                }
                hs.add(candidate);
            }
            testCase.lexicalCands.put(excodeCand, hs);
        }
        for (Map.Entry<String, HashSet<Candidate>> excodeCands : testCase.lexicalCands.entrySet()) {
            HashSet<Candidate> cands = excodeCands.getValue();
            ArrayList<Candidate> thisCands = new ArrayList<>();
            for (Candidate cand: cands) {
                if (cand.lexical.startsWith("this.")) {
                    Candidate thisCand = new Candidate(cand.lexical.substring(5));
                    thisCand.defRecentness = cand.defRecentness;
                    thisCand.useRecentness = cand.useRecentness;
                    thisCands.add(thisCand);
                }
            }
            cands.addAll(thisCands);
            excodeCands.setValue(cands);
        }
    }

    public void addCandsSpecialToken() {
        HashSet<Candidate> cands = new HashSet<>();
        for (String specialToken : ModelConfig.specialTokens) {
            cands.add(new Candidate("<" + specialToken + ">"));
        }
        testCase.lexicalCands.put("SPECIAL", cands);
    }

    public void initPublicStaticMemberHM() {
        initPublicStaticMemberHM(ProjectConfig.publicStaticMemberJre);
        initPublicStaticMemberHM(ProjectConfig.publicStaticMemberProject);
        for (Map.Entry<String, List<Candidate>> entry : publicStaticMemberHM.entrySet()) {
            HashMap<PsClass, List<Candidate>> classMems = new HashMap<>();
            for (Candidate mem : entry.getValue()) {
                String[] tokens = mem.lexical.split("\\.");
                List<Candidate> x = new ArrayList<>();
                PsClass psClass = new PsClass(tokens[0], mem.packageName);
                if (classMems.containsKey(psClass)) {
                    x = classMems.get(psClass);
                }
                x.add(mem);
                classMems.put(psClass, x);

                HashMap<String, List<Candidate>> classTypeMems;
                if (psCandsMapByClass.containsKey(psClass)) {
                    classTypeMems = psCandsMapByClass.get(psClass);
                } else classTypeMems = new HashMap<>();
                List<Candidate> tmp = new ArrayList<>();
                if (classTypeMems.containsKey(entry.getKey())) {
                    tmp = classTypeMems.get(entry.getKey());
                }
                tmp.add(mem);
                classTypeMems.put(entry.getKey(), tmp);
                psCandsMapByClass.put(psClass, classTypeMems);
            }
            psCandsMapByType.put(entry.getKey(), classMems);
        }
    }

    public void initPublicStaticMemberHM(String path) {
        try {
            System.out.println(path);
            Gson gson = new Gson();
            BufferedReader br = new BufferedReader(new FileReader(path));
            Type publicStaticMemberType = new TypeToken<List<Candidate>>() {}.getType();
            // adding fields
            ArrayList<Candidate> psMemberList = gson.fromJson(br.readLine(), publicStaticMemberType);
            // adding methods
            psMemberList.addAll(gson.fromJson(br.readLine(), publicStaticMemberType));
            for (Candidate member : psMemberList) {
                member.useRecentness = -1;
                member.defRecentness = -1;
                member.tokenizeSelf();
                if (Modifier.isPublic(member.modifier) && Modifier.isStatic(member.modifier)) {
                    if (publicStaticMemberHM.containsKey(member.key)) {
                        publicStaticMemberHM.get(member.key).add(member);
                    } else {
                        publicStaticMemberHM.put(member.key, new ArrayList<>(Collections.singletonList(member)));
                    }
                }
            }
            this.psMemberList.addAll(psMemberList);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void addPublicStaticMember() {
        long start_time_ps_checking = System.nanoTime();
        if (publicStaticMemberHM == null) initPublicStaticMemberHM();
        long step = System.nanoTime();
        HashMap<PsClass, Integer> classes = new HashMap<>();
        Set<String> validTypes;
        if (testCase.request.paramTypeKey != null && testCase.request.paramTypeKey.equals(TypeConstraintKey.OBJECT_TYPE)) {
            validTypes = psCandsMapByType.keySet();
        } else {
            validTypes = TypeConstraintKey.assignWith(testCase.request.paramTypeKey);
        }
        int totalPsCands = 0;
        for (String type : validTypes) {
            HashMap<PsClass, List<Candidate>> mems = psCandsMapByType.get(type);
            if (mems == null) continue;
            for (Map.Entry<PsClass, List<Candidate>> mem : mems.entrySet()) {
                if (classes.containsKey(mem.getKey())) {
                    classes.put(mem.getKey(), classes.get(mem.getKey()) + mem.getValue().size());
                } else {
                    classes.put(mem.getKey(), mem.getValue().size());
                }
                totalPsCands += mem.getValue().size();
            }
        }
        step = System.nanoTime();
        testCase.excodeCands.add(ModelConfig.PS_EXCODE);
        HashSet<String> psFilterTokens = new HashSet<>();
        // only use filter with primitive arguments
        boolean usePsFilterOverlap = (isPrimitiveOrStringArgument() || isObjectArgument()) && ModelConfig.USE_PS_FILTER_OVERLAP;
        if (usePsFilterOverlap) {
            addFilterTokens(psFilterTokens, testCase.request.method_name);
            addFilterTokens(psFilterTokens, testCase.request.methodInvoc);
            addFilterTokens(psFilterTokens, testCase.request.methodInvocCaller);
            String methodInvocClassName = "";
            if (testCase.request.methodInvocClassQualifiedName != null) {
                methodInvocClassName = getClassFromString(testCase.request.methodInvocClassQualifiedName);
            }
            if (!methodInvocClassName.isEmpty()) {
                methodInvocClassName = removeLowerCaseTokenFromMethodInvocClassName(methodInvocClassName);
                addFilterTokens(psFilterTokens, methodInvocClassName);
            }
            addFilterTokens(psFilterTokens, "min");
            addFilterTokens(psFilterTokens, "max");
            addFilterTokens(psFilterTokens, "minimum");
            addFilterTokens(psFilterTokens, "maximum");
        }

//        int currentPsCands = 0;

        double step1 = (1.0 * System.nanoTime() - step) / 1000000000;
        step = System.nanoTime();
        HashSet<Candidate> psCands = new HashSet<>();
        boolean ok = false;
        for (PsClass classCand : classes.keySet()) {
            if (usePsFilterOverlap) {
                ok = false;
                if (classCand.packagename != null && testCase.request.packageName != null) {
                    ok = testCase.request.packageName.equals(classCand.packagename);
                }
                String classCandFullname = classCand.packagename + "." + classCand.className;
                for (int i = 0; i < testCase.request.classHierarchy.size() - 1; ++i) {
                    ok |= testCase.request.classHierarchy.get(i).equals(classCandFullname);
                }
            }
            for (Map.Entry<String, List<Candidate>> typeMems: psCandsMapByClass.get(classCand).entrySet()) {
                if (validTypes.contains(typeMems.getKey())) {
                    List<Candidate> candidates = typeMems.getValue();
                    for (Candidate candidate : candidates) {
                        if (usePsFilterOverlap) {
                            if (!ok) if (!isOverlapped(psFilterTokens, candidate.lexicalTokens)) continue;
                        }
//                        currentPsCands += 1;
                        psCands.add(candidate);
                    }
                }
            }
        }

        testCase.lexicalCands.put(ModelConfig.PS_EXCODE, psCands);
        double step2 = (1.0 * System.nanoTime() - step) / 1000000000;
        step = System.nanoTime();
        if (ModelConfig.USE_PS_RECENT_CLASS) {
            HashSet<Candidate> psRecent = testCase.lexicalCands.get(ModelConfig.PS_EXCODE);
            for (String type : validTypes) {
                HashMap<PsClass, List<Candidate> > typeMembers = psCandsMapByType.get(type);
                if (typeMembers == null) continue;
                for (PsClass recentClass : recentPsClasses) {
                    if (typeMembers.containsKey(recentClass)) {
                        psRecent.addAll(typeMembers.get(recentClass));
                    }
                }
            }
            testCase.lexicalCands.put(ModelConfig.PS_EXCODE, psRecent);
        }

        double step3 = (1.0 * System.nanoTime() - step) / 1000000000;
        testCase.psCheckTime = (1.0 * System.nanoTime() - start_time_ps_checking) / 1000000000;
//        System.out.println("STEP1: " + step1);
//        System.out.println("STEP2: " + step2);
//        System.out.println("STEP3: " + step3);
//        System.out.println(testCase.request.test_id);
//        System.out.println(testCase.psCheckTime);
    }

    public PublicStaticEfficiencyData calculatePublicStaticMember() {
        PublicStaticEfficiencyData publicStaticEfficiencyData = new PublicStaticEfficiencyData();
        if (publicStaticMemberHM == null) initPublicStaticMemberHM();
        HashMap<PsClass, Integer> classes = new HashMap<>();
        Set<String> validTypes;
        if (testCase.request.paramTypeKey != null && testCase.request.paramTypeKey.equals(TypeConstraintKey.OBJECT_TYPE)) {
            validTypes = psCandsMapByType.keySet();
        } else {
            validTypes = TypeConstraintKey.assignWith(testCase.request.paramTypeKey);
        }
        int total = 0, reduced = 0;
        for (String type : validTypes) {
            HashMap<PsClass, List<Candidate>> mems = psCandsMapByType.get(type);
            if (mems == null) continue;
            for (Map.Entry<PsClass, List<Candidate>> mem : mems.entrySet()) {
                if (classes.containsKey(mem.getKey())) {
                    classes.put(mem.getKey(), classes.get(mem.getKey()) + mem.getValue().size());
                } else {
                    classes.put(mem.getKey(), mem.getValue().size());
                }
            }
        }

        testCase.excodeCands.add(ModelConfig.PS_EXCODE);
        HashSet<String> psFilterTokens = new HashSet<>();
        // only use filter with primitive arguments, string or object
        boolean usePsFilterOverlap = (isPrimitiveOrStringArgument() || isObjectArgument()) && ModelConfig.USE_PS_FILTER_OVERLAP;
        if (usePsFilterOverlap) {
            addFilterTokens(psFilterTokens, testCase.request.method_name);
            addFilterTokens(psFilterTokens, testCase.request.methodInvoc);
            addFilterTokens(psFilterTokens, testCase.request.methodInvocCaller);
            String methodInvocClassName = "";
            if (testCase.request.methodInvocClassQualifiedName != null) {
                methodInvocClassName = getClassFromString(testCase.request.methodInvocClassQualifiedName);
            }
            if (!methodInvocClassName.isEmpty()) {
                methodInvocClassName = removeLowerCaseTokenFromMethodInvocClassName(methodInvocClassName);
                addFilterTokens(psFilterTokens, methodInvocClassName);
            }
            addFilterTokens(psFilterTokens, "min");
            addFilterTokens(psFilterTokens, "max");
            addFilterTokens(psFilterTokens, "minimum");
            addFilterTokens(psFilterTokens, "maximum");
        }

        String[] tokens = testCase.request.filePath.trim().split("/");
        String className = tokens[tokens.length - 1].split("\\.")[0];
        publicStaticEfficiencyData.containTargetBeforeReduced = -1;
        publicStaticEfficiencyData.containTargetAfterReduced = -1;

        for (PsClass classCand : classes.keySet()) {
            for (Map.Entry<String, List<Candidate>> typeMems: psCandsMapByClass.get(classCand).entrySet()) {
                if (!validTypes.contains(typeMems.getKey())) continue;
                List<Candidate> candidates = typeMems.getValue();
                for (Candidate candidate : candidates) {
                    total += 1;
                    boolean isPb = false;
                    if (testCase.request.argType != null) {
                        if (testCase.request.argType.equals("FIELD_ACCESS") || testCase.request.argType.equals("NAME")) {
                            tokens = testCase.request.expected_lex.split("\\.");
                            String field = tokens[tokens.length - 1];
                            if (candidate.lexical.equals(testCase.request.expected_lex) || candidate.lexical.equals(className + "." + field) ||
                                    (candidate.packageName + "." + candidate.lexical).equals(testCase.request.expected_lex) ||
                                    (candidate.packageName + "." + candidate.lexical).equals(className + "." + field)) {
                                isPb = true;
                            }
                        } else if (testCase.request.argType.equals("METHOD_INVOC")) {
                            tokens = testCase.request.expected_lex.split("\\("); //org.a.b.c().d(x, y) -> org.a.b.c
                            String method = tokens[0]; //can be this.method or method or class.method
                            tokens = method.split("\\."); // org.a.b.c -> c
                            String methodName = tokens[tokens.length - 1]; //method
                            if (candidate.lexical.equals(method + "(") || candidate.lexical.equals(className + "." + methodName + "(") ||
                                    (candidate.packageName + "." + candidate.lexical).equals(method + "(") ||
                                    (candidate.packageName + "." + candidate.lexical).equals(className + "." + methodName + "(")) {
                                isPb = true;
                            }
                        }
                    }
                    if (isPb && publicStaticEfficiencyData.containTargetBeforeReduced == -1)
                        publicStaticEfficiencyData.containTargetBeforeReduced = total;

                    if (usePsFilterOverlap) {
                        boolean ok = isOverlapped(psFilterTokens, candidate.lexicalTokens);
                        if (classCand.packagename != null && testCase.request.packageName != null) {
                            ok |= testCase.request.packageName.equals(classCand.packagename);
                        }
                        String classCandFullname = classCand.packagename+"."+classCand.className;
                        for (int i = 0; i < testCase.request.classHierarchy.size() - 1; ++i) {
                            ok |= testCase.request.classHierarchy.get(i).equals(classCandFullname);
                        }
                        if (!ok) continue;
                    }
                    reduced += 1;
                    if (isPb && publicStaticEfficiencyData.containTargetAfterReduced == -1)
                        publicStaticEfficiencyData.containTargetAfterReduced = reduced;
                }
            }
        }
        publicStaticEfficiencyData.fold = Integer.parseInt(TestConfig.fold);
        publicStaticEfficiencyData.test_id = testCase.request.test_id;
        publicStaticEfficiencyData.reduced = reduced;
        publicStaticEfficiencyData.total = total;
        publicStaticEfficiencyData.paramTypeKey = testCase.request.paramTypeKey;
        return publicStaticEfficiencyData;
    }

    private String removeLowerCaseTokenFromMethodInvocClassName(String s) {
        boolean pause=false;
        StringBuilder sb = new StringBuilder();
        sb.append(s.charAt(0));
        for(int i = 1; i < s.length(); ++i) {
            if (Character.isLowerCase(s.charAt(i)) && !Character.isLetter(s.charAt(i - 1))) {
                pause = true;
            } else if (Character.isUpperCase(s.charAt(i))) {
                pause = false;
            }
            if (!pause || (s.charAt(i) != '.' && !Character.isLetter(s.charAt(i)))) {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

    public RecentClassEfficiencyData calculateRecentClassEfficiency() {
        RecentClassEfficiencyData recentClassEfficiencyData = new RecentClassEfficiencyData();
        if (publicStaticMemberHM == null) initPublicStaticMemberHM();
        HashMap<PsClass, Integer> classes = new HashMap<>();
        Set<String> validTypes;
        if (testCase.request.paramTypeKey != null && testCase.request.paramTypeKey.equals(TypeConstraintKey.OBJECT_TYPE)) {
            validTypes = psCandsMapByType.keySet();
        } else {
            validTypes = TypeConstraintKey.assignWith(testCase.request.paramTypeKey);
        }
        for (String type : validTypes) {
            HashMap<PsClass, List<Candidate>> mems = psCandsMapByType.get(type);
            if (mems == null) continue;
            for (Map.Entry<PsClass, List<Candidate>> mem : mems.entrySet()) {
                if (classes.containsKey(mem.getKey())) {
                    classes.put(mem.getKey(), classes.get(mem.getKey()) + mem.getValue().size());
                } else {
                    classes.put(mem.getKey(), mem.getValue().size());
                }
            }
        }
        String[] tokens = testCase.request.filePath.trim().split("/");
        String className = tokens[tokens.length - 1].split("\\.")[0];
        recentClassEfficiencyData.containTarget = 0;
        if (ModelConfig.USE_PS_RECENT_CLASS) {
            for (String type : validTypes) {
                HashMap<PsClass, List<Candidate> > typeMembers = psCandsMapByType.get(type);
                if (typeMembers == null) continue;
                for (PsClass recentClass : recentPsClasses) {
                    if (typeMembers.containsKey(recentClass)) {
                        List<Candidate> classMembers = typeMembers.get(recentClass);
                        for (Candidate candidate : classMembers) {
                            if (testCase.request.argType != null) {
                                if (testCase.request.argType.equals("FIELD_ACCESS") || testCase.request.argType.equals("NAME")) {
                                    tokens = testCase.request.expected_lex.split("\\.");
                                    String field = tokens[tokens.length - 1];
                                    if  (candidate.lexical.equals(testCase.request.expected_lex) || candidate.lexical.equals(className + "." + field) ||
                                            (candidate.packageName + "." + candidate.lexical).equals(testCase.request.expected_lex) ||
                                            (candidate.packageName + "." + candidate.lexical).equals(className + "." + field)) {
                                        recentClassEfficiencyData.containTarget = 1;
                                    }
                                } else if (testCase.request.argType.equals("METHOD_INVOC")) {
                                    tokens = testCase.request.expected_lex.split("\\("); //org.a.b.c().d(x, y) -> org.a.b.c
                                    String method = tokens[0]; //can be this.method or method or class.method
                                    tokens = method.split("\\."); // org.a.b.c -> c
                                    String methodName = tokens[tokens.length - 1]; //method
                                    if (candidate.lexical.equals(method + "(") || candidate.lexical.equals(className + "." + methodName + "(") ||
                                            (candidate.packageName + "." + candidate.lexical).equals(method + "(") ||
                                            (candidate.packageName + "." + candidate.lexical).equals(className + "." + methodName + "(")) {
                                        recentClassEfficiencyData.containTarget = 1;
                                    }
                                }
                            }
                        }
                        recentClassEfficiencyData.totalRecent += classMembers.size();
                    }
                }
            }
        }
        addRecentClass();
        recentClassEfficiencyData.test_id = testCase.request.test_id;
        recentClassEfficiencyData.paramTypeKey = testCase.request.paramTypeKey;
        recentClassEfficiencyData.fold = Integer.parseInt(TestConfig.fold);
        return recentClassEfficiencyData;
    }

    private void addRecentClass() {
        if (testCase.request.paramTypeKey != null && !testCase.request.expected_excode.startsWith("LIT")) {
            if (!foundRecentClassInExpectedLex()) {
                // check if candidate is a field of current class
                String currentClassName = getClassFromString(testCase.request.classHierarchy.get(0));
                String currentClassPackage = getPackageFromString(testCase.request.classHierarchy.get(0));
                PsClass psClass = new PsClass(currentClassName, currentClassPackage);
                if (!psCandsMapByClass.containsKey(psClass)) return;
                String probableClassCand = psClass.className + "." + testCase.request.expected_lex;
                for (Map.Entry<String, List<Candidate>> t: psCandsMapByClass.get(psClass).entrySet()) {
                    if (!TypeConstraintKey.assignWith(t.getKey(), testCase.request.paramTypeKey)) continue;
                    for (Candidate candidate: t.getValue()) {
                        if (candidate.lexical.equals(probableClassCand)) {
                            recentPsClasses.add(psClass);
                            return;
                        }
                    }
                }
            }
        }
    }

    private boolean foundRecentClassInExpectedLex() {
        String expectedLex = testCase.request.expected_lex;
        String recentType = getTypeFromExcodeSequence(testCase.request.expected_excode);
        if (recentType == null) return false;
        String className = normalizeType(recentType);
        if (className == null) return false;
        // java.a.b.c.Clazz....
        int idxClassInExpectedLex = expectedLex.indexOf(className);
        if (idxClassInExpectedLex == -1) return false;
        String probableClassCand = "";
        for (int i = idxClassInExpectedLex; i <= expectedLex.length(); ++i) {
            if (expectedLex.length() == i) {
                probableClassCand = expectedLex.substring(idxClassInExpectedLex);
            } else if (expectedLex.charAt(i) == '(') {
                probableClassCand = expectedLex.substring(idxClassInExpectedLex, i+1);
                break;
            }
        }
        for (PsClass psClass : psCandsMapByClass.keySet()) {
            if (psClass.className.equals(className)) {
                for (Map.Entry<String, List<Candidate>> t: psCandsMapByClass.get(psClass).entrySet()) {
                    if (!TypeConstraintKey.assignWith(t.getKey(), testCase.request.paramTypeKey)) continue;
                    for (Candidate candidate: t.getValue()) {
                        if (candidate.lexical.equals(probableClassCand)) {
                            recentPsClasses.add(psClass);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean isPrimitiveOrStringArgument() {
        if (testCase.request.paramTypeKey != null &&
                !TypeConstraintKey.NUM_TYPES.contains(testCase.request.paramTypeKey) &&
                !TypeConstraintKey.BOOL_TYPES.contains(testCase.request.paramTypeKey) &&
                !testCase.request.paramTypeKey.equals(TypeConstraintKey.STRING_TYPE)) {
            return false;
        }
        return true;
    }

    public boolean isObjectArgument() {
         return testCase.request.paramTypeKey.equals(TypeConstraintKey.OBJECT_TYPE);
    }

    public void addFilterTokens(HashSet<String> filterTokens, String filterCriterion) {
        JavaLexer lexer = new JavaLexer();
        ModelConfig.TokenizedType tmp = ModelConfig.tokenizedType;
        ModelConfig.tokenizedType = ModelConfig.TokenizedType.SUB_TOKEN;
        filterTokens.addAll(lexer.tokenizeLines(filterCriterion).get(0)
                .stream()
                .map(String::toLowerCase)
                .filter(c -> Character.isLetter(c.charAt(0)) || Character.isDigit(c.charAt(0)))
                .collect(Collectors.toList()));
        ModelConfig.tokenizedType = tmp;
    }

    public boolean isOverlapped(HashSet<String> targetTokens, Set<String> patternTokens) {
        assert patternTokens != null;
        for (String patternToken: patternTokens) {
            char c = patternToken.charAt(0);
            if (Character.isLetter(c) || Character.isDigit(c)) {
                if (targetTokens.contains(patternToken.toLowerCase())) return true;
            }
        }
        return false;
    }

    public String getPackageFromString(String s) {
        // i should start at 0 but it will produce error and I'm lazy to fix
        for (int i = 1; i < s.length(); ++i) {
            if (Character.isUpperCase(s.charAt(i))) {
                return s.substring(0,i-1);
            }
        }
        return "";
    }

    public String getClassFromString(String s) {
        for (int i = 0; i < s.length(); ++i) {
            if (Character.isUpperCase(s.charAt(i))) {
                return s.substring(i);
            }
        }
        return "";
    }

    public void scoreSequences() {
        if (ModelConfig.USE_NGRAM_GLOBAL) {
            scoreSequencesNgramGlobal();
        } else if (ModelConfig.USE_BEAM_SEARCH) {
            scoreSequencesBeamSearch();
        } else {
            scoreSequencesPA();
        }
    }

    public void scoreSequencesNgramGlobal() {
        long startTimeRanking = System.nanoTime();
        testCase.numberOfCands = 0;
        ArrayList<ScoreInfo> scores = new ArrayList<>();
        JavaLexer lexer = new JavaLexer();
        Vocabulary lexicalvocabulary = singleParamLexicalManager.modelRunner.getVocabulary();
        int lexicalContextLen = testCase.lexContext.size();
        Gson gson = new Gson();
        PredictionDetail predictionDetail;
        List<String> localPredictions = new ArrayList<>();
        try {
            predictionDetail = gson.fromJson(localModelPredictions.readLine(), PredictionDetail.class);
            localPredictions = predictionDetail.predictions;
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int ii = 0; ii < Math.min(20, localPredictions.size()); ++ii) {
            String lexicalCand = localPredictions.get(ii);
            List<String> candTokens = lexer.tokenizeLines(lexicalCand).get(0);
            List<Integer> candTokensIndices = lexicalvocabulary.toIndices(candTokens);
            testCase.lexContext = Stream.of(testCase.lexContext, candTokensIndices)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
            ScoreInfo score = new ScoreInfo();
            score.candidate = new Candidate(lexicalCand);

            List<Map<Integer, Pair<Double, Double>>> predictions
                    = singleParamLexicalManager.modelRunner.getModel().predict(testCase.lexContext, lexicalContextLen);
            for (int i = 0; i < predictions.size(); ++i) {
                Map<Integer, Pair<Double, Double>> mp = predictions.get(i);
                Pair<Double, Double> prob = mp.get(testCase.lexContext.get(lexicalContextLen + i));
                score.lexModelScore += modifiedScore(prob.left);
            }

            score.totalScore = score.lexModelScore;
            scores.add(score);
            testCase.lexContext = testCase.lexContext.subList(0, lexicalContextLen);
        }
        scores.sort(Comparator.comparing(p -> -p.totalScore));
        testCase.scores = scores;
        testCase.rankingTime = (1.0 * System.nanoTime() - startTimeRanking) / 1000000000;
    }

    public void scoreSequencesBeamSearch() {
        long startTimeRanking = System.nanoTime();

        int contextLen = testCase.lexContext.size();
        singleParamLexicalManager.modelRunner.getModel().setDynamic(false);

        // pair of score and cand tokens
        ArrayList<Pair<Double, List<Integer>>> topCands = new ArrayList<>();
        topCands.add(new Pair<>(0.0, new ArrayList<>()));
        Vocabulary lexicalvocabulary = singleParamLexicalManager.modelRunner.getVocabulary();
        ArrayList<Pair<Double, List<Integer>>> completeCandidates = new ArrayList<>();
        for (int iteration = 1; iteration <= ModelConfig.BEAM_SEARCH_MAX_ITERATION; ++iteration) {
            ArrayList<Pair<Double, List<Integer>>> tmpTopCands = new ArrayList<>();
            for (Pair<Double, List<Integer>> cand : topCands) {
                testCase.lexContext.addAll(cand.right);
                // the next line adds a random token at the end
                testCase.lexContext.add(0);
                Map<Integer, Pair<Double, Double>> predictions = singleParamLexicalManager.modelRunner.getModel()
                        .predict(testCase.lexContext, contextLen + cand.right.size()).get(0);
                for (Map.Entry<Integer,Pair<Double, Double>> entry : predictions.entrySet()) {
                    Integer indice = entry.getKey();
                    String token = lexicalvocabulary.toWord(indice);
                    List<Integer> newCand = new ArrayList<>(cand.right);
                    newCand.add(indice);
                    Double score = cand.left + Math.log(entry.getValue().left);
                    switch (token) {
                        case ",":
                            completeCandidates.add(new Pair<>(score, cand.right));
                            continue;
                        case "(":
                            completeCandidates.add(new Pair<>(score, newCand));
                            break;
                        case "<LAMBDA>":
                            if (iteration == 1)
                                completeCandidates.add(new Pair<>(score, newCand));
                            continue;
                    }
                    tmpTopCands.add(new Pair<>(score, newCand));
                }
                testCase.lexContext = testCase.lexContext.subList(0, contextLen);
            }
            tmpTopCands.sort(Comparator.comparing(p -> -p.left));
            topCands = new ArrayList<>();
            for (int i = 0; i < Math.min(ModelConfig.BEAM_WIDTH, tmpTopCands.size()); ++i) {
                topCands.add(tmpTopCands.get(i));
            }
        }
        completeCandidates.sort(Comparator.comparing(p -> -p.left));
        testCase.scores = new ArrayList<>();
        for (int i = 0; i < Math.min(completeCandidates.size(), TestConfig.TOP_K); ++i) {
            String candidate = completeCandidates.get(i).right.stream()
                    .map(lexicalvocabulary::toWord)
                    .collect(Collectors.joining(" "));
            ScoreInfo scoreInfo = new ScoreInfo();
            scoreInfo.lexModelScore = completeCandidates.get(i).left;
            scoreInfo.candidate = new Candidate(candidate);
            testCase.scores.add(scoreInfo);
        }
        if (ModelConfig.USE_DYNAMIC) {
            singleParamLexicalManager.modelRunner.getModel().setDynamic(true);
        }
        testCase.rankingTime = (1.0 * System.nanoTime() - startTimeRanking) / 1000000000;
    }

    public void scoreSequencesPA() {
        long startTimeRanking = System.nanoTime();
        testCase.numberOfCands = 0;
        ArrayList<ScoreInfo> scores = new ArrayList<>();
        JavaLexer lexer = new JavaLexer();
        Vocabulary lexicalvocabulary = singleParamLexicalManager.modelRunner.getVocabulary();
        List<String> endParamTokens = new ArrayList<>();
        endParamTokens.add(")");
        endParamTokens.add(",");
        List<Integer> endParamIndices = lexicalvocabulary.toIndices(endParamTokens);
        Integer closeParenIndice = endParamIndices.get(0);
        Integer commaIndice = endParamIndices.get(1);
        int lexicalContextLen = testCase.lexContext.size();
//        int excodeContextLen = testCase.excodeContext.size();
        if (ModelConfig.USE_LEXICAL && ModelConfig.USE_DYNAMIC) {
            singleParamLexicalManager.modelRunner.getModel().setDynamic(false);
        }
        if (ModelConfig.USE_EXCODE) {
            singleParamExcodeManager.modelRunner.getModel().setDynamic(false);
        }
        HashSet<Character> nonAugmentDetectors = new HashSet<>();
        nonAugmentDetectors.add('(');
        nonAugmentDetectors.add('"');
        nonAugmentDetectors.add('[');
        nonAugmentDetectors.add(']');
        for (Map.Entry<String, HashSet<Candidate>> entry : testCase.lexicalCands.entrySet()) {
//            String excodeCand = entry.getKey();
//            double excodeScore = 0.0;
//            if (ModelConfig.USE_EXCODE) {
//                Vocabulary excodeVocabulary = singleParamExcodeManager.modelRunner.getVocabulary();
//                List<String> candTokens = Arrays.asList(excodeCand.split(" "));
//                List<Integer> candIndices = excodeVocabulary.toIndices(candTokens);
//                testCase.excodeContext.addAll(candIndices);
//                List<Map<Integer, Pair<Double, Double>>> predictions
//                        = singleParamExcodeManager.modelRunner.getModel().predict(testCase.excodeContext, excodeContextLen);
//                for (int i = 0; i < predictions.size(); ++i) {
//                    Map<Integer, Pair<Double, Double>> mp = predictions.get(i);
//                    Pair<Double, Double> prob = mp.get(testCase.excodeContext.get(excodeContextLen + i));
//                    excodeScore += modifiedScore(prob.left);
//                }
//                testCase.excodeContext = testCase.excodeContext.subList(0, excodeContextLen);
//            }

            HashSet<Candidate> lexicalCands = entry.getValue();
            testCase.numberOfCands += lexicalCands.size();
            for (Candidate lexicalCand : lexicalCands) {
                List<String> candTokens = lexer.tokenizeLines(lexicalCand.lexical).get(0);
                List<Integer> candTokensIndices = lexicalvocabulary.toIndices(candTokens);
                testCase.lexContext = Stream.of(testCase.lexContext, candTokensIndices)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());
                ScoreInfo score = new ScoreInfo();
                score.candidate = lexicalCand;

                if (ModelConfig.USE_LEXICAL) {
                    if (!ModelConfig.CHECK_END_TOKEN) {
                        List<Map<Integer, Pair<Double, Double>>> predictions
                                = singleParamLexicalManager.modelRunner.getModel().predict(testCase.lexContext, lexicalContextLen);
                        for (int i = 0; i < predictions.size(); ++i) {
                            Map<Integer, Pair<Double, Double>> mp = predictions.get(i);
                            Pair<Double, Double> prob = mp.get(testCase.lexContext.get(lexicalContextLen + i));
                            score.lexModelScore += modifiedScore(prob.left);
                        }
                    } else {
                        if (!nonAugmentDetectors.contains(lexicalCand.lexical.charAt(lexicalCand.lexical.length()-1))) {
                            Double closeParenScore = 0.0;
                            Double commaScore;
                            testCase.lexContext.add(closeParenIndice);
                            List<Map<Integer, Pair<Double, Double>>> predictions
                                    = singleParamLexicalManager.modelRunner.getModel().predict(testCase.lexContext, lexicalContextLen);
                            for (int i = 0; i < predictions.size(); ++i) {
                                Map<Integer, Pair<Double, Double>> mp = predictions.get(i);
                                Pair<Double, Double> prob = mp.get(testCase.lexContext.get(lexicalContextLen + i));
                                if (i < predictions.size()-1) {
                                    score.lexModelScore += modifiedScore(prob.left);
                                } else {
                                    closeParenScore = prob.left;
                                }
                            }
                            testCase.lexContext.remove(testCase.lexContext.size()-1);
                            testCase.lexContext.add(commaIndice);
                            predictions = singleParamLexicalManager.modelRunner.getModel().predict(testCase.lexContext, testCase.lexContext.size()-1);
                            Map<Integer, Pair<Double, Double>> mp = predictions.get(0);
                            Pair<Double, Double> prob = mp.get(commaIndice);
                            commaScore = prob.left;
                            if (closeParenScore.isInfinite() && commaScore.isInfinite()) {
                                score.lexModelScore += TestConfig.INFINITE_NEGATIVE;
                            } else {
                                double sum = 0.0;
                                if (!closeParenScore.isInfinite()) {
                                    sum += closeParenScore;
                                }
                                if (!commaScore.isInfinite()) {
                                    sum += commaScore;
                                }
                                score.lexModelScore += modifiedScore(sum);
                            }
                        }
                        else {
                            List<Map<Integer, Pair<Double, Double>>> predictions
                                    = singleParamLexicalManager.modelRunner.getModel().predict(testCase.lexContext, lexicalContextLen);
                            for (int i = 0; i < predictions.size(); ++i) {
                                Map<Integer, Pair<Double, Double>> mp = predictions.get(i);
                                Pair<Double, Double> prob = mp.get(testCase.lexContext.get(lexicalContextLen + i));
                                score.lexModelScore += modifiedScore(prob.left);
                            }
                        }
                    }
                }
//                if (ModelConfig.USE_EXCODE) {
//                    score += excodeScore;
//                }
                if (ModelConfig.USE_LEXSIM) {
                    score.lexSimScore = TestConfig.INFINITE_NEGATIVE;
                    if (testCase.request.param_name != null) {
                        score.lexSimScore = modifiedScore(
                                LexicalSimilaritySolver.lexicalSimilarity(candTokens, testCase.paramNameIndices));
                    }
                    if (testCase.request.paramTypeName != null) {
                        score.lexSimScore = Math.max(score.lexSimScore, modifiedScore(
                                LexicalSimilaritySolver.lexicalSimilarity(candTokens, testCase.paramTypeNameIndices)));
                    }
                }
                score.totalScore = score.lexModelScore + score.lexSimScore;
                if (ModelConfig.USE_RECENTNESS) {
                    score.defRecentness = lexicalCand.defRecentness;
                    score.useRecentness = lexicalCand.useRecentness;
//                    double normalizedDefRecentness = Math.max(score.defRecentness, 0);
//                    double normalizedUseRecentness = score.useRecentness == -1 ? 0 : score.useRecentness == 0 ? 3.5 : score.useRecentness;
                    Double defR = score.defRecentness == -1 ? 1 : ModelConfig.defRecentness.get(lexicalCand.defRecentness);
                    Double useR = score.defRecentness == -1 ? 1 : ModelConfig.useRecentness.get(lexicalCand.useRecentness);
//                    score.totalScore -= modifiedScore(1 + normalizedDefRecentness + normalizedUseRecentness);
                    score.totalScore += modifiedScore(defR) + modifiedScore(useR);
                }
                scores.add(score);
                testCase.lexContext = testCase.lexContext.subList(0, lexicalContextLen);
            }
        }
        if (ModelConfig.USE_LEXICAL && ModelConfig.USE_DYNAMIC) {
            singleParamLexicalManager.modelRunner.getModel().setDynamic(true);
        }
        if (ModelConfig.USE_EXCODE) {
            singleParamExcodeManager.modelRunner.getModel().setDynamic(true);
        }
        scores.sort(Comparator.comparing(p -> -p.totalScore));
        testCase.scores = scores;
        testCase.rankingTime = (1.0 * System.nanoTime() - startTimeRanking) / 1000000000;
    }

    // deprecated
    public void scoreFirstToken() {
        ArrayList<ScoreInfo> scores = new ArrayList<>();
        SLPJavaLexer slpLexer = new SLPJavaLexer();
        JavaLexer fluteLexer = new JavaLexer();
        int lexicalContextLen = testCase.lexContext.size();
        HashSet<String> firstTokenCandidates = new HashSet<>();
        for (Map.Entry<String, HashSet<Candidate>> entry : testCase.lexicalCands.entrySet()) {
            HashSet<Candidate> lexicalCands = entry.getValue();
            for (Candidate lexicalCand : lexicalCands) {
                firstTokenCandidates.add(slpLexer.lexLine(lexicalCand.lexical).collect(Collectors.toList()).get(0));
            }
        }
        if (slpLexer.lexLine(testCase.normalizedExpectedLex).count() > 0)
            firstTokenCandidates.add(slpLexer.lexLine(testCase.normalizedExpectedLex).collect(Collectors.toList()).get(0));
        Vocabulary lexicalvocabulary = singleParamLexicalManager.modelRunner.getVocabulary();
        if (ModelConfig.USE_LEXICAL && ModelConfig.USE_DYNAMIC) {
            singleParamLexicalManager.modelRunner.getModel().setDynamic(false);
        }
        for (String lexicalCand : firstTokenCandidates) {
            List<String> candTokens = fluteLexer.tokenizeLines(lexicalCand).get(0);
            List<Integer> candTokensIndices = lexicalvocabulary.toIndices(candTokens);
            testCase.lexContext = Stream.of(testCase.lexContext, candTokensIndices)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
            ScoreInfo score = new ScoreInfo();
            score.candidate = new Candidate(lexicalCand);
            if (ModelConfig.USE_LEXICAL) {
                List<Map<Integer, Pair<Double, Double>>> predictions
                        = singleParamLexicalManager.modelRunner.getModel().predict(testCase.lexContext, lexicalContextLen);
                for (int i = 0; i < predictions.size(); ++i) {
                    Map<Integer, Pair<Double, Double>> mp = predictions.get(i);
                    Pair<Double, Double> prob = mp.get(testCase.lexContext.get(lexicalContextLen + i));
                    score.lexModelScore += modifiedScore(prob.left);
                }
            }
            score.totalScore = score.lexModelScore;
            scores.add(score);
            testCase.lexContext = testCase.lexContext.subList(0, lexicalContextLen);
        }

        if (ModelConfig.USE_LEXICAL && ModelConfig.USE_DYNAMIC) {
            singleParamLexicalManager.modelRunner.getModel().setDynamic(true);
        }
        scores.sort(Comparator.comparing(p -> -p.totalScore));
        testCase.scores = scores;
    }

    public double modifiedScore(Double d) {
        if (Math.abs(d) <= 1e-6) return TestConfig.INFINITE_NEGATIVE;
        if (d.isInfinite()) return TestConfig.INFINITE_NEGATIVE;
        return Math.log(d);
    }

    public void postProcessing() {
        if (ModelConfig.USE_PS_RECENT_CLASS) {
            addRecentClass();
        }

        if (ModelConfig.USE_LEXICAL) {
            testCase.lexContext.addAll(testCase.realLexicalParamIndices);
            if (ModelConfig.USE_DYNAMIC) {
                singleParamLexicalManager.modelRunner.getModel().learn(testCase.lexContext);
            }

            if (ModelConfig.USE_CACHE) {
                ModelRunner modelRunner = singleParamLexicalManager.modelRunner;
                MixModel mixModel = (MixModel) modelRunner.getModel();
                CacheModel cache = (CacheModel) mixModel.getRight();
                for (int i = 0; i < testCase.lexContext.size(); ++i) {
                    cache.updateCache(testCase.lexContext, i);
                }
            }
        }

        if (ModelConfig.USE_EXCODE) {
            testCase.excodeContext.addAll(testCase.realExcodeParamIndices);
            if (ModelConfig.USE_DYNAMIC) {
                singleParamExcodeManager.modelRunner.getModel().learn(testCase.excodeContext);
            }
            if (ModelConfig.USE_CACHE) {
                ModelRunner modelRunner = singleParamExcodeManager.modelRunner;
                MixModel mixModel = (MixModel) modelRunner.getModel();
                CacheModel cache = (CacheModel) mixModel.getRight();
                for (int i = 0; i < testCase.excodeContext.size(); ++i) {
                    cache.updateCache(testCase.excodeContext, i);
                }
            }
        }
    }

    public String normalizeType(String extractType) {
        if (extractType.equals("byte")) return null;
        if (extractType.equals("char")) return null;
        if (extractType.equals("short")) return null;
        if (extractType.equals("int")) return null;
        if (extractType.equals("long")) return null;
        if (extractType.equals("float")) return null;
        if (extractType.equals("double")) return null;
        if (extractType.startsWith("byte[]") || extractType.equals("Array_byte")) return null;
        if (extractType.startsWith("char[]") || extractType.equals("Array_char")) return null;
        if (extractType.startsWith("short[]") || extractType.equals("Array_short")) return null;
        if (extractType.startsWith("int[]") || extractType.equals("Array_int")) return null;
        if (extractType.startsWith("long[]") || extractType.equals("Array_long")) return null;
        if (extractType.startsWith("float[]") || extractType.equals("Array_float")) return null;
        if (extractType.startsWith("double[]") || extractType.equals("Array_double")) return null;
        return getClassFromString(extractType);
    }

    public String getTypeFromExcodeSequence(String excodeSequence) {
        if (excodeSequence.startsWith("VAR")) {
            for (int i = 0; i < excodeSequence.length(); ++i) {
                if (excodeSequence.charAt(i) == ' ') {
                    if (excodeSequence.startsWith("M_ACCESS", i+1) || excodeSequence.startsWith("F_ACCESS", i+1)) {
                        return excodeSequence.substring(4,i-1);
                    }
                    else return null;
                }
            }
        } else if (excodeSequence.startsWith("M_ACCESS") || excodeSequence.startsWith("F_ACCESS")) {
            StringBuilder excodeToken = new StringBuilder();
            int openParen = 0;
            for (int i = 0; i < excodeSequence.length(); ++i) {
                if (excodeSequence.charAt(i) == ' ') {
                    if (openParen > 0) {
                        excodeToken.append(excodeSequence.charAt(i));
                    } else {
                        return getTypeFromExcodeToken(excodeToken.toString());
                    }
                } else {
                    if (excodeSequence.charAt(i) == '(') openParen += 1;
                    else if (excodeSequence.charAt(i) == ')') openParen -= 1;
                    excodeToken.append(excodeSequence.charAt(i));
                }
            }
        }
        return null;
    }

    private String getTypeFromExcodeToken(String excodeToken) {
        if (excodeToken.startsWith("F_ACCESS")) {
            for (int i = excodeToken.length() - 1; i >= 0; --i) {
                if (excodeToken.charAt(i) == ',') {
                    return excodeToken.substring(9, i);
                }
            }
        } else if (excodeToken.startsWith("M_ACCESS")) {
            int cntComma = 0;
            for (int i = excodeToken.length() - 1; i >= 0; --i) {
                if (excodeToken.charAt(i) == ',') cntComma += 1;
                if (cntComma == 2) {
                    return excodeToken.substring(9, i);
                }
            }
        }
        return null;
    }

    public ArrayList<ScoreInfo> getTopCands() {
        ArrayList<ScoreInfo> topCands = new ArrayList<>();
        for (int i = 0; i < testCase.scores.size(); ++i) {
            // sometimes test case generated ")" as candidate
            if (testCase.scores.get(i).candidate.lexical.equals(")")) continue;
            if (i > 0 && testCase.scores.get(i).candidate.lexical.equals(testCase.scores.get(i-1).candidate.lexical)) continue;
            topCands.add(testCase.scores.get(i));
            if (topCands.size() == TestConfig.TOP_K) break;
        }
        return topCands;
    }

    public void resolveCorrectLexicalParam() {
        if (SingleParamTester.isNoParamTest(testCase.requestString)) return;
        for (List<String> candsByExcode: testCase.request.next_lex) {
            for (String lexCand : candsByExcode) {
                Candidate candidate = new Candidate("", lexCand);
                if (CandidateMatcher.matches(candidate, testCase.normalizedExpectedLex)) {
                    return;
                }
            }
        }
        HashSet<Candidate> hs = new HashSet<>();
        hs.add(new Candidate(testCase.normalizedExpectedLex));
        // dont know weather normalizedExpectedLex is public static or not
        testCase.lexicalCands.put("CORRECT_LEXICAL_PARAM", hs);
    }
}
