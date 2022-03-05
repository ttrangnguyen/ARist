package flute.analysis.analysers;

import flute.analysis.structure.DataFrame;

import java.io.File;

public abstract class AnalyzeDecorator extends JavaAnalyser {
    JavaAnalyser analyser;

    public AnalyzeDecorator(JavaAnalyser analyser) {
        this.analyser = analyser;
    }

    @Override
    DataFrame analyseFile(File file) {
        return analyser.analyseFile(file);
    }
}
