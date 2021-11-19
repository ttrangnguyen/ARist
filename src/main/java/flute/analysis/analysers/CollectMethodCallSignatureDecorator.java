package flute.analysis.analysers;

import flute.analysis.structure.DataFrame;
import org.eclipse.jdt.core.dom.*;

import java.io.File;

public class CollectMethodCallSignatureDecorator extends AnalyzeDecorator {
    public CollectMethodCallSignatureDecorator(JavaAnalyser analyser) {
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
                    StringBuilder sb = new StringBuilder();
                    //sb.append(currentProject);
                    //sb.append(':');
                    sb.append(methodBinding.getName());
                    sb.append('(');
                    ITypeBinding[] paramTypes = methodBinding.getParameterTypes();
                    for (int i = 0; i < paramTypes.length; ++i) {
                        String typeSimpleName = paramTypes[i].getName();
                        if (typeSimpleName.indexOf('<') >= 0) {
                            typeSimpleName = typeSimpleName.substring(0, typeSimpleName.indexOf('<'));
                        }
                        sb.append(typeSimpleName);
                        if (i < paramTypes.length - 1) sb.append(',');
                    }
                    sb.append(')');
                    stringCounter.add(sb.toString());
                }
                return true;
            }
        });

        analysingTime += System.nanoTime() - startTime;

        return dataFrameOfFile;
    }
}