package flute.tokenizing.exe;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import flute.jdtparser.FileParser;
import flute.jdtparser.ProjectParser;
import flute.tokenizing.excode_data.NodeSequenceInfo;
import flute.tokenizing.excode_data.RecTest;
import flute.utils.file_processing.DirProcessor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class RecTestGenerator {
    private JavaExcodeTokenizer tokenizer;
    private ProjectParser projectParser;
    private FileParser fileParser;

    public RecTestGenerator(String projectPath, ProjectParser projectParser) {
        tokenizer = new JavaExcodeTokenizer(projectPath);
        this.projectParser = projectParser;
    }

    public JavaExcodeTokenizer getTokenizer() {
        return tokenizer;
    }

    public ProjectParser getProjectParser() {
        return projectParser;
    }

    public FileParser getFileParser() {
        return fileParser;
    }

    public void setFileParser(FileParser fileParser) {
        this.fileParser = fileParser;
    }

    public List<? extends RecTest> generateAll(int threshold) {
        List<File> javaFiles = DirProcessor.walkJavaFile(tokenizer.getProject().getAbsolutePath());
        List<RecTest> tests = new ArrayList<>();
        for (File file: javaFiles) {
            List<? extends RecTest> fileTests = generate(file.getAbsolutePath());
            while (threshold >= 0 && (tests.size() + fileTests.size()) > threshold) {
                fileTests.remove(fileTests.size() - 1);
            }
            tests.addAll(fileTests);
            if (tests.size() >= threshold) break;
        }
        return tests;
    }

    public List<? extends RecTest> generateAll() {
        return generateAll(-1);
    }

    public List<? extends RecTest> generate(String javaFilePath) {
        System.out.println("File path: " + javaFilePath);
        List<RecTest> tests = new ArrayList<>();
        List<NodeSequenceInfo> excodes = getTokenizer().tokenize(javaFilePath);
        if (excodes.isEmpty()) return tests;
        MethodDeclaration methodDeclaration = null;
        int methodDeclarationIdx = -1;
        setFileParser(null);

        for (int i = 0; i < excodes.size(); ++i) {
            NodeSequenceInfo excode = excodes.get(i);

            if (excode.oriNode instanceof MethodDeclaration) {
                if (methodDeclaration == null) {
                    methodDeclaration = (MethodDeclaration) excode.oriNode;
                    methodDeclarationIdx = i;
                } else if (excode.oriNode == methodDeclaration) {
                    if (getFileParser() == null) {
                        File javaFile = new File(javaFilePath);
                        Node node = methodDeclaration;
                        while (!(node instanceof CompilationUnit)) node = node.getParentNode().get();

                        fileParser = new FileParser(getProjectParser(), javaFile.getName(), node.toString(),
                                methodDeclaration.getBegin().get().line, methodDeclaration.getBegin().get().column);
                    }

                    methodDeclaration = null;
                    tests.addAll(generateInMethodScope(excodes, methodDeclarationIdx, i));
                }
            }
            if (methodDeclaration == null) continue;
        }
        return tests;
    }

    abstract List<RecTest> generateInMethodScope(List<NodeSequenceInfo> excodes, int methodDeclarationStartIdx, int methodDeclarationEndIdx);
}
