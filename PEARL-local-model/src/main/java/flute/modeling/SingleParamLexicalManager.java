package flute.modeling;

import flute.config.ModelConfig;
import flute.config.ProjectConfig;
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

public class SingleParamLexicalManager extends ModelManager{

    public SingleParamLexicalManager(Integer mode) {
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
        lexerRunner.setExtension("java");
        Vocabulary vocabulary;
        if (ModelConfig.LEXICAL_VOCABULARY_CUTOFF > 0) {
            VocabularyRunner.cutOff(ModelConfig.LEXICAL_VOCABULARY_CUTOFF);
            vocabulary = VocabularyRunner.build(lexerRunner, new File(ProjectConfig.projectGeneratedDataRoot));
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
}
