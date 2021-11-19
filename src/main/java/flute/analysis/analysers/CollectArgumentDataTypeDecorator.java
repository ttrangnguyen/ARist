package flute.analysis.analysers;

import flute.analysis.structure.DataFrame;
import org.eclipse.jdt.core.dom.*;

import java.io.File;

public class CollectArgumentDataTypeDecorator extends AnalyzeDecorator {
    public CollectArgumentDataTypeDecorator(JavaAnalyser analyser) {
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
                if (methodBinding != null) {
                    ITypeBinding[] paramTypes = methodBinding.getParameterTypes();
                    for (ITypeBinding paramType: paramTypes) {
                        String paramTypeSimpleName = paramType.getName();
                        if (paramTypeSimpleName.indexOf('<') >= 0) {
                            paramTypeSimpleName = paramTypeSimpleName.substring(0, paramTypeSimpleName.indexOf('<'));
                        }
                        stringCounter.add(paramTypeSimpleName);
                    }
                }
                return true;
            }
        });

        analysingTime += System.nanoTime() - startTime;

        return dataFrameOfFile;
    }
}