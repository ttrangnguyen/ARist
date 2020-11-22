package flute.antlr4.parser;

import flute.antlr4.listener.ThrowingErrorListener;
import flute.antlr4.config.Config;
import flute.tokenizing.exe.GetDirStructureCrossProject;
import flute.tokenizing.parsing.JavaFileParser;
import flute.tokenizing.excode_data.FileInfo;
import flute.tokenizing.excode_data.NodeSequenceInfo;
import flute.tokenizing.excode_data.SystemTableCrossProject;
import flute.utils.Pair;
import flute.utils.file_processing.CountLOC;
import flute.utils.file_processing.DirProcessor;
import flute.utils.file_processing.JavaTokenizer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import flute.utils.logging.Logger;
import flute.utils.StringUtils;
import flute.tokenizing.visitors.MetricsVisitor;
import flute.antlr4.ExcodeLexer;
import flute.antlr4.ExcodeParser;
import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// eclipse stops = [0, 23000, 28000, 35000, 42000, 46177]
// netbeans stops = [0, 5000, 9759]

public class Parser {
    public static final int parseBegin = 5001;
    public static final int parseEnd = 9759;
    public static final int recursionMaxDepth = 1;
    public static final int threshPerDepth = 7;

    private HashMap<SystemTableCrossProject, String> systemTableCrossProjectMap;
    private HashMap<String, Integer> LOC;

    public Parser() {
        systemTableCrossProjectMap = new HashMap<>();
        LOC = new HashMap<>();
    }

    public void run() {
        translateJavaToExcode();
//        checkExcodeGrammar();
        createExcodeFiles();
    }

    private void translateJavaToExcode() {
        File[] projects = new File(Config.projectsPath).listFiles(File::isDirectory);
        for (File project : projects) {
            String srcPath;
//            if (project.getName().equals("eclipse-platform-sources-4.17")) {
//                srcPath = "";
//            } else continue;
            if (project.getName().equals("netbeans")) {
                srcPath = "/ide";
            } else continue;
//            if (project.getName().equals("ant")) {
//                srcPath = "/src/main";
//            } else if (project.getName().equals("batik")) {
//                srcPath = "/sources";
//            } else if (project.getName().equals("log4j")) {
//                srcPath = "/src/main/java";
//            } else if (project.getName().equals("lucene")) {
//                srcPath = "/lucene/src/java";
//            } else if (project.getName().equals("xalan")) {
//                srcPath = "/src/";
//            } else if (project.getName().equals("xerces")) {
//                srcPath = "/src/";
//            } else continue;
            translateJavaToExcode(project.getName(), project.getName() + srcPath);
        }
    }

    private void translateJavaToExcode(String projectName, String projectSrcPath) {
        Logger.initDebug("debugVisitor.txt");
        List<File> allSubFilesTmp = DirProcessor.walkJavaFile(Config.projectsPath + projectSrcPath);
        Integer numFiles = allSubFilesTmp.size();
        Integer fileCount = 0;
        Logger.log("allSubFiles size: " + numFiles);
        MetricsVisitor visitor = new MetricsVisitor();
        SystemTableCrossProject systemTableCrossProject = new SystemTableCrossProject();
        systemTableCrossProjectMap.put(systemTableCrossProject, projectName);
        Integer totalLOC = 0;
        for (File file : allSubFilesTmp) {
            fileCount++;
            if (fileCount-1 < parseBegin || fileCount-1 > parseEnd)
                continue;
            if (!canTest(file, projectName)) continue;
            Integer linesOfCode = CountLOC.count(file);
            totalLOC += linesOfCode;
//            System.out.println(file.getAbsolutePath());
            JavaFileParser.visitFile(visitor, file, systemTableCrossProject, "xxx/");
        }
        LOC.put(projectName, totalLOC);
        GetDirStructureCrossProject.buildSystemPackageList(systemTableCrossProject);
        systemTableCrossProject.buildTypeFullMap();
        systemTableCrossProject.buildMethodMap();
        systemTableCrossProject.buildMethodFullMap();
        systemTableCrossProject.buildFeasibleTypeListForFiles();
        systemTableCrossProject.buildTypeFullVariableMap();
        systemTableCrossProject.buildMethodInvocListForMethods();
        systemTableCrossProject.buildMethodDics();
        systemTableCrossProject.getTypeVarNodeSequence();
        systemTableCrossProject.buildNodeSeqDic();
        systemTableCrossProject.buildMethodInvocListForFiles();
        Logger.closeDebug();
    }

