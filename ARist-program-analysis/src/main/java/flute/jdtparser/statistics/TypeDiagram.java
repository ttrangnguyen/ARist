package flute.jdtparser.statistics;

import flute.config.Config;
import flute.data.type.TypeConstraintKey;
import flute.jdtparser.ProjectParser;
import flute.utils.ProgressBar;
import flute.utils.file_processing.DirProcessor;
import flute.utils.logging.Logger;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TypeDiagram {
    public static void main(String[] args) throws IOException {
        Config.loadConfig(Config.STORAGE_DIR + "/json/eclipse.json");

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

    public static void init(ProjectParser projectParser) {
        List<File> allJavaFiles = DirProcessor.walkJavaFile(Config.PROJECT_DIR);

        List<File> javaFiles = allJavaFiles.stream().filter(file -> {
            if (!file.getAbsolutePath().contains("src")) return false;

            for (String blackName : Config.BLACKLIST_NAME_SRC) {
                if (file.getAbsolutePath().contains(blackName)) return false;
            }

            return true;
        }).collect(Collectors.toList());
        float countFile = 0;

        System.out.println("Init type tree...");
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
        }
        System.out.println("Finish init...");
    }

    private static void typeProcess(TypeDeclaration typeDeclaration) {
        List<List<ITypeBinding>> tree = typeGraph(typeDeclaration.resolveBinding());
        tree.forEach(line -> {
            if (line.size() > 1) {
                //write line
                List<String> lineText = new ArrayList<>();
                line.forEach(node -> {
                    lineText.add(node.getKey() + "||" + node.getName());
                });
                Logger.writeData(String.join("=->", lineText), Config.STORAGE_DIR + "/flute-ide/" + Config.PROJECT_NAME + "_class_tree.txt");
            }
        });
    }

    private static List<List<ITypeBinding>> typeGraph(ITypeBinding iTypeBinding) {
        List<List<ITypeBinding>> result = new ArrayList<>();
        if (iTypeBinding == null) return result;
        List<ITypeBinding> firstLine = new ArrayList<>();
        firstLine.add(iTypeBinding);
        result.add(firstLine);

        while (!checkEndNodes(result)) {
            for (int i = 0; i < result.size(); i++) {
                List<ITypeBinding> line = result.get(i);
                List<ITypeBinding> nextTypes = nextSuper(line.get(line.size() - 1)); //get last node
                if (nextTypes.size() > 0) {
                    for (int j = 0; j < nextTypes.size() - 1; j++) {
                        //clone
                        result.add(new ArrayList<>(line));
                        List<ITypeBinding> lastLine = result.get(result.size() - 1);
                        lastLine.add(nextTypes.get(j + 1));
                    }
                    line.add(nextTypes.get(0));
                }
            }
        }

        return result;
    }

    private static List<ITypeBinding> getLastNodes(List<List<ITypeBinding>> tree) {
        List<ITypeBinding> result = new ArrayList<>();
        tree.forEach(line -> {
            result.add(line.get(line.size() - 1));
        });
        return result;
    }

    private static boolean checkEndNodes(List<List<ITypeBinding>> tree) {
        for (ITypeBinding typeNode : getLastNodes(tree)) {
            if (nextSuper(typeNode).size() > 0) return false;
        }
        return true;
    }

    private static List<ITypeBinding> nextSuper(ITypeBinding iTypeBinding) {
        List<ITypeBinding> result = new ArrayList<>();
        if (iTypeBinding == null) return result;
        if (iTypeBinding.getSuperclass() != null && !iTypeBinding.getSuperclass().getKey().equals(TypeConstraintKey.OBJECT_TYPE)) {
            result.add(iTypeBinding.getSuperclass());
        }

        for (ITypeBinding interfaceItem :
                iTypeBinding.getInterfaces()) {
            result.add(interfaceItem);
        }

        return result;
    }
}


//class TypeNode {
//    private ITypeBinding type;
//    private TypeNode nextNode;
//
//    public TypeNode(ITypeBinding type) {
//        this.type = type;
//    }
//
//    public ITypeBinding getType() {
//        return type;
//    }
//
//    public void setType(ITypeBinding type) {
//        this.type = type;
//    }
//
//    public TypeNode getNextNode() {
//        return nextNode;
//    }
//
//    public void setNextNode(TypeNode nextNode) {
//        this.nextNode = nextNode;
//    }
//}