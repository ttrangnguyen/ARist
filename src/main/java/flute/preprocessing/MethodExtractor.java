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
    class TypeDeclarationData {
        TypeDeclaration node = null;
        String context;
        HashMap<String, List<String>> methodMapByName;
        HashMap<String, List<String>> nonVoidMapByReturnType;
        HashMap<String, List<String>> paramTypesMapByMethod;
    }

    private int jdtLevel = 13;
    private String javaVersion = "13";
    private ASTParser parser;
    private TypeDeclarationData lastTypeDeclaration = new TypeDeclarationData();

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

    private CompilationUnit createCU(String fileName, String fileData) throws IllegalArgumentException {
        parser.setUnitName(fileName);
        parser.setSource(fileData.toCharArray());
        CompilationUnit cu = (CompilationUnit) parser.createAST(new NullProgressMonitor());
        return cu;
    }

    public static String getTypeDeclarationContext(TypeDeclaration node) {
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
        return simplifiedClass.toString();
    }

    public TypeDeclarationData getTypeDeclarationData(TypeDeclaration node) {
        if (lastTypeDeclaration.node != node) {
            TypeDeclarationData data = new TypeDeclarationData();
            data.node = node;
            data.context = getTypeDeclarationContext(node);
            data.methodMapByName = new HashMap<>();
            data.nonVoidMapByReturnType = new HashMap<>();
            data.paramTypesMapByMethod = new HashMap<>();

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
                    if (data.methodMapByName.get(methodName) == null) {
                        data.methodMapByName.put(methodName, new ArrayList<>());
                    }
                    data.methodMapByName.get(methodName).add(methodSignature.toString());

                    if (returnType.compareTo("void") != 0) {
                        if (data.nonVoidMapByReturnType.get(returnType) == null) {
                            data.nonVoidMapByReturnType.put(returnType, new ArrayList<>());
                        }
                        data.nonVoidMapByReturnType.get(returnType).add(methodSignature.toString());
                    }

                    data.paramTypesMapByMethod.put(methodSignature.toString(), paramTypes);
                }
            }
            lastTypeDeclaration = data;
        }
        return lastTypeDeclaration;
    }

    private String getTypeDeclarationContext(TypeDeclaration node, Set<String> methodInvocSet) {
        TypeDeclarationData data = getTypeDeclarationData(node);
        StringBuilder simplifiedCode = new StringBuilder();
        simplifiedCode.append(data.context);

        Set<String> methodSet = new HashSet();
        Set<String> paramTypeSet = new HashSet<>();
        for (String methodInvoc: methodInvocSet) {
            List<String> methods = data.methodMapByName.getOrDefault(methodInvoc, null);
            if (methods != null) {
                methodSet.addAll(methods);

                for (String method: methods) {
                    List<String> paramTypes = data.paramTypesMapByMethod.getOrDefault(method, null);
                    if (paramTypes != null) {
                        paramTypeSet.addAll(paramTypes);
                    }
                }
            }
        }

        for (String paramType: paramTypeSet) {
            List<String> methods = data.nonVoidMapByReturnType.getOrDefault(paramType, null);
            if (methods != null) {
                methodSet.addAll(methods);
            }
        }

        for (String method: methodSet) {
            simplifiedCode.append(method);
        }

        return simplifiedCode.toString();
    }

    public String getMethodDeclarationContext(MethodDeclaration methodDeclaration, Set<String> methodInvocSet) {
        ASTNode node = methodDeclaration;
        while (!(node == null || node instanceof TypeDeclaration)) node = node.getParent();
        if (node == null) return null;
        return getTypeDeclarationContext((TypeDeclaration) node, methodInvocSet);
    }

    public String getInitializerContext(Initializer initializer, Set<String> methodInvocSet) {
        ASTNode node = initializer;
        while (!(node == null || node instanceof TypeDeclaration)) node = node.getParent();
        if (node == null) return null;
        return getTypeDeclarationContext((TypeDeclaration) node, methodInvocSet);
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
                if (node.isInterface()) return true;
                for (MethodDeclaration methodDeclaration: node.getMethods()) {
                    Set<String> methodInvocSet = new HashSet<>();
                    methodDeclaration.accept(new ASTVisitor() {
                        @Override
                        public boolean visit(MethodInvocation node) {
                            String methodInvoc = node.getName().toString();
                            methodInvocSet.add(methodInvoc);
                            return true;
                        }
                    });

                    StringBuilder simplifiedCode = new StringBuilder(getMethodDeclarationContext(methodDeclaration, methodInvocSet));
                    simplifiedCode.append(preprocessCodeBlock(methodDeclaration.toString()));
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

    public static String simplifyTypeName(Object typeName) {
        if (typeName == null) return "null";
        return typeName.toString().substring(typeName.toString().lastIndexOf('.') + 1);
    }

    public static String preprocessCodeBlock(String codeBlock) {
        codeBlock = RemoveNewLineDecorator.preprocess(codeBlock);
        codeBlock = RemoveRedundantSpaceDecorator.preprocess(codeBlock);
        codeBlock = EmptyStringLiteralDecorator.preprocess(codeBlock);
        return codeBlock;
    }

    public static void main(String[] args) {
        Preprocessor preprocessor = new MethodExtractor();
        preprocessor = new RemoveCommentDecorator(preprocessor);
        preprocessor = new RemovePackageDecorator(preprocessor);
        preprocessor = new RemoveImportDecorator(preprocessor);
        preprocessor = new RemoveNewLineDecorator(preprocessor);
        preprocessor = new RemoveIndentDecorator(preprocessor);

        preprocessor.preprocessProjects(new File(Config.REPO_DIR + "sampleproj/"),
                                        new File(Config.LOG_DIR + "dataset-sample-method/"));
//        preprocessor.preprocessProjects(new File("D:\\Java\\Research\\java-data\\"),
//                new File(Config.LOG_DIR + "dataset-gpt-method/"));
    }
}
