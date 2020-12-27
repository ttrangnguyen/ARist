package flute.jdtparser.predict.similar;

import com.google.gson.Gson;
import flute.communicate.SocketClient;
import flute.config.Config;
import flute.jdtparser.FileParser;
import flute.jdtparser.ProjectParser;
import flute.utils.ProgressBar;
import flute.utils.file_processing.DirProcessor;
import flute.utils.logging.Logger;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class PredictTest {
    private static float alpha = 0.5f;
    private static String projectName = "netbeans";
    private static List<FileParser> fileParserList = new ArrayList();

    private static HashMap<String, MethodDeclaration> methodDeclarationHashMap = new HashMap<>();

    public static void main(String[] args) throws Exception {
        Config.loadConfig(Config.STORAGE_DIR + "/json/" + projectName + ".json");

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

        ProgressBar progressBar = new ProgressBar();
        int numFile = 0;
        for (File javaFile : javaFiles) {
            FileParser fileParser = new FileParser(projectParser, javaFile, 0);
            fileParserList.add(fileParser);
            fileParser.getCu().accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodDeclaration methodDeclaration) {
                    methodDeclarationHashMap.put(methodDeclaration.resolveBinding().getKey(), methodDeclaration);
                    return true;
                }
            });
            progressBar.setProgress(numFile++ * 1f / javaFiles.size(), true);
        }

        SocketClient socketClient = new SocketClient(18007);
        Gson gson = new Gson();

        for (FileParser fileParser : fileParserList) {
            fileParser.getCu().accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodInvocation methodInvocation) {
                    try {
                        fileParser.setPosition(methodInvocation.getStartPosition() + methodInvocation.getLength() - 2);
                        for (int idx = 0; idx < methodInvocation.arguments().size(); idx++) {
                            if (methodInvocation.arguments().get(idx) instanceof SimpleName
                                    && fileParser.getCurMethodInvocation().getOrgASTNode() == methodInvocation) {
                                SimilarData similarData = new SimilarData();
                                similarData.setExpectedOutput(methodInvocation.arguments().get(idx).toString());

                                IMethodBinding binding = (IMethodBinding) methodInvocation.getName().resolveBinding();

                                MethodDeclaration methodDeclaration = methodDeclarationHashMap.get(binding.getKey());

                                if (methodDeclaration.parameters().get(idx) instanceof SingleVariableDeclaration) {
                                    SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) methodDeclaration.parameters().get(idx);
                                    similarData.setArgName(singleVariableDeclaration.getName().toString());
                                }

                                HashMap<String, List<String>> params = fileParser.genParamsAt(idx).getValue();

                                List<String> nextExcodeList = new ArrayList<>(params.keySet());
                                List<String> nextLexList = new ArrayList<>();
                                for (String nextExcode : nextExcodeList) {
                                    nextLexList.addAll(params.get(nextExcode));
                                }

                                nextLexList = nextLexList.stream().filter(nextLex -> {
                                    return !nextLex.equals("\"\"");
                                }).collect(Collectors.toList());

                                similarData.setCandidates(nextLexList);


                                similarData.setExpectedOutputSimilarly(
                                        socketClient.lexSimService(similarData.getExpectedOutput(), similarData.getArgName()).orElse(-1f)
                                );

                                List<Float> nextSimilarly = new ArrayList<>();
                                for (String nextLex : nextLexList) {
                                    nextSimilarly.add(
                                            socketClient.lexSimService(nextLex, similarData.getArgName()).orElse(-1f)
                                    );
                                }

                                similarData.setCandidatesSimilarly(nextSimilarly);

                                System.out.println(gson.toJson(similarData));
                                Logger.write(gson.toJson(similarData), projectName + "_similarly.txt");
                            }
                        }
                    } catch (Exception e) {
//                        e.printStackTrace();
                    }
                    return true;
                }
            });
        }
    }
}
