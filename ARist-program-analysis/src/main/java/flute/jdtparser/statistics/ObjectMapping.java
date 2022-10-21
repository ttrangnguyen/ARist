package flute.jdtparser.statistics;

import flute.config.Config;
import flute.data.type.TypeConstraintKey;
import flute.jdtparser.ProjectParser;
import flute.utils.Pair;
import flute.utils.ProgressBar;
import flute.utils.file_processing.DirProcessor;
import flute.utils.logging.Logger;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ObjectMapping {
    public static void main(String[] args) throws IOException {
        Config.loadConfig(Config.STORAGE_DIR + "/json/rt.json");

        ProjectParser projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
                Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);

        List<File> allJavaFiles = DirProcessor.walkJavaFile(Config.PROJECT_DIR);

        List<File> javaFiles = allJavaFiles.stream().filter(file -> {
            if (!file.getAbsolutePath().contains("src")) return false;

            for (String blackName : Config.BLACKLIST_NAME_SRC) {
                if (file.getAbsolutePath().contains(blackName)) return false;
            }

            return true;
        }).collect(Collectors.toList());
        float countFile = 0;

        ProgressBar progressBar = new ProgressBar();
        System.out.println("Prepare file parser");
        //visit astnode
        for (File file : javaFiles) {
            CompilationUnit cu = projectParser.createCU(file);
            cu.accept(new ASTVisitor() {
                @Override
                public boolean visit(TypeDeclaration typeDeclaration) {
                    typeProcess(typeDeclaration);
                    return true;
                }
            });
            progressBar.setProgress(countFile++ / javaFiles.size(), true);
        }
    }

    public static void typeProcess(TypeDeclaration typeDeclaration) {
        HashMap<String, HashMap<String, Integer>> typeNameList = new HashMap<>();
        HashSet<Pair<String, String>> setType2Name = new HashSet<>();

        if (typeDeclaration.typeParameters().size() > 0) {
            List typeParameter = typeDeclaration.typeParameters();

            List<String> typeParamName = (List<String>) typeParameter.stream().map(item -> {
                TypeParameter typeParameterItem = (TypeParameter) item;
                return typeParameterItem.resolveBinding().getName();
            }).collect(Collectors.toList());

            for (MethodDeclaration method : typeDeclaration.getMethods()) {
                if (method.parameters().size() > 0) {
                    method.parameters().forEach(param -> {
                        SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) param;
                        String typeName = singleVariableDeclaration.getType().resolveBinding().getName();
                        String varName = singleVariableDeclaration.getName().toString();

                        if (typeParamName.contains(typeName)) {
                            if (typeNameList.get(typeName) == null) {
                                HashMap<String, Integer> newHM = new HashMap<>();
                                newHM.put(varName, 1);
                                setType2Name.add(new Pair<>(String.valueOf(typeParamName.indexOf(typeName)), varName));
                                typeNameList.put(typeName, newHM);
                            } else {
                                HashMap<String, Integer> typeMap = typeNameList.get(typeName);
                                if (typeMap.get(varName) == null) {
                                    typeMap.put(varName, 1);
                                } else {
                                    typeMap.put(varName, typeMap.get(varName) + 1);
                                }
                            }
                        }
                    });
                }
            }
            for (MethodDeclaration methodDeclaration : typeDeclaration.getMethods()) {
                for (int i = 0; i < methodDeclaration.parameters().size(); i++) {
                    Object param = methodDeclaration.parameters().get(i);
                    SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) param;
                    if (singleVariableDeclaration.resolveBinding().getType().getKey().equals(TypeConstraintKey.OBJECT_TYPE)) {
                        String paramName = singleVariableDeclaration.getName().toString();
                        int mapping = -1;
                        for (Pair<String, String> item : setType2Name) {
                            item.getSecond().equals(paramName);
                            mapping = Integer.valueOf(item.getFirst());
                        }
                        if (mapping != -1) {
                            System.out.println(methodDeclaration.resolveBinding().getKey() +
                                    "-" + mapping + "-" + i);
                            Logger.write(methodDeclaration.resolveBinding().getKey() +
                                    "||" + mapping + "||" + i,"object_mapping.txt");
                        }
                    }
                }
            }
        }
    }
}