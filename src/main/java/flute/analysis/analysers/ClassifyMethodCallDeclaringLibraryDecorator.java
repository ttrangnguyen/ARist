package flute.analysis.analysers;

import flute.analysis.structure.DataFrame;
import flute.config.Config;
import flute.jdtparser.ProjectParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

import java.io.File;

public class ClassifyMethodCallDeclaringLibraryDecorator extends AnalyzeDecorator {
    public ClassifyMethodCallDeclaringLibraryDecorator(JavaAnalyser analyser) {
        super(analyser);
    }

    @Override
    DataFrame analyseFile(File file) {
        DataFrame dataFrameOfFile = super.analyseFile(file);

        long startTime = System.nanoTime();

        CompilationUnit cu = projectParser.createCU(file);
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodInvocation node) {
                IMethodBinding methodBinding = node.resolveMethodBinding();
                if (methodBinding == null) {
                    stringCounter.add("lib");
                } else if (methodBinding.getDeclaringClass().getPackage().getName().startsWith("java.")) {
                    stringCounter.add("jre");
                } else {
                    stringCounter.add("src");
                }
                return true;
            }
        });

        analysingTime += System.nanoTime() - startTime;

        return dataFrameOfFile;
    }

    @Override
    void setupParsers(File project, boolean parseStatically) {
        super.setupParsers(project, parseStatically);
        projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH, Config.ENCODE_SOURCE,
                new String[]{}, Config.JDT_LEVEL, Config.JAVA_VERSION);
    }
}