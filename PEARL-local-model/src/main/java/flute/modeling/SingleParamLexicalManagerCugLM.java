package flute.modeling;

import flute.config.ModelConfig;
import flute.config.ProjectConfig;
import flute.testing.TestFilesManager;
import slp.core.counting.giga.GigaCounter;
import slp.core.lexing.Lexer;
import slp.core.lexing.code.JavaLexer;
import slp.core.lexing.runners.LexerRunner;
import slp.core.modeling.Model;
import slp.core.modeling.dynamic.CacheModel;
import slp.core.modeling.dynamic.NestedModel;
import slp.core.modeling.mix.MixModel;
import slp.core.modeling.ngram.*;
import slp.core.modeling.runners.ModelRunner;
import slp.core.translating.Vocabulary;
import slp.core.translating.VocabularyRunner;

import java.io.*;

public class SingleParamLexicalManagerCugLM extends ModelManager{

    public SingleParamLexicalManagerCugLM(Integer mode) {
        super(mode);
    }

    @Override
    public void initModelPath() {
        modelPath = ModelConfig.lexicalModelPath;
    }

    @Override
    public void createModel() {
        Lexer lexer = new JavaLexer();
        LexerRunner lexerRunner = new LexerRunner(lexer, false);
        lexerRunner.setSentenceMarkers(true);

        // Be careful
        lexerRunner.setExtension("java");

        Vocabulary vocabulary = new Vocabulary();
        VocabularyRunner.cutOff(ModelConfig.LEXICAL_VOCABULARY_CUTOFF);
        vocabulary = VocabularyRunner.build(lexerRunner, new File(ProjectConfig.cugLMAllProjectsPath));
        Model model = new JMModel(ModelConfig.NGRAM, new GigaCounter());
        System.out.println("Creating model");
        if (ModelConfig.USE_NESTED) {
            model = new NestedModel(model, lexerRunner, vocabulary, new File(ProjectConfig.cugLMTestProjectsPath));
            if (ModelConfig.USE_CACHE) model = MixModel.standard(model, new CacheModel());
            if (ModelConfig.USE_DYNAMIC) model.setDynamic(true);
        }
        System.out.println("Creating model done!");
        modelRunner = new ModelRunner(model, lexerRunner, vocabulary);

        // manually forget/relearn
        modelRunner.setSelfTesting(false);
    }

    @Override
    public void train() {
        File[] CugLMPaths = new File(ProjectConfig.cugLMAllProjectsPath).listFiles(File::isDirectory);
        assert CugLMPaths != null;
        System.out.println("Begin training");
        for (File projectPath : CugLMPaths) {
            if (TestFilesManager.testProjects.contains(projectPath)) continue;
            long projectStartTimeTrain = System.nanoTime();
            modelRunner.learnDirectory(new File(projectPath.getAbsolutePath()));
            System.out.println(projectPath.getName() + " Train time: " + (1.0*System.nanoTime()-projectStartTimeTrain)/1000000000);
            System.out.println("Vocab size: " + modelRunner.getVocabulary().getCounts().size());
        }
    }

    @Override
    public void saveModel() throws IOException {
        FileOutputStream f = new FileOutputStream(ModelConfig.cugLMModelPath);
        ObjectOutputStream o = new ObjectOutputStream(f);
        o.writeObject(modelRunner);
        o.close();
        f.close();
    }
}
