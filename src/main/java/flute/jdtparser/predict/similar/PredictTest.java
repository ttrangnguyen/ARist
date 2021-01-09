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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PredictTest {
    private static String projectName = "log4j";
    private static ProjectParser projectParser;

    private static int shortName = 0; //name have no more 3 character
    private static int numberName = 0; //name have least one number in character
    private static int localName = 0; //name is local variable
    private static int shortLocalName = 0; //short name is variable name

    private static long numberOfTest = 0;

    public static boolean containsNumber(String s) {
        return Pattern.compile("[0-9]").matcher(s).find();
    }

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
        List<SimilarData> similarZeroList = new ArrayList<>();

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

        HashMap<Float, ResultMap> result = new HashMap<>(); //number candidate/precision
        for (int i = 0; i <= numberOfSet; i++) {
            result.put(i * 1f / numberOfSet, new ResultMap());
        }

        ProgressBar progressBar = new ProgressBar();
        long fileCount = 0;
        for (File javaFile : javaFiles) {
            progressBar.setProgress(fileCount++ * 1f / javaFiles.size(), true);
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

                                Object oldN = nextLexList;

                                nextLexList = nextLexList.stream().filter(nextLex -> {
                                    return !(nextLex.equals("\"\"") || nextLex.equals("true") || nextLex.equals("false") || nextLex.contains("."));
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

                                numberOfTest++;

                                if (similarData.getStep1Result()) {
                                    for (Float key : result.keySet()) {
                                        if (key <= similarData.getExpectedOutputSimilarly()) {
                                            result.get(key).setPrecision(result.get(key).getPrecision() + 1);
                                        }
                                    }
                                }

                                for (Float key : result.keySet()) {
                                    for (Float candidateSimilarly : similarData.getCandidatesSimilarly()) {
                                        if (key <= candidateSimilarly) {
                                            result.get(key).setNumCandidate(result.get(key).getNumCandidate() + 1);
                                        }
                                    }
                                }

                                if (similarData.getStep1Result() && similarData.getExpectedOutputSimilarly() == 0) {
                                    similarZeroList.add(similarData);
                                    Logger.write(gson.toJson(similarData), projectName + "_similarly_zero.txt");
                                    if (containsNumber(similarData.getArgName()) || containsNumber(similarData.getExpectedOutput())) {
                                        numberName++;
                                    }
                                    if (similarData.getArgName().length() < 4 || similarData.getExpectedOutput().length() < 4) {
                                        shortName++;
                                        if (fileParser.getLocalVariableList().contains(similarData.getArgName())) {
                                            shortLocalName++;
                                        }
                                    }
                                    if (fileParser.getLocalVariableList().contains(similarData.getArgName())) {
                                        localName++;
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

        System.out.printf("Statistic in arg have lexSim equal 0:");
        System.out.printf("Argument or parameter have least one number: %4.2f%%\n", numberName * 100.0f / similarZeroList.size());
        System.out.printf("Argument or parameter have no more 3 character: %4.2f%%\n", shortName * 100.0f / similarZeroList.size());
        System.out.printf("Argument or parameter is local variable: %4.2f%%\n", localName * 100.0f / similarZeroList.size());
        System.out.printf("Argument or parameter is short local variable: %4.2f%%\n", shortLocalName * 100.0f / similarZeroList.size());

        Logger.write(("Statistic in arg have lexSim equal 0", projectName + "_result_similarly.csv");
        Logger.write(String.format("Argument or parameter have least one number, %4.2f%%", numberName * 100.0f / similarZeroList.size()), projectName + "_result_similarly.csv");
        Logger.write(String.format("Argument or parameter have no more 3 character, %4.2f%%", shortName * 100.0f / similarZeroList.size()), projectName + "_result_similarly.csv");
        Logger.write(String.format("Argument or parameter is local variable, %4.2f%%\n", localName * 100.0f / similarZeroList.size()), projectName + "_result_similarly.csv");
        Logger.write(String.format("Argument or parameter is short local variablem %4.2f%%\n", shortLocalName * 100.0f / similarZeroList.size()), projectName + "_result_similarly.csv");

        Logger.write(String.format("Number of test, %d", numberOfTest), projectName + "_result_similarly.csv");
        Logger.write("Alpha, Candidates, Precision, ", projectName + "_result_similarly.csv");
        for (int i = 0; i <= numberOfSet; i++) {
            float key = i * 1f / numberOfSet;
            result.get(key).setPrecision(result.get(key).getPrecision() * 1f / numberOfTest);
            System.out.println(String.format("%6.2f%%", key * 100f)
                    + " \t " + String.format("%8d candidates", result.get(key).getNumCandidate())
                    + " \t " + String.format("%.4f%%", result.get(key).getPrecision() * 100f));
            Logger.write(String.format("%.2f%%", key * 100f)
                    + ", " + result.get(key).getNumCandidate()
                    + ", " + String.format("%.4f%%", result.get(key).getPrecision() * 100f), projectName + "_result_similarly.csv");
        }
    }
}

class ResultMap {
    private long numCandidate;
    private float precision;

    public ResultMap() {
        numCandidate = 0;
        precision = 0f;
    }

    public long getNumCandidate() {
        return numCandidate;
    }

    public void setNumCandidate(long numCandidate) {
        this.numCandidate = numCandidate;
    }

    public float getPrecision() {
        return precision;
    }

    public void setPrecision(float precision) {
        this.precision = precision;
    }
}