package flute.jdtparser.predict.similar;

import com.google.gson.Gson;
import flute.communicate.SocketClient;
import flute.config.Config;
import flute.jdtparser.FileParser;
import flute.jdtparser.ProjectParser;
import flute.utils.file_processing.DirProcessor;
import flute.utils.logging.Logger;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class PredictTest {
    private static String projectName = "log4j";
    private static ProjectParser projectParser;

    private static long numberOfTest = 0;


    public static MethodDeclaration findMethodDeclaration(IMethodBinding iMethodBinding, CompilationUnit curCu) {
        ASTNode methodDeclaration = curCu.findDeclaringNode(iMethodBinding.getKey());
        if (methodDeclaration != null) {
            return (MethodDeclaration) methodDeclaration;
        }
        //create a compilation unit from binding class
        CompilationUnit virtualCu = projectParser.createCU(iMethodBinding.getDeclaringClass().getName(), iMethodBinding.getDeclaringClass().toString());
        return (MethodDeclaration) virtualCu.findDeclaringNode(iMethodBinding.getKey());
    }

    public static void main(String[] args) throws Exception {
        Config.loadConfig(Config.STORAGE_DIR + "/json/" + projectName + ".json");

        projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
                Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);

        List<File> javaFiles = DirProcessor.walkJavaFile(Config.PROJECT_DIR).stream().filter(file -> {
            if (!file.getAbsolutePath().contains("src")) return false;

            for (String blackName : Config.BLACKLIST_NAME_SRC) {
                if (file.getAbsolutePath().contains(blackName)) return false;
            }

            return true;
        }).collect(Collectors.toList());

        SocketClient socketClient = new SocketClient(18007);
        Gson gson = new Gson();

        int numberOfSet = 10;

        HashMap<Float, Float> result = new HashMap<>();
        for (int i = 0; i <= numberOfSet; i++) {
            result.put(i * 1f / numberOfSet, 0f);
        }

        for (File javaFile : javaFiles) {
            FileParser fileParser = new FileParser(projectParser, javaFile, 0);
            fileParser.getCu().accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodInvocation methodInvocation) {
                    try {
                        fileParser.setPosition(methodInvocation.getStartPosition() + methodInvocation.getLength() - 2);
                        for (int idx = 0; idx < methodInvocation.arguments().size(); idx++) {
                            if (methodInvocation.arguments().get(idx) instanceof SimpleName
                                    && fileParser.getCurMethodInvocation().getOrgASTNode() == methodInvocation) {
                                SimilarData similarData = new SimilarData();

                                //add expected output
                                similarData.setExpectedOutput(methodInvocation.arguments().get(idx).toString());

                                IMethodBinding binding = (IMethodBinding) methodInvocation.getName().resolveBinding();

                                //break on varargs
                                if (binding.isVarargs() && idx >= binding.getParameterTypes().length) continue;

                                MethodDeclaration methodDeclaration = findMethodDeclaration(binding, fileParser.getCu());

                                if (methodDeclaration.parameters().get(idx) instanceof SingleVariableDeclaration) {
                                    SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) methodDeclaration.parameters().get(idx);
                                    similarData.setArgName(singleVariableDeclaration.getName().toString());
                                }

                                //add next list
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

                                if (nextLexList.contains(similarData.getExpectedOutput())) {
                                    similarData.setStep1Result(true);
                                } else {
                                    similarData.setStep1Result(false);
                                }

                                //add next similarly
                                List<Float> nextSimilarly = new ArrayList<>();
                                for (String nextLex : nextLexList) {
                                    nextSimilarly.add(
                                            socketClient.lexSimService(nextLex, similarData.getArgName()).orElse(-1f)
                                    );
                                }

                                similarData.setCandidatesSimilarly(nextSimilarly);

                                System.out.println(gson.toJson(similarData));
                                numberOfTest++;

                                if (similarData.getStep1Result()) {
                                    for (Float key : result.keySet()) {
                                        if (key <= similarData.getExpectedOutputSimilarly()) {
                                            result.put(key, result.get(key) + 1);
                                        }
                                    }
                                }

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

        Logger.delete(projectName + "_result_similarly.csv");
        Logger.write(String.format("Number of test, %d", numberOfTest), projectName + "_result_similarly.csv");
        Logger.write("Alpha, Precision", projectName + "_result_similarly.csv");
        for (int i = 0; i <= numberOfSet; i++) {
            float key = i * 1f / numberOfSet;
            result.put(key, result.get(key) * 1f / numberOfTest);
            System.out.println(String.format("%5.2f%%", key * 100f) + " \t " + String.format("%.4f%%", result.get(key) * 100f));
            Logger.write(String.format("%.2f%%", key * 100f) + ", " + String.format("%.4f%%", result.get(key) * 100f), projectName + "_result_similarly.csv");
        }
    }
}