package flute;

import flute.config.Config;
import flute.config.ModelConfig;
import flute.config.ProjectConfig;
import flute.config.TestConfig;
import flute.evaluating.SequenceEvaluator;
import flute.evaluating.SequenceEvaluatorCuglm;
import flute.modeling.ModelManager;
import flute.modeling.SingleParamLexicalManager;
import flute.modeling.SingleParamLexicalManagerCugLM;
import flute.testing.CugLMMaintenanceTester;
import flute.testing.CugLMTester;
import flute.testing.SingleParamTester;
import flute.testing.TestFilesManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Main {
    private static final String TRAIN = "--train";
    private static final String TEST = "--test";
    private static final String EVALUATE = "--evaluate";

    private static final String BEAM_SEARCH = "--beam-search";
    private static final String DYNAMIC = "--dynamic";
    private static final String STATIC = "--static";
    private static final String MAINTENANCE = "--maintenance";

    private static final String PROJECT = "--project";
    private static final String PARC = "--parc";
    private static final String CUGLM = "--cuglm";

    private static final String TOKENIZED = "--tokenized";
    private static final String PREDICT_TYPE = "--predict-type";

    private static final String FOLD = "--fold";

    private static final String SOLUTION_NAME = "--solution-name";
    private static final String EVALUATE_PARC = "--eval-parc";

    private static final String CUG_DYNAMIC_BEAM_PROJ_ID = "--cuglm-id";

    private static final String LAST_PARC_FOLD = "--last-parc-fold";

    private static final String NGRAM_GLOBAL = "--ngram-global";

    private static String[] arguments;

    private static boolean isSet(String arg) {
        for (String a : arguments) {
            if (a.matches(arg)) return true;
        }
        return false;
    }

    private static String getArg(String arg) {
        for (int i = 1; i < arguments.length; i++) {
            String a = arguments[i];
            if (a.matches(arg)) {
                if (i < arguments.length - 1) return arguments[i + 1];
                return "";
            }
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        arguments = args;
        if (isSet(TRAIN)) {
            Config.mode = Config.Mode.TRAIN;
        } else if (isSet(TEST)) {
            Config.mode = Config.Mode.TEST;
        } else if (isSet(EVALUATE)) {
            Config.mode = Config.Mode.EVALUATE;
        } else {
            System.out.println("Please specify mode: --train, --test or --evaluate");
            return;
        }

        if (isSet(CUGLM)) ProjectConfig.CUGLM = true;
        else if (isSet(PARC)) ProjectConfig.CUGLM = false;
        else {
            System.out.println("Either specify --cuglm or --parc");
            return;
        }

        if (!isSet(CUGLM)) {
            if (!isSet(EVALUATE)) {
                if (!isSet(PROJECT)) {
                    System.out.println("Please specify --project as netbeans or eclipse");
                    return;
                } else {
                    ProjectConfig.project = getArg(PROJECT);
                }
                if (!isSet(FOLD)) {
                    System.out.println("Please specify --fold (a number from 0 to 9)");
                    return;
                } else {
                    if (isSet(FOLD)) TestConfig.fold = getArg(FOLD);
                }
            }
        }

        if (isSet(MAINTENANCE) && isSet(CUGLM)) {
            ModelConfig.USE_MAINTENANCE = true;
            ModelConfig.USE_NESTED = true;
        } else if (isSet(DYNAMIC)) ModelConfig.USE_NESTED = true;
        else if (isSet(STATIC)) ModelConfig.USE_NESTED = false;
        else {
            System.out.println("Either specify --dynamic or --static or --maintenance with --cuglm");
            return;
        }

        if (isSet(NGRAM_GLOBAL)) ModelConfig.USE_NGRAM_GLOBAL = true;
        if (isSet(BEAM_SEARCH)) ModelConfig.USE_BEAM_SEARCH = true;

        if (isSet(TOKENIZED)) {
            if (getArg(TOKENIZED).equals("fulltoken")) ModelConfig.tokenizedType = ModelConfig.TokenizedType.FULL_TOKEN;
            else ModelConfig.tokenizedType = ModelConfig.TokenizedType.SUB_TOKEN;
        } else {
            System.out.println("Either specify --tokenized as fulltoken or subtoken");
            return;
        }

        if (isSet(LAST_PARC_FOLD)) {
            TestConfig.lastParcFold = Integer.parseInt(getArg(LAST_PARC_FOLD));
        }

        if (Config.mode == Config.Mode.TRAIN){
            Config.init();
            long startTimeTrain = System.nanoTime();
            if (ProjectConfig.CUGLM) {
                SingleParamLexicalManagerCugLM singleParamLexicalManagerCugLM = new SingleParamLexicalManagerCugLM(ModelManager.CREATING);
                singleParamLexicalManagerCugLM.train();
                singleParamLexicalManagerCugLM.saveModel();
            } else {
                SingleParamLexicalManager singleParamLexicalManager = new SingleParamLexicalManager(ModelManager.CREATING);
                singleParamLexicalManager.train();
                singleParamLexicalManager.saveModel();
            }
            System.out.println("Train time: " + (1.0*System.nanoTime()-startTimeTrain)/1000000000);
        } else if (Config.mode == Config.Mode.TEST){
            if (isSet(PREDICT_TYPE)) {
                if (getArg(PREDICT_TYPE).equals("sequence")) TestConfig.predictType = TestConfig.PredictType.SEQUENCE;
                else TestConfig.predictType = TestConfig.PredictType.FIRST_TOKEN;
            } else {
                System.out.println("Specify --predict-type as firsttoken or sequence");
                return;
            }
            if (ProjectConfig.CUGLM) {
                File[] CugLMPaths = new File(ProjectConfig.cugLMTestProjectsPath).listFiles(File::isDirectory);
                System.out.println("Testing begin");
                assert CugLMPaths != null;
                int cnt = 0;
                for (File projectPath : CugLMPaths) {
                    ++cnt;
                    if (isSet(BEAM_SEARCH) && isSet(DYNAMIC) && !isSet(MAINTENANCE)) {
                        if (cnt != Integer.parseInt(getArg(CUG_DYNAMIC_BEAM_PROJ_ID))) continue;
                    }
                    ProjectConfig.project = projectPath.getName();
                    Config.init();
                    System.out.println("Testing " + projectPath.getName());
                    if (!isSet(MAINTENANCE)) {
                        CugLMTester tester = new CugLMTester();
                        tester.run();
                    } else {
                        CugLMMaintenanceTester tester = new CugLMMaintenanceTester();
                        tester.run();
                    }
                }
                System.out.println("Testing done");
            } else {
                Config.init();
                SingleParamTester tester = new SingleParamTester();
                tester.run();
            }
        } else if (Config.mode == Config.Mode.EVALUATE){
            if (isSet(PREDICT_TYPE)) {
                if (getArg(PREDICT_TYPE).equals("sequence")) TestConfig.predictType = TestConfig.PredictType.SEQUENCE;
                else TestConfig.predictType = TestConfig.PredictType.FIRST_TOKEN;
            } else {
                System.out.println("Specify --predict-type as firsttoken or sequence");
                return;
            }
            if (!isSet(SOLUTION_NAME)) {
                System.out.println("Please set --solution-name as flute");
                return;
            }
            if (ProjectConfig.CUGLM) {
                ArrayList<String> solutions = new ArrayList<>();
                solutions.add(getArg(SOLUTION_NAME));
                for (String solution : solutions) {
                    SequenceEvaluatorCuglm fluteEvaluator = new SequenceEvaluatorCuglm();
                    fluteEvaluator.evaluate(solution);
                }
            } else {
                ArrayList<String> projects = new ArrayList<>();
                projects.add("netbeans");
                projects.add("eclipse");
                ArrayList<String> solutions = new ArrayList<>();
                solutions.add(getArg(SOLUTION_NAME));
                TestConfig.EVALUATE_PARC = isSet(EVALUATE_PARC);
                for (String project : projects) {
                    for (String solution : solutions) {
                        SequenceEvaluator fluteEvaluator = new SequenceEvaluator();
                        fluteEvaluator.evaluate(project, solution);
                    }
                }
            }
        }
    }
}
