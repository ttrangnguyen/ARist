package flute.analysis.analysers;

import flute.analysis.structure.DataFrame;
import flute.analysis.structure.StringCounter;
import flute.config.Config;
import flute.jdtparser.ProjectParser;
import org.eclipse.jdt.core.dom.*;

import java.io.File;

/**
 * Warning: Must be the last decorator to work!
 */
public class ClassifyArgumentIdentifierDeclaringLibraryDecorator extends AnalyzeDecorator {
    public ClassifyArgumentIdentifierDeclaringLibraryDecorator(JavaAnalyser analyser) {
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
                //System.out.println(node);
                node.arguments().forEach(argument -> {
                    ((ASTNode) argument).accept(new ASTVisitor() {
                        // TODO: check binding more accurately
                        @Override
                        public boolean visit(SimpleName node) {
                            boolean isAcceptable = false;
                            ASTNode parentNode = node.getParent();
                            // variable
                            if (parentNode instanceof MethodInvocation) isAcceptable = true;

                            // variable
                            if (parentNode instanceof InfixExpression) isAcceptable = true;

                            // array access
                            if (parentNode instanceof ArrayAccess) {
                                if (((ArrayAccess) parentNode).getArray().toString().compareTo(node.toString())==0) {
                                    isAcceptable = true;
                                }
                            }
                            if (isAcceptable) {
                                String declaringLibrary;
                                IBinding binding = node.resolveBinding();
                                if (binding == null) {
                                    declaringLibrary = "lib";
                                } else {
                                    declaringLibrary = "src";
                                }
                                //System.out.println(node);
                                //System.out.println(declaringLibrary);
                                stringCounter.add(declaringLibrary);
                            }
                            return false;
                        }

                        @Override
                        public boolean visit(MethodInvocation node) {
                            String declaringLibrary;
                            IMethodBinding methodBinding = node.resolveMethodBinding();
                            if (methodBinding == null) {
                                declaringLibrary = "lib";
                            } else if (methodBinding.getDeclaringClass().getPackage().getName().startsWith("java.")) {
                                declaringLibrary = "jre";
                            } else {
                                declaringLibrary = "src";
                            }
                            //System.out.println(node);
                            //System.out.println(declaringLibrary);
                            stringCounter.add(declaringLibrary);
                            return false;
                        }

                        @Override
                        public boolean visit(FieldAccess node) {
                            //System.out.println(node);
                            String declaringLibrary;
                            IVariableBinding variableBinding = node.resolveFieldBinding();
                            if (variableBinding == null) {
                                declaringLibrary = "lib";
                            } else {
                                if (variableBinding.getDeclaringClass() == null && node.getName().toString().compareTo("length") == 0) {
                                    declaringLibrary = "jre";
                                } else {
                                    if (variableBinding.getDeclaringClass().getPackage().getName().startsWith("java.")) {
                                        declaringLibrary = "jre";
                                    } else {
                                        declaringLibrary = "src";
                                    }
                                }
                            }
                            //System.out.println(node);
                            //System.out.println(declaringLibrary);
                            stringCounter.add(declaringLibrary);
                            return false;
                        }

                        // TODO: check binding more accurately
                        @Override
                        public boolean visit(QualifiedName node) {
                            String declaringLibrary;
                            IBinding binding = node.resolveBinding();
                            if (binding == null) {
                                declaringLibrary = "lib";
                            } else {
                                declaringLibrary = "src";
                            }
                            //System.out.println(node);
                            //System.out.println(declaringLibrary);
                            stringCounter.add(declaringLibrary);
                            return false;
                        }

                        @Override
                        public boolean visit(ClassInstanceCreation node) {
                            String declaringLibrary;
                            IMethodBinding methodBinding = node.resolveConstructorBinding();
                            if (methodBinding == null) {
                                declaringLibrary = "lib";
                            } else if (methodBinding.getDeclaringClass().getPackage().getName().startsWith("java.")) {
                                declaringLibrary = "jre";
                            } else {
                                declaringLibrary = "src";
                            }
                            //System.out.println(node);
                            //System.out.println(declaringLibrary);
                            stringCounter.add(declaringLibrary);
                            return false;
                        }

                        @Override
                        public boolean visit(ArrayCreation node) {
                            String declaringLibrary;
                            ITypeBinding typeBinding = node.getType().getElementType().resolveBinding();
                            if (typeBinding == null) {
                                declaringLibrary = "lib";
                            } else {
                                if (typeBinding.getPackage() == null) {
                                    declaringLibrary = "jre";
                                } else {
                                    if (typeBinding.getPackage().getName().startsWith("java.")) {
                                        declaringLibrary = "jre";
                                    } else {
                                        declaringLibrary = "src";
                                    }
                                }
                            }
                            //System.out.println(node);
                            //System.out.println(declaringLibrary);
                            stringCounter.add(declaringLibrary);
                            return false;
                        }

                        @Override
                        public boolean visit(LambdaExpression node) {
                            return false;
                        }

                        @Override
                        public boolean visit(MethodRef node) {
                            return false;
                        }
                    });
                });
                //System.out.println();
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

    public static void main(String[] args) {
        JavaAnalyser javaAnalyser = new JavaAnalyser();
        javaAnalyser = new ClassifyArgumentIdentifierDeclaringLibraryDecorator(javaAnalyser);

        javaAnalyser.analyseProjects(new File(Config.REPO_DIR + "oneproj/"), false);

        javaAnalyser.printAnalysingTime();
        StringCounter stringCounter = null;

        stringCounter = javaAnalyser.getCollection(ClassifyArgumentIdentifierDeclaringLibraryDecorator.class);
        System.out.println(stringCounter.describe());
    }
}