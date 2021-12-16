package flute.analysis.analysers;

import flute.analysis.structure.DataFrame;
import flute.analysis.structure.StringCounter;
import flute.config.Config;
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

    public static void main(String[] args) {
        JavaAnalyser javaAnalyser = new JavaAnalyser();
        javaAnalyser = new CollectMethodCallSignatureDecorator(javaAnalyser);

        javaAnalyser.analyseProjects(new File(Config.REPO_DIR + "oneproj/"), false);

        javaAnalyser.printAnalysingTime();
        DataFrame.Variable variable = null;
        StringCounter stringCounter = null;

        stringCounter = javaAnalyser.getCollection(CollectMethodCallSignatureDecorator.class);
        System.out.println(stringCounter.describe(100));
        variable = new DataFrame.Variable();
        for (String argUsage: stringCounter.getDistinctStrings()) {
            variable.insert(stringCounter.getCount(argUsage));
        }
        System.out.println("Statistics on usage frequency of method call:");
        System.out.println(DataFrame.describe(variable));
        System.out.println("Frequency distribution of occurrence of method call:");
        for (int i = 1; i <= 9; ++i) {
            System.out.println(String.format("\t%5d times: %5.2f%%", i, variable.getProportionOfValue(i, true)));
        }
        for (int i = 1; i <= 9; ++i) {
            System.out.println(String.format("\t%4dx times: %5.2f%%", i, variable.getProportionOfRange(i*10, (i+1)*10-1, true)));
        }
        System.out.println(String.format("\t>=%3d times: %5.2f%%", 100, variable.getProportionOfRange(100, variable.getMax(), true)));
    }
}