package flute.preprocessing;

import flute.config.Config;
import flute.utils.file_processing.FileProcessor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MethodExtractor extends Preprocessor {
    private int jdtLevel = 13;
    private String javaVersion = "13";
    private static ASTParser parser;

    public MethodExtractor() {
        parser = ASTParser.newParser(jdtLevel); //choose source code analyzing strategy

        parser.setResolveBindings(true); // turn on binding strategy
        parser.setKind(ASTParser.K_COMPILATION_UNIT);// the source code is a file .java
        parser.setBindingsRecovery(false);
        parser.setStatementsRecovery(true);
        Hashtable<String, String> options = JavaCore.getOptions();

        JavaCore.setComplianceOptions(javaVersion, options);

        parser.setCompilerOptions(options);
        parser.setEnvironment(new String[]{}, new String[]{}, new String[]{}, true);
    }

    @Override
    protected void exportCode(String sourceCode, File outputFolder, File project, File file) {
        String projectPath = project.getAbsolutePath();
        String relativeFilePath = file.getAbsolutePath();
        relativeFilePath = relativeFilePath.substring(projectPath.length() - project.getName().length());

        CompilationUnit cu = null;
        try {
            cu = createCU(file.getName(), sourceCode);
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace();
            return;
        }
        String finalRelativeFilePath = relativeFilePath;
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                StringBuilder simplifiedClass = new StringBuilder();
                simplifiedClass.append(node.getName());
                simplifiedClass.append(" {");

                Set<String> fieldDeclarationSet = new HashSet<>();
                for (FieldDeclaration fieldDeclaration: node.getFields()) {
                    StringBuilder simplifiedField = new StringBuilder();
                    if (Modifier.isStatic(fieldDeclaration.getModifiers())) {
                        simplifiedField.append("static ");

                        //TODO: Handle static field access
                        continue;
                    }
                    simplifiedField.append(simplifyTypeName(fieldDeclaration.getType()));
                    simplifiedField.append(' ');
                    fieldDeclaration.fragments().forEach(fragment -> {
                        VariableDeclarationFragment variable = (VariableDeclarationFragment) fragment;
                        String variableName = variable.getName().toString();
                        //Normalize variable name
                        variableName = variableName.replaceFirst("\\d+$", "");
                        simplifiedField.append(variableName);
                        simplifiedField.append(", ");
                    });
                    simplifiedField.delete(simplifiedField.length() - 2, simplifiedField.length());
                    simplifiedField.append(';');

                    fieldDeclarationSet.add(simplifiedField.toString());
                }
                for (String fieldDeclaration: fieldDeclarationSet) {
                    simplifiedClass.append(fieldDeclaration);
                }

                HashMap<String, List<String>> methodMapByName = new HashMap<>();
                HashMap<String, List<String>> nonVoidMapByReturnType = new HashMap<>();
                HashMap<String, List<String>> paramTypesMapByMethod = new HashMap<>();
                for (MethodDeclaration methodDeclaration: node.getMethods()) {
                    StringBuilder methodSignature = new StringBuilder();
                    if (Modifier.isStatic(methodDeclaration.getModifiers())) {
                        methodSignature.append("static ");
                    }
                    String returnType = simplifyTypeName(methodDeclaration.getReturnType2());
                    String methodName = methodDeclaration.getName().toString();

                    methodSignature.append(returnType);
                    methodSignature.append(' ');
                    methodSignature.append(methodName);
                    methodSignature.append('(');
                    List<String> paramTypes = new ArrayList<>();
                    methodDeclaration.parameters().forEach(param -> {
                        SingleVariableDeclaration variable = (SingleVariableDeclaration) param;
                        String paramType = simplifyTypeName(variable.getType());
                        methodSignature.append(paramType);
                        methodSignature.append(' ');
                        methodSignature.append(variable.getName());
                        methodSignature.append(", ");

                        paramTypes.add(paramType);
                    });
                    if (paramTypes.size() > 0) {
                        methodSignature.delete(methodSignature.length() - 2, methodSignature.length());
                    }
                    methodSignature.append(");");

                    if (returnType.compareTo("null") != 0) {
                        if (methodMapByName.get(methodName) == null) {
                            methodMapByName.put(methodName, new ArrayList<>());
                        }
                        methodMapByName.get(methodName).add(methodSignature.toString());

                        if (returnType.compareTo("void") != 0) {
                            if (nonVoidMapByReturnType.get(returnType) == null) {
                                nonVoidMapByReturnType.put(returnType, new ArrayList<>());
                            }
                            nonVoidMapByReturnType.get(returnType).add(methodSignature.toString());
                        }

                        paramTypesMapByMethod.put(methodSignature.toString(), paramTypes);
                    }
                }

                for (MethodDeclaration methodDeclaration: node.getMethods()) {
                    StringBuilder simplifiedCode = new StringBuilder();
                    simplifiedCode.append(simplifiedClass);

                    Set<String> methodInvocSet = new HashSet<>();
                    methodDeclaration.accept(new ASTVisitor() {
                        @Override
                        public boolean visit(MethodInvocation node) {
                            String methodInvoc = node.getName().toString();
                            methodInvocSet.add(methodInvoc);
                            return true;
                        }
                    });

                    Set<String> methodSet = new HashSet();
                    Set<String> paramTypeSet = new HashSet<>();
                    for (String methodInvoc: methodInvocSet) {
                        List<String> methods = methodMapByName.getOrDefault(methodInvoc, null);
                        if (methods != null) {
                            methodSet.addAll(methods);

                            for (String method: methods) {
                                List<String> paramTypes = paramTypesMapByMethod.getOrDefault(method, null);
                                if (paramTypes != null) {
                                    paramTypeSet.addAll(paramTypes);
                                }
                            }
                        }
                    }

                    for (String paramType: paramTypeSet) {
                        List<String> methods = nonVoidMapByReturnType.getOrDefault(paramType, null);
                        if (methods != null) {
                            methodSet.addAll(methods);
                        }
                    }

                    for (String method: methodSet) {
                        simplifiedCode.append(method);
                    }

                    String method = methodDeclaration.toString();
                    method = RemoveNewLineDecorator.preprocess(method);
                    method = RemoveIndentDecorator.preprocess(method);
                    method = EmptyStringLiteralDecorator.preprocess(method);

                    simplifiedCode.append(method);
                    simplifiedCode.append('}');

                    String outputFileName = finalRelativeFilePath.replace("\\", "_");
                    outputFileName = outputFileName.replace(".java", "");
                    outputFileName = outputFileName + "_" + methodDeclaration.getName();
                    outputFileName = outputFileName + "_" + methodDeclaration.getStartPosition();
                    outputFileName = outputFileName + ".txt";

                    String outputFilePath = outputFolder.getAbsolutePath() + "/" + outputFileName;
                    try {
                        FileProcessor.write(simplifiedCode.toString(), outputFilePath);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                    //System.out.println(simplifiedCode);
                }
                return true;
            }
        });
    }

    public static CompilationUnit createCU(String fileName, String fileData) throws IllegalArgumentException {
        parser.setUnitName(fileName);
        parser.setSource(fileData.toCharArray());
        CompilationUnit cu = (CompilationUnit) parser.createAST(new NullProgressMonitor());
        return cu;
    }

    public static String simplifyTypeName(Object typeName) {
        if (typeName == null) return "null";
        return typeName.toString().substring(typeName.toString().lastIndexOf('.') + 1);
    }

    public static void main(String[] args) {
        Preprocessor preprocessor = new MethodExtractor();
        preprocessor = new RemoveCommentDecorator(preprocessor);
        preprocessor = new RemovePackageDecorator(preprocessor);
        preprocessor = new RemoveImportDecorator(preprocessor);
        preprocessor = new RemoveNewLineDecorator(preprocessor);
        preprocessor = new RemoveIndentDecorator(preprocessor);

//        preprocessor.preprocessProjects(new File(Config.REPO_DIR + "sampleproj/src/xyz"),
//                                        new File(Config.LOG_DIR + "trial/"));
        preprocessor.preprocessProjects(new File("D:\\Java\\Research\\java-data\\"),
                new File(Config.LOG_DIR + "dataset-gpt-method/"));
    }
}
