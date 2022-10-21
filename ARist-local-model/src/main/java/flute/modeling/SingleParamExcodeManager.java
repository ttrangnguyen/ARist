package flute.modeling;

import flute.config.Config;
import flute.config.ModelConfig;
import flute.config.ProjectConfig;
import flute.lexing.ExcodeLexer;
import slp.core.counting.giga.GigaCounter;
import slp.core.lexing.Lexer;
import slp.core.lexing.runners.LexerRunner;
import slp.core.modeling.Model;
import slp.core.modeling.dynamic.CacheModel;
import slp.core.modeling.dynamic.NestedModel;
import slp.core.modeling.mix.MixModel;
import slp.core.modeling.ngram.JMModel;
import slp.core.modeling.runners.ModelRunner;
import slp.core.translating.Vocabulary;
import slp.core.translating.VocabularyRunner;

import java.io.*;

public class SingleParamExcodeManager extends ModelManager {

    public SingleParamExcodeManager(int mode) {
        super(mode);
    }

    @Override
    public void initModelPath() {
        modelPath = ModelConfig.excodeModelPath;
    }

    @Override
    public void createModel() {
        Lexer lexer = new ExcodeLexer();
        LexerRunner lexerRunner = new LexerRunner(lexer, false);
        lexerRunner.setSentenceMarkers(true);
        lexerRunner.setExtension("jexcode");
        Vocabulary vocabulary;
        if (ProjectConfig.project.equals("netbeans") || ProjectConfig.project.equals("eclipse")) {
//            VocabularyRunner.cutOff(ModelConfig.EXCODE_VOCABULARY_CUTOFF);
            vocabulary = VocabularyRunner.read(new File(ModelConfig.vocabPath));
        } else {
            vocabulary = new Vocabulary();
        }
        Model model = new JMModel(ModelConfig.NGRAM, new GigaCounter());
        if (ModelConfig.USE_NESTED) {
            model = new NestedModel(model, lexerRunner, vocabulary, new File(ProjectConfig.projectGeneratedDataRoot));
            if (ModelConfig.USE_CACHE) model = MixModel.standard(model, new CacheModel());
            if (ModelConfig.USE_DYNAMIC) model.setDynamic(true);
        }
        modelRunner = new ModelRunner(model, lexerRunner, vocabulary);

        // manually forget/relearn
        modelRunner.setSelfTesting(false);
    }

    @Override
    public void train() {
        modelRunner.learnDirectory(new File(ProjectConfig.projectGeneratedDataRoot));
    }

    public static void main(String[] args) throws IOException {
//        // Config.init(args[0], args[1], args[2], args[3]);
        long start_time_train = System.nanoTime();

        SingleParamExcodeManager singleParamExcodeManager = new SingleParamExcodeManager(ModelManager.CREATING);
        singleParamExcodeManager.train();
        System.out.println("Train time: " + (1.0*System.nanoTime()-start_time_train)/1000000000);
        singleParamExcodeManager.saveModel();
    }
}
