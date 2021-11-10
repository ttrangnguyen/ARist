package flute.preprocessing;

import flute.config.Config;
import flute.jdtparser.ProjectParser;
import flute.jdtparser.utils.DevUtils;
import flute.utils.file_processing.FileProcessor;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class MethodExtractor extends Preprocessor {
    class TypeDeclarationData {
        TypeDeclaration node = null;
        String context;
        Map<String, String> fieldMapByName;
        List<String> fieldList;
        Map<String, List<String>> methodMapByName;
    }

    private ProjectParser parser;
    private TypeDeclarationData lastTypeDeclaration = new TypeDeclarationData();

    public MethodExtractor() {
    }

    @Override
    public void preprocessProject(File project, File outputFolder, File fileList, boolean revert) {
        Config.autoConfigure(project.getName(), project.getAbsolutePath());
        parser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH, Config.ENCODE_SOURCE,
                Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);

        super.preprocessProject(project, outputFolder, fileList, revert);
    }

    public static String getTypeDeclarationContext(TypeDeclaration node) {
        StringBuilder simplifiedClass = new StringBuilder();
        simplifiedClass.append(simplifyTypeName(node.getName()));
        if (node.getSuperclassType() != null) {
            simplifiedClass.append(" extends ");
            simplifiedClass.append(simplifyTypeName(node.getSuperclassType()));
        }
        if (!node.superInterfaceTypes().isEmpty()) {
            simplifiedClass.append(" implements ");
            node.superInterfaceTypes().forEach(superInterface -> {
                simplifiedClass.append(simplifyTypeName(superInterface));
                simplifiedClass.append(",");
            });
            simplifiedClass.delete(simplifiedClass.length() - 1, simplifiedClass.length());
        }
        simplifiedClass.append(" {");

        return simplifiedClass.toString();
    }

    public TypeDeclarationData getTypeDeclarationData(TypeDeclaration node) {
        if (lastTypeDeclaration.node != node) {
            TypeDeclarationData data = new TypeDeclarationData();
            data.node = node;
            data.context = getTypeDeclarationContext(node);
            data.fieldMapByName = new HashMap<>();
            data.fieldList = new ArrayList<>();
            data.methodMapByName = new HashMap<>();

            for (FieldDeclaration fieldDeclaration: node.getFields()) {
                StringBuilder simplifiedField = new StringBuilder();
                if (Modifier.isStatic(fieldDeclaration.getModifiers())) {
                    simplifiedField.append("static ");
                }
                simplifiedField.append(simplifyTypeName(fieldDeclaration.getType()));
                simplifiedField.append(' ');
                fieldDeclaration.fragments().forEach(fragment -> {
                    VariableDeclarationFragment variable = (VariableDeclarationFragment) fragment;
                    String variableName = variable.getName().toString();
                    simplifiedField.append(variableName);
                    simplifiedField.append(",");
                });
                simplifiedField.delete(simplifiedField.length() - 1, simplifiedField.length());
                simplifiedField.append(';');

                fieldDeclaration.fragments().forEach(fragment -> {
                    VariableDeclarationFragment variable = (VariableDeclarationFragment) fragment;
                    String variableName = variable.getName().toString();
                    data.fieldMapByName.put(variableName, simplifiedField.toString());
                    data.fieldList.add(variableName);
                });
            }

            for (MethodDeclaration methodDeclaration: node.getMethods()) {
                String methodName = methodDeclaration.getName().toString();
                StringBuilder methodSignature = new StringBuilder();
                if (Modifier.isStatic(methodDeclaration.getModifiers())) {
                    methodSignature.append("static ");
                }
                String returnType = simplifyTypeName(methodDeclaration.getReturnType2());

                methodSignature.append(returnType);
                methodSignature.append(' ');
                methodSignature.append(methodName);
                methodSignature.append('(');
                List params = methodDeclaration.parameters();
                for (int i = 0; i < params.size(); ++i) {
                    SingleVariableDeclaration variable = (SingleVariableDeclaration) params.get(i);
//                    String paramType = simplifyTypeName(variable.getType());
//                    methodSignature.append(paramType);
//                    methodSignature.append(' ');
                    methodSignature.append(variable.getName());
                    if (i < params.size() - 1) {
                        methodSignature.append(",");
                    }
                }
                methodSignature.append(");");
                // Ignore methods which do not return a value
                if (!(returnType.compareTo("void") == 0)) {
                    if (data.methodMapByName.get(methodName) == null) {
                        data.methodMapByName.put(methodName, new ArrayList<>());
                    }
                    data.methodMapByName.get(methodName).add(methodSignature.toString());
                }
            }
            lastTypeDeclaration = data;
        }
        return lastTypeDeclaration;
    }

    private String getTypeDeclarationContext(BodyDeclaration bodyDeclaration, Set<String> methodInvocSet) {
        ASTNode node = bodyDeclaration;
        while (!(node == null || node instanceof TypeDeclaration)) node = node.getParent();
        if (node == null) return null;

        TypeDeclarationData data = getTypeDeclarationData((TypeDeclaration) node);
        StringBuilder simplifiedCode = new StringBuilder();
        simplifiedCode.append(data.context);

        Set<String> nameSet = new HashSet<>();
        bodyDeclaration.accept(new ASTVisitor() {
            @Override
            public boolean visit(SimpleName node) {
                nameSet.add(node.toString());
                return true;
            }
        });

        Set<String> fieldSet = new LinkedHashSet<>();
        for (String field: data.fieldList) {
            if (nameSet.contains(field)) {
                fieldSet.add(data.fieldMapByName.get(field));
            }
        }

        for (String field: fieldSet) {
            simplifiedCode.append(field);
        }

        Set<String> methodSet = new HashSet();
        TypeDeclaration finalNode = (TypeDeclaration) node;
        bodyDeclaration.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodInvocation methodInvocation) {
                String methodName = methodInvocation.getName().toString();
                if (methodInvocSet.contains(methodName)) {
                    IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
                    if (methodBinding == null) {
                        //System.out.println("    Couldn't resolve: " + methodInvocation.toString());
                    } else {
                        //System.out.println("    Successfully resolve: " + methodInvocation.toString());
                        try {
                            StringBuilder methodSignature = new StringBuilder();
                            if (Modifier.isStatic(methodBinding.getModifiers())) {
                                methodSignature.append("static ");
                            }
                            String returnType = simplifyTypeName(methodBinding.getReturnType());

                            methodSignature.append(returnType);
                            methodSignature.append(' ');
                            String declaringClass = methodBinding.getDeclaringClass().getName();
                            if (declaringClass.compareTo(finalNode.getName().toString()) != 0) {
                                methodSignature.append(simplifyTypeName(declaringClass));
                                methodSignature.append('#');
                            }
                            methodSignature.append(methodName);
                            methodSignature.append('(');
                            ITypeBinding[] paramTypes = methodBinding.getParameterTypes();
                            List<String> paramNames = DevUtils.getParamNames(methodBinding.getMethodDeclaration());
                            for (int i = 0; i < paramTypes.length; ++i) {
//                                methodSignature.append(paramTypes[i].getName());
//                                methodSignature.append(' ');
                                if (i < paramNames.size()) {
                                    methodSignature.append(paramNames.get(i));
                                } else {
                                    String alternateParamName = paramTypes[i].getName();
                                    alternateParamName = alternateParamName.substring(0, 1).toLowerCase() + alternateParamName.substring(1);
                                    alternateParamName = alternateParamName.replaceAll("[^a-zA-Z]", "");
                                    methodSignature.append(alternateParamName);
                                }
                                if (i < paramTypes.length - 1) {
                                    methodSignature.append(",");
                                }
                            }
                            methodSignature.append(");");

                            // Ignore methods which do not have any param or return a value
                            if (!(paramTypes.length == 0 && returnType.compareTo("void") == 0)) {
                                methodSet.add(methodSignature.toString());
                            }
                        } catch (Exception e) {
                        }
                    }
                }
                return true;
            }
        });

        for (String method: methodSet) {
            simplifiedCode.append(method);
        }

        return simplifiedCode.toString();
    }

    public String getMethodDeclarationContext(MethodDeclaration methodDeclaration, Set<String> methodInvocSet) {
        return getTypeDeclarationContext(methodDeclaration, methodInvocSet);
    }

    public String getInitializerContext(Initializer initializer, Set<String> methodInvocSet) {
        return getTypeDeclarationContext(initializer, methodInvocSet);
    }

    private String getTypeDeclarationContextForTesting(BodyDeclaration bodyDeclaration, Set<String> fieldCandidateSet, Set<String> methodCandidateSet, String methodInvoc) {
        ASTNode node = bodyDeclaration;
        while (!(node == null || node instanceof TypeDeclaration)) node = node.getParent();
        if (node == null) return null;

        TypeDeclarationData data = getTypeDeclarationData((TypeDeclaration) node);
        StringBuilder simplifiedCode = new StringBuilder();
        simplifiedCode.append(data.context);

        Set<String> fieldSet = new LinkedHashSet<>();
        for (String field: data.fieldList) {
            if (fieldCandidateSet.contains(field)) {
                fieldSet.add(data.fieldMapByName.get(field));
            }
        }

        for (String field: fieldSet) {
            simplifiedCode.append(field);
        }

        Set<String> methodSet = new HashSet();
        for (String methodCandidate: methodCandidateSet) {
            methodSet.addAll(data.methodMapByName.getOrDefault(methodCandidate, new ArrayList<>()));
        }

        for (String method: methodSet) {
            simplifiedCode.append(method);
        }

        Set<String> finalMethodSet = new HashSet<>();
        TypeDeclaration finalNode = (TypeDeclaration) node;
        bodyDeclaration.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodInvocation methodInvocation) {
                String methodName = methodInvocation.getName().toString();
                if (methodInvoc.compareTo(methodName) == 0) {
                    IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
                    if (methodBinding == null) {
                        //System.out.println("    Couldn't resolve: " + methodInvocation.toString());
                    } else {
                        //System.out.println("    Successfully resolve: " + methodInvocation.toString());
                        try {
                            StringBuilder methodSignature = new StringBuilder();
                            if (Modifier.isStatic(methodBinding.getModifiers())) {
                                methodSignature.append("static ");
                            }
                            String returnType = simplifyTypeName(methodBinding.getReturnType());

                            methodSignature.append(returnType);
                            methodSignature.append(' ');
                            String declaringClass = methodBinding.getDeclaringClass().getName();
                            if (declaringClass.compareTo(finalNode.getName().toString()) != 0) {
                                methodSignature.append(simplifyTypeName(declaringClass));
                                methodSignature.append('#');
                            }
                            methodSignature.append(methodName);
                            methodSignature.append('(');
                            ITypeBinding[] paramTypes = methodBinding.getParameterTypes();
                            List<String> paramNames = DevUtils.getParamNames(methodBinding.getMethodDeclaration());
                            for (int i = 0; i < paramTypes.length; ++i) {
//                                methodSignature.append(paramTypes[i].getName());
//                                methodSignature.append(' ');
                                if (i < paramNames.size()) {
                                    methodSignature.append(paramNames.get(i));
                                } else {
                                    String alternateParamName = paramTypes[i].getName();
                                    alternateParamName = alternateParamName.substring(0, 1).toLowerCase() + alternateParamName.substring(1);
                                    alternateParamName = alternateParamName.replaceAll("[^a-zA-Z]", "");
                                    methodSignature.append(alternateParamName);
                                }
                                if (i < paramTypes.length - 1) {
                                    methodSignature.append(",");
                                }
                            }
                            methodSignature.append(");");

                            // Ignore methods which do not have any param or return a value
                            if (!(paramTypes.length == 0 && returnType.compareTo("void") == 0)) {
                                if (!methodSet.contains(methodSignature.toString())) {
                                    finalMethodSet.add(methodSignature.toString());
                                }
                            }
                        } catch (Exception e) {
                        }
                    }
                }
                return true;
            }
        });

        for (String method: finalMethodSet) {
            simplifiedCode.append(method);
        }

        return simplifiedCode.toString();
    }

    public String getMethodDeclarationContextForTesting(MethodDeclaration methodDeclaration, Set<String> fieldCandidateSet, Set<String> methodCandidateSet, String methodInvoc) {
        return getTypeDeclarationContextForTesting(methodDeclaration, fieldCandidateSet, methodCandidateSet, methodInvoc);
    }

    public String getInitializerContextForTesting(Initializer initializer, Set<String> fieldCandidateSet, Set<String> methodCandidateSet, String methodInvoc) {
        return getTypeDeclarationContextForTesting(initializer, fieldCandidateSet, methodCandidateSet, methodInvoc);
    }

    @Override
    protected void exportCode(String sourceCode, File outputFolder, File project, File file) {
        String projectPath = project.getAbsolutePath();
        String relativeFilePath = file.getAbsolutePath();
        relativeFilePath = relativeFilePath.substring(projectPath.length() - project.getName().length());
        //System.out.println(relativeFilePath);

        CompilationUnit cu = null;
        try {
            cu = parser.createCU(file.getName(), sourceCode);
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace();
            return;
        } catch (NullPointerException npe) {
            npe.printStackTrace();
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

                    String outputFileName = finalRelativeFilePath.substring(0, finalRelativeFilePath.indexOf('\\') + 1);
                    outputFileName = outputFileName + finalRelativeFilePath.substring(finalRelativeFilePath.indexOf('\\') + 1)
                            .replace("\\", "_");
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
//                    if (file.getName().compareTo("Test.java") == 0) {
//                        System.out.println(simplifiedCode);
//                        System.out.println();
//                    }
                }
                return true;
            }
        });
    }

    public static String simplifyTypeName(Object typeName) {
        if (typeName == null) return "null";
        if (typeName.toString().indexOf(' ') >= 0) {
            if (typeName.toString().contains("interface") || typeName.toString().contains("class")) {
                String[] typeNameTokens = typeName.toString().trim().split("\\s+");
                for (int i = 0; i < typeNameTokens.length; ++i) {
                    if (typeNameTokens[i].compareTo("interface") == 0) {
                        typeName = typeNameTokens[i + 1];
                        break;
                    }
                    if (typeNameTokens[i].compareTo("class") == 0) {
                        typeName = typeNameTokens[i + 1];
                        break;
                    }
                }
            }
            if (typeName.toString().contains("Class#RAW")) {
                return "Class";
            }
        }
        if (typeName.toString().indexOf('<') >= 0) {
            typeName = typeName.toString().substring(0, typeName.toString().indexOf('<'));
        }
        return typeName.toString().substring(typeName.toString().lastIndexOf('.') + 1);
    }

    public static String preprocessCodeBlock(String codeBlock) {
        codeBlock = RemoveNewLineDecorator.preprocess(codeBlock);
        codeBlock = RemoveRedundantSpaceDecorator.preprocess(codeBlock);
        codeBlock = EmptyStringLiteralDecorator.preprocess(codeBlock);
        codeBlock = RemoveAnnotationDecorator.preprocess(codeBlock);
        codeBlock = NormalizeCharLiteralDecorator.preprocess(codeBlock);
        codeBlock = NormalizeTypeLiteralDecorator.preprocess(codeBlock);
        codeBlock = NormalizeMethodRefDecorator.preprocess(codeBlock);
        codeBlock = RemoveArrayInitializerDecorator.preprocess(codeBlock);
        //codeBlock = RemoveArrayAccessIndexDecorator.preprocess(codeBlock);
        codeBlock = NormalizeCompoundDecorator.preprocess(codeBlock);
        codeBlock = NormalizeLambdaExprDecorator.preprocess(codeBlock);
        return codeBlock;
    }

    public static void main(String[] args) {
//        String inputFolder = Config.REPO_DIR + "oneproj/";
//        String outputFolder = Config.LOG_DIR + "dataset-sample-method/";
        String inputFolder = "../../Kien/Flute-Kien-full/storage/repositories/git/four_hundred_excluded/";
        String outputFolder = "../../Tannm/storage/" + "dataset-gpt-method/";

        Preprocessor preprocessor = new Preprocessor();
        System.out.println("\nBacking up projects...");
        preprocessor.preprocessProjects(new File(inputFolder), new File("../../Tannm/storage/" + "backup/"));

        System.out.println("\nPreprocessing projects...");
        preprocessor = new RemoveCommentDecorator(preprocessor);
        preprocessor = new RemoveNewLineDecorator(preprocessor);
        preprocessor = new RemoveIndentDecorator(preprocessor);
        preprocessor.preprocessProjects(new File(inputFolder), new File(inputFolder));

        System.out.println("\nExtracting methods...");
        preprocessor = new MethodExtractor();
        preprocessor.preprocessProjects(new File(inputFolder), new File(outputFolder));

//        String inputFolder = "D:\\Flute\\storage\\repositories\\git\\eclipse\\";
//        String outputFolder = Config.LOG_DIR + "dataset-gpt-method/";
//
//        Preprocessor preprocessor = new Preprocessor();
//        System.out.println("\nBacking up project...");
//        preprocessor.preprocessProject(new File(inputFolder),
//                new File(Config.LOG_DIR + "backup/"),
//                new File(Config.LOG_DIR + "eclipse/eclipse_train.txt"));
//
//        System.out.println("\nPreprocessing project...");
//        preprocessor = new RemoveCommentDecorator(preprocessor);
//        preprocessor = new RemoveNewLineDecorator(preprocessor);
//        preprocessor = new RemoveIndentDecorator(preprocessor);
//        preprocessor.preprocessProject(new File(inputFolder),
//                (new File(inputFolder)).getParentFile(),
//                new File(Config.LOG_DIR + "eclipse/eclipse_train.txt"));
//
//        System.out.println("\nExtracting methods...");
//        preprocessor = new MethodExtractor();
//        preprocessor.preprocessProject(new File(inputFolder),
//                new File(outputFolder),
//                new File(Config.LOG_DIR + "eclipse/eclipse_train.txt"));
    }
}