    private void checkExcodeGrammar() {
        try {
            for (Map.Entry<SystemTableCrossProject, String> entry : systemTableCrossProjectMap.entrySet()) {
                checkExcodeGrammar(entry.getKey(), entry.getValue());
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    private void checkExcodeGrammar(SystemTableCrossProject systemTableCrossProject, String projectName) throws IOException {
        System.out.println("Checking grammar");
        long mx = 0, cnt = 0, sumExcodeSize = 0, sumBigExcodeSize = 0;
        File fout = new File(Config.parsingResult);
        FileOutputStream fos = new FileOutputStream(fout);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        BufferedWriter bw2 = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(Config.parsingResultFast)));
        for (FileInfo fileInfo : systemTableCrossProject.fileList) {
            StringBuilder builder = new StringBuilder();
            for (NodeSequenceInfo node : fileInfo.getNodeSequenceList()) {
                builder.append(node.toString());
            }
            Instant start = Instant.now();
            parseExcodeSequence(builder.toString(), fileInfo, bw);
            Instant end = Instant.now();
            Duration timeElapsed = Duration.between(start, end);
            sumExcodeSize += builder.toString().length();
            if (timeElapsed.toMillis() > Config.maxParsingTimeInMillis) {
                mx = Math.max(mx, timeElapsed.toMillis());
                ++cnt;
                sumBigExcodeSize += builder.toString().length();
                bw.write(fileInfo.filePath); bw.newLine();
                bw.write("Time taken: " + timeElapsed.toMillis() +" milliseconds"); bw.newLine();
                bw.write("Excode size: " + builder.toString().length()); bw.newLine();
            } else {
                bw2.write(fileInfo.filePath); bw2.newLine();
                bw2.write("Time taken: " + timeElapsed.toMillis() +" milliseconds"); bw2.newLine();
                bw2.write("Excode size: " + builder.toString().length()); bw2.newLine();
            }
        }
        bw.newLine();
        bw.write("Slowest file" + mx); bw.newLine();
        bw.write("Num of slow files: " + cnt); bw.newLine();
        bw.write("Sum of big files: " + sumBigExcodeSize); bw.newLine();
        bw.write("Sum excode size total: " + sumExcodeSize); bw.newLine();
        bw.close();
        bw2.close();
    }

    private void createExcodeFiles() {
        try {
            for (Map.Entry<SystemTableCrossProject, String> entry : systemTableCrossProjectMap.entrySet()) {
                createExcodeFiles(entry.getKey(), entry.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean canTest(File file, String projectName) {
        if (projectName.equals("netbeans") || projectName.equals("eclipse-platform-sources-4.17")) {
            return file.getAbsolutePath().contains("src")
                    && !file.getAbsolutePath().contains("examples")
                    && !file.getAbsolutePath().contains("test")
                    && !file.getAbsolutePath().contains("demo");
        }
        return true;
    }

    private void createExcodeFiles(SystemTableCrossProject systemTableCrossProject, String projectName) throws IOException {
        String javaProjectTrainingPath = createDirectory(Config.javaTrainingPath, projectName);
        String javaProjectTestingPath = createDirectory(Config.javaTestingPath, projectName);
        String javaProjectValidatingPath = createDirectory(Config.javaValidatingPath, projectName);
        String excodeProjectTrainingPath = createDirectory(Config.excodeTrainingPath, projectName);
        String excodeProjectTestingPath = createDirectory(Config.excodeTestingPath, projectName);
        String excodeProjectValidatingPath = createDirectory(Config.excodeValidatingPath, projectName);
        FileWriter testFilePaths = new FileWriter(new File(Config.testFilePath + projectName + ".txt"), true);

        int testLOCThresh = (int) (LOC.get(projectName) * 0.1);
        int validateLOCThresh = (int) (LOC.get(projectName) * 0.9 * 0.15);
        int trainLOCThresh = LOC.get(projectName) - testLOCThresh - validateLOCThresh;
        int currentTestLOC = 0;
        int currentValidateLOC = 0;
        int currentTrainLOC = 0;

        boolean testCap = false;
        boolean validateCap = false;

        for (FileInfo fileInfo : systemTableCrossProject.fileList) {
            StringBuilder builder = new StringBuilder();
            for (NodeSequenceInfo node : fileInfo.getNodeSequenceList()) {
//                    System.out.print(node.toString());
                String space = " ";
//                    if (node.toString().contains("STSTM{")) {
//                        builder.append("\n");
//                        space = "";
//                    }
                builder.append(node.toString().replace(" ", "").replace("\r\n", space));
            }

            String[] filePath = fileInfo.filePath.split(Pattern.quote(File.separator));
            String excodeFilePath;
            String javaFileTokenPath;
            while (true) {
                if (canTest(fileInfo.file, projectName) && toTest() && !testCap) {
                    String absolutePath = fileInfo.file.getAbsolutePath();
                    testFilePaths.write(absolutePath.substring(absolutePath.indexOf(projectName)) + "\n");
                    javaFileTokenPath = javaProjectTestingPath +
                            fileInfo.file.getName().replace(".java", ".txt");
                    excodeFilePath = excodeProjectTestingPath +
                            filePath[filePath.length - 1].replace(".java", ".txt");
                    currentTestLOC += CountLOC.count(fileInfo.file);
                    if (currentTestLOC > testLOCThresh) testCap = true;
                    break;
                } else if (toValidate() && !validateCap){
                    javaFileTokenPath = javaProjectValidatingPath +
                            fileInfo.file.getName().replace(".java", ".txt");
                    excodeFilePath = excodeProjectValidatingPath +
                            filePath[filePath.length - 1].replace(".java", ".txt");
                    currentValidateLOC += CountLOC.count((fileInfo.file));
                    if (currentValidateLOC > validateLOCThresh) validateCap = true;
                    break;
                } else if (currentTrainLOC <= trainLOCThresh || (testCap && validateCap)){
                    javaFileTokenPath = javaProjectTrainingPath +
                            fileInfo.file.getName().replace(".java", ".txt");
                    excodeFilePath = excodeProjectTrainingPath +
                            filePath[filePath.length - 1].replace(".java", ".txt");
                    currentTrainLOC += CountLOC.count((fileInfo.file));
                    break;
                }
            }


            File fout = new File(excodeFilePath);
            FileOutputStream fos = new FileOutputStream(fout);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write(builder.toString());
            bw.close();

            FileWriter writer = new FileWriter(javaFileTokenPath);
            String fileContent = JavaTokenizer.removePackagesAndImports(fileInfo.file.getAbsolutePath());
            fileContent = fileContent.replaceAll("[a-zA-Z0-9_]*.class", ".class")
                            .replaceAll("\\[.*?]", "[]");

            ArrayList<String> tokens = JavaTokenizer.tokenize(fileContent);
            for (String token : tokens) {
                writer.write(token + "\n");
            }
            writer.close();
        }
        testFilePaths.close();
    }

    private String createDirectory(String root, String subDir) {
        File projectDirectory = new File(root + subDir + "/");
        if (!projectDirectory.exists()) {
            projectDirectory.mkdir();
        }
        return projectDirectory.getPath() + "/";
    }

//    private void createExcodeJSONData() {
//        try {
//            FileWriter writer = new FileWriter(Config.excodeJSONPath);
////            writer.append("[");
//            for (FileInfo fileInfo : systemTableCrossProject.fileList) {
//                StringBuilder builder = new StringBuilder();
//                for (NodeSequenceInfo node : fileInfo.getNodeSequenceList()) {
//                    builder.append(node.toString().replaceAll("\\r", " ").replaceAll("\\n",""));
//                }
//                writer.append("{\"input\": \"");
//                writer.append(builder.toString());
//                writer.append("\"}\n");
//            }
////            writer.append("]");
//            writer.close();
//        } catch (IOException ignored) {
//        }
//    }

    private boolean toTest() {
        return Math.random() < 0.1;
    }

    private boolean toValidate() {
        return Math.random() < 0.15;
    }

    public ArrayList<ArrayList<String>> identifyTemplates(String content /*, FileInfo fileInfo */) {
        ArrayList<String> exSeq = StringUtils.splitToArrayList(content, "[ \\n]");
        String lastExcodeStatement = "";
        int startPos = getStatementStartPosition(exSeq);
        for (int i = startPos; i < exSeq.size(); ++i) {
            lastExcodeStatement = StringUtils.concat(lastExcodeStatement, exSeq.get(i));
        }
        return expandExcodeSeq(exSeq, lastExcodeStatement, 0);
    }

    private int getStatementStartPosition(ArrayList<String> exSeq) {
        for (int i = exSeq.size() - 1; i > 0; --i) {
            if (exSeq.get(i).contains("STSTM{"))
                return i;
        }
        return 0;
    }

    public ArrayList<ArrayList<String>> expandExcodeSeq(ArrayList<String> exSeq, String lastExcodeStatement, int depth) {
//        ArrayList<ArrayList<String>> newExSeqs = new ArrayList<>();
//        if (isEnded(exSeq) || reachMaxDepth(depth)) {
//            newExSeqs.add(exSeq);
//        } else {
//            ArrayList<String> validTokens = getValidNextTokens(lastExcodeStatement);
//            if (validTokens.size() > 0) {
//                ArrayList<String> topCandidates = rank(exSeq, validTokens);
//                for (String candidate : topCandidates) {
//                    ArrayList<String> newExSeq = StringUtils.concat(exSeq, candidate);
//                    String newLastExcodeStatement = StringUtils.concat(lastExcodeStatement, candidate);
//                    ArrayList<ArrayList<String>> newTemplates = expandExcodeSeq(newExSeq, newLastExcodeStatement, depth + 1);
//                    newExSeqs.addAll(newTemplates);
//                }
//            }
//        }
//        return newExSeqs;
        final int maxExpansions = 500;
        final int maxKeep = 7;
        ArrayList<ArrayList<String>> newExSeqs = new ArrayList<>();
        int count = 0;
        while (count < maxExpansions) {

        }
        return newExSeqs;
    }

    private boolean isEnded(ArrayList<String> exSeq) {
        return exSeq.get(exSeq.size() - 1).contains("ENSTM{");
    }

    private boolean reachMaxDepth(int recursionDepth) {
        return recursionDepth >= recursionMaxDepth;
    }

    private ArrayList<String> getValidNextTokens(String subStatement) {
        ArrayList<String> candidates = new ArrayList<>();
        ArrayList<String> validTokens = new ArrayList<>();
        try{
            parseExcodeSequence(subStatement);
        } catch (ParseCancellationException e) {
            String message = e.getMessage();
//            System.out.println(e.getMessage());
            // e.g: line 1:338 missing 'ENSTM{EXPR}' at '<EOF>'
            //      line 1:327 mismatched input '<EOF>' expecting {'VAR', 'LIT'}
            message = message.replaceAll("'|}$", "");
            ArrayList<String> tokens = StringUtils.splitToArrayList(message, " ");
//            System.out.println(tokens);
            if (message.contains("missing")) {
                // e.g: line 1:338 missing ENSTM{EXPR} at <EOF>
                candidates.add(tokens.get(3));
            } else {
                // e.g: line 1:327 mismatched input <EOF> expecting {VAR LIT}
                for(int i = 6; i < tokens.size(); ++i) {
                    candidates.add(tokens.get(i).replaceAll(",$|^\\{", ""));
                }
            }
            validTokens = getValidNextTokens(subStatement, candidates);
        }

        return validTokens;
    }

    private ArrayList<String> getValidNextTokens(String statement, ArrayList<String> candidates) {
        ArrayList<String> validTokens = new ArrayList<>();
        for (String candidate : candidates) {
            try {
                String newStatement = StringUtils.concat(statement, candidate);
                parseExcodeSequence(newStatement);
                validTokens.add(candidate);
            }
            catch (ParseCancellationException e) {
                if (!e.getMessage().contains("no viable"))
                    validTokens.add(candidate);
            }
        }
        return validTokens;
    }

    private void parseExcodeSequence(String statement) throws ParseCancellationException{
        ExcodeLexer lexer = new ExcodeLexer(CharStreams.fromString(statement));
        lexer.removeErrorListeners();
        lexer.addErrorListener(ThrowingErrorListener.INSTANCE);
        ExcodeParser parser = new ExcodeParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(ThrowingErrorListener.INSTANCE);
        ParseTree tree = parser.blockStatement();  // start rule
    }

    private ArrayList<String> rank(ArrayList<String> exSeq, ArrayList<String> validTokens) {
        ArrayList<String> topCandidates = new ArrayList<>();
        ArrayList<Pair<Double, String>> scores = new ArrayList<>();

        for (String nextToken: validTokens) {
            String modifiedToken = modifyToken(nextToken);
            List<String> sentence = new ArrayList<>(exSeq);
            sentence.add(modifiedToken);
            double score = 1;
//            double score = excodeModel.scoreSentence(sentence);
            scores.add(new Pair<>(score, modifiedToken));
        }

        // sort in descending order of score
        try {
            scores.sort((o1, o2) -> {
                if (o1.getFirst().equals(o2.getFirst())) {
                    return 0;
                } else if (o1.getFirst() > o2.getFirst()){
                    return -1;
                } else {
                    return 1;
                }
            });
        } catch (Exception e){
//            System.out.println(scores);
            e.printStackTrace();
        }

        for(int i = 0; i < Math.min(threshPerDepth, scores.size()); ++i) {
            topCandidates.add(scores.get(i).getSecond());
        }
        System.out.println(topCandidates);
        return topCandidates;
    }

    private String modifyToken(String token) {
        String result;
        switch (token) {
            case "TYPE":
                result = "TYPE(int)";
                break;
            case "VAR":
                result = "VAR(int,instance)";
                break;
            case "LIT":
                result = "LIT(int)";
                break;
            case "M_ACCESS":
                result = "M_ACCESS(String,getBytes,1)";
                break;
            case "F_ACCESS":
                result = "F_ACCESS(MyClass,instance)";
                break;
            case "C_CALL":
                result = "C_CALL(Text,Text)";
                break;
            default:
                result = token;
                break;
        }
        return result;
    }

    public void parseExcodeSequence(String content, FileInfo fileInfo, BufferedWriter bw) throws IOException {
        try{
            ExcodeLexer lexer = new ExcodeLexer(CharStreams.fromString(content));
            lexer.removeErrorListeners();
            lexer.addErrorListener(ThrowingErrorListener.INSTANCE);
            ExcodeParser parser = new ExcodeParser(new CommonTokenStream(lexer));
            parser.removeErrorListeners();
            parser.addErrorListener(ThrowingErrorListener.INSTANCE);
            ParseTree tree = parser.compilationUnit();  // start rule
        } catch (ParseCancellationException e) {
            bw.write(fileInfo.filePath + e.getMessage() + "\n");
            bw.newLine();
        }
    }

    public HashMap<SystemTableCrossProject, String> getSystemTableCrossProjectMap() {
        return this.systemTableCrossProjectMap;
    }

    public static void main(String[] args) {
        Parser parser = new Parser();
        parser.run();
    }
}