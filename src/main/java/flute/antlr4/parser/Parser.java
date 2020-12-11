package flute.antlr4.parser;

import flute.antlr4.listener.ThrowingErrorListener;
import flute.antlr4.config.Config;
import flute.tokenizing.exe.GetDirStructureCrossProject;
import flute.tokenizing.parsing.JavaFileParser;
import flute.tokenizing.excode_data.FileInfo;
import flute.utils.file_processing.*;
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
import flute.jdtparser.FileParser;
import flute.jdtparser.ProjectParser;
import org.eclipse.core.internal.resources.Project;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
// eclipse stops = [0, 23000, 28000, 35000, 42000, 46177]
// netbeans stops = [0, 5000, 9759]

public class Parser {
    public static int parseBegin = 0;
    public static int parseEnd = 23000;
    public static final boolean CREATE_DATA_PATH = false;
    public static final int recursionMaxDepth = 1;
    public static final int threshPerDepth = 7;

    private final String projectName;
    private final String projectSrcPath;
    private final String javaTrainPath, javaTestPath, javaValidatePath;
    private final String excodeTrainPath, excodeTestPath, excodeValidatePath;
    private final ProjectParser projectParser;
    private HashSet<String> validateFilesPath;
    private HashSet<String> testFilesPath;

    private SystemTableCrossProject systemTableCrossProject;
    private int LOC;

    public Parser(String projectName, String srcPath, int parseBegin, int parseEnd) {
        this.projectName = projectName;
        this.projectSrcPath = projectName + srcPath;
        Parser.parseBegin = parseBegin;
        Parser.parseEnd = parseEnd;
        systemTableCrossProject = new SystemTableCrossProject();
        DirProcessor.createDirectoryIfNotExists(Config.excodeTrainDataPath);
        DirProcessor.createDirectoryIfNotExists(Config.excodeValidateDataPath);
        DirProcessor.createDirectoryIfNotExists(Config.excodeTestDataPath);
        DirProcessor.createDirectoryIfNotExists(Config.javaTrainDataPath);
        DirProcessor.createDirectoryIfNotExists(Config.javaValidateDataPath);
        DirProcessor.createDirectoryIfNotExists(Config.javaTestDataPath);
        DirProcessor.createDirectoryIfNotExists(Config.trainFilesPath);
        DirProcessor.createDirectoryIfNotExists(Config.validateFilesPath);
        DirProcessor.createDirectoryIfNotExists(Config.testFilesPath);

        javaTrainPath = DirProcessor.createDirectoryIfNotExists(Config.javaTrainDataPath, projectName);
        javaTestPath = DirProcessor.createDirectoryIfNotExists(Config.javaTestDataPath, projectName);
        javaValidatePath = DirProcessor.createDirectoryIfNotExists(Config.javaValidateDataPath, projectName);
        excodeTrainPath = DirProcessor.createDirectoryIfNotExists(Config.excodeTrainDataPath, projectName);
        excodeTestPath = DirProcessor.createDirectoryIfNotExists(Config.excodeTestDataPath, projectName);
        excodeValidatePath = DirProcessor.createDirectoryIfNotExists(Config.excodeValidateDataPath, projectName);
        validateFilesPath = new HashSet<>();
        testFilesPath = new HashSet<>();

        try {
            flute.config.Config.loadConfig(flute.config.Config.STORAGE_DIR + "/json/" + projectName + ".json");
        } catch (Exception e) {
            e.printStackTrace();
        }

        projectParser = new ProjectParser(flute.config.Config.PROJECT_DIR, flute.config.Config.SOURCE_PATH,
                flute.config.Config.ENCODE_SOURCE, flute.config.Config.CLASS_PATH, flute.config.Config.JDT_LEVEL, flute.config.Config.JAVA_VERSION);
    }

    public void run() {
        translateJavaToExcode();
//        checkExcodeGrammar();
        createExcodeFiles();
    }

    private void translateJavaToExcode() {
        Logger.initDebug("debugVisitor.txt");
        List<File> allSubFilesTmp = DirProcessor.walkJavaFile(Config.projectsPath + projectSrcPath);
        int numFiles = allSubFilesTmp.size();
        int fileCount = 0;
        Logger.log("allSubFiles size: " + numFiles);
        MetricsVisitor visitor = new MetricsVisitor();
        systemTableCrossProject = new SystemTableCrossProject();
        for (File file : allSubFilesTmp) {
            fileCount++;
            if (fileCount-1 < parseBegin || fileCount-1 > parseEnd)
                continue;
            if (!canTest(file, projectName)) continue;
            int thisFileLOC = CountLOC.count(file);
            LOC += thisFileLOC;
//            System.out.println(file.getAbsolutePath());
            JavaFileParser.visitFile(visitor, file, systemTableCrossProject, "xxx/");
        }
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

    //special method for netbeans and eclipse only
    private boolean canTest(File file, String projectName) {
        if (projectName.equals("netbeans") || projectName.equals("eclipse")) {
            return file.getAbsolutePath().contains("src")
                    && !file.getAbsolutePath().contains("examples")
                    && !file.getAbsolutePath().contains("test")
                    && !file.getAbsolutePath().contains("demo");
        }
        return true;
    }

    private void checkExcodeGrammar() {
        try {
            checkExcodeGrammar(systemTableCrossProject, projectName);
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
            if (CREATE_DATA_PATH) createDataFilesPath();
            createDataFiles();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean trainTestSplited(String projectName) {
        File testPath = new File(Config.testFilesPath + projectName + ".txt");
        return testPath.exists();
    }


    private void createDataFilesPath() throws IOException {
        FileWriter trainFilesPath = new FileWriter(new File(Config.trainFilesPath + projectName + ".txt"), true);
        FileWriter validateFilesPath = new FileWriter(new File(Config.validateFilesPath + projectName + ".txt"), true);
        FileWriter testFilesPath = new FileWriter(new File(Config.testFilesPath + projectName + ".txt"), true);

        int testLOCThresh = (int) (LOC * 0.1);
        int validateLOCThresh = (int) (LOC * 0.9 * 0.15);
        int trainLOCThresh = LOC - testLOCThresh - validateLOCThresh;
        int currentTestLOC = 0;
        int currentValidateLOC = 0;
        int currentTrainLOC = 0;

        for (FileInfo fileInfo : systemTableCrossProject.fileList) {
            StringBuilder builder = new StringBuilder();
            for (NodeSequenceInfo node : fileInfo.getNodeSequenceList()) {
                String space = " ";
                builder.append(node.toString().replace(" ", "").replace("\r\n", space));
            }

            while (true) {
                final String absolutePath = fileInfo.file.getAbsolutePath();
                final String relativePath = absolutePath.substring(absolutePath.indexOf(projectName)) + "\n";
                if (toTest() && currentTestLOC <= testLOCThresh) {
                    testFilesPath.write(relativePath);
                    currentTestLOC += CountLOC.count(fileInfo.file);
                    break;
                } else if (toValidate() && currentValidateLOC <= validateLOCThresh){
                    validateFilesPath.write(relativePath);
                    currentValidateLOC += CountLOC.count((fileInfo.file));
                    break;
                } else if (currentTrainLOC <= trainLOCThresh){
                    trainFilesPath.write(relativePath);
                    currentTrainLOC += CountLOC.count((fileInfo.file));
                    break;
                }
            }
        }
        trainFilesPath.close();
        validateFilesPath.close();
        testFilesPath.close();
    }

    private void createDataFiles() throws Exception {
        validateFilesPath = FileProcessor.readLineByLine(Config.validateFilesPath + projectName + ".txt");
        testFilesPath = FileProcessor.readLineByLine(Config.testFilesPath + projectName + ".txt");
        for (FileInfo fileInfo : systemTableCrossProject.fileList) {
            String[] filePath = fileInfo.filePath.split(Pattern.quote(File.separator));
            String excodeFilePath;
            String javaFileTokenPath;

            final String absolutePath = fileInfo.file.getAbsolutePath();
            final String relativePath = absolutePath.substring(absolutePath.indexOf(projectName));
            if (testFilesPath.contains(relativePath)) {
                javaFileTokenPath = javaTestPath +
                        fileInfo.file.getName().replace(".java", ".txt");
                excodeFilePath = excodeTestPath +
                        filePath[filePath.length - 1].replace(".java", ".txt");
            } else if (validateFilesPath.contains(relativePath)){
                javaFileTokenPath = javaValidatePath +
                        fileInfo.file.getName().replace(".java", ".txt");
                excodeFilePath = excodeValidatePath +
                        filePath[filePath.length - 1].replace(".java", ".txt");
            } else {
                javaFileTokenPath = javaTrainPath +
                        fileInfo.file.getName().replace(".java", ".txt");
                excodeFilePath = excodeTrainPath +
                        filePath[filePath.length - 1].replace(".java", ".txt");
            }

//            createExcodeFile(fileInfo, excodeFilePath);
            createJavaFile(absolutePath, javaFileTokenPath);
        }
    }

    private void createExcodeFile(FileInfo fileInfo, String excodeFilePath) throws IOException {
        StringBuilder builder = new StringBuilder();
        for (NodeSequenceInfo node : fileInfo.getNodeSequenceList()) {
            String space = " ";
            builder.append(node.toString().replace(" ", "").replace("\r\n", space));
        }

        File fout = new File(excodeFilePath);
        FileOutputStream fos = new FileOutputStream(fout);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        bw.write(builder.toString());
        bw.close();
    }

    private void createJavaFile(String absolutePath, String javaFileTokenPath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(absolutePath));
        String currentMethodName;
        String currentClassName;
        boolean insideMethod = false;
        boolean insideClass = false;
        StringBuilder fileContentBuilder = new StringBuilder();
        int curLineNum = 0;
        File curFile = new File(absolutePath);
        FileParser fileParser = new FileParser(projectParser, curFile, flute.config.Config.TEST_POSITION);
        for (String line = reader.readLine(); line!=null; line=reader.readLine()) {
//            FileParser fileParser = new FileParser(projectParser, curFile, flute.config.Config.TEST_POSITION);
            ++curLineNum;
            if (line.equals("")) continue;
            try {
                fileParser.setPosition(curLineNum, 1);
            } catch (Exception e) {
//                e.printStackTrace();
//                System.out.println(curLineNum);  // always exception
                insideMethod = false;
                continue;
            }
            Optional<String> classScopeName = fileParser.getCurClassScopeName();
            Optional<String> methodScopeName = fileParser.getCurMethodScopeName();
            if (classScopeName.isPresent()) {
                if (!insideClass) {
                    insideClass = true;
                    currentClassName = classScopeName.get();
                    fileContentBuilder.append("`").append(currentClassName);
                }
            } else {
                insideClass = false;
            }
            if (methodScopeName.isPresent() && fileParser.checkInsideMethod()) {
                if (!insideMethod) {
                    insideMethod = true;
                    currentMethodName = methodScopeName.get();
                    fileContentBuilder.append("#").append(currentMethodName);
                }
                fileContentBuilder.append(line).append("\n");
            } else if (insideMethod){
                fileContentBuilder.append("#");
                insideMethod = false;
            }
        }
        FileWriter writer = new FileWriter(javaFileTokenPath);
        String fileContent = fileContentBuilder.toString().replaceAll("[a-zA-Z0-9_]+.class", ".class")
                .replaceAll("\\[.*?]", "[]");
        ArrayList<String> tokens = JavaTokenizer.tokenize(fileContent);
        for (String token : tokens) {
            writer.write(token + "\n");
        }
        writer.close();
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

    private ArrayList<ArrayList<String>> expandExcodeSeq(ArrayList<String> exSeq, String lastExcodeStatement, int depth) {
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

    public static void main(String[] args) {
        // eclipse stops = [0, 23000, 28000, 35000, 42000, 46177]
        // netbeans stops = [0, 5000, 9759]
//        Parser parser = new Parser("eclipse", "", 0, 23000);
//        parser.run();
//        parser = new Parser("eclipse", "", 23001, 28000);
//        parser.run();
//        Parser parser = new Parser("eclipse", "", 28001, 35000);
//        parser.run();
//        parser = new Parser("eclipse", "", 35001, 42000);
//        parser.run();
//        Parser parser = new Parser("eclipse", "", 42001, 46177);
//        parser.run();
//        Parser parser = new Parser("netbeans", "/ide", 0, 5000);
//        parser.run();
        Parser parser = new Parser("netbeans", "/ide", 5001, 9759);
        parser.run();


        //File[] projects = new File(Config.projectsPath).listFiles(File::isDirectory);
//        ProjectParser projectParser = new ProjectParser(flute.config.Config.PROJECT_DIR, flute.config.Config.SOURCE_PATH,
//                flute.config.Config.ENCODE_SOURCE, flute.config.Config.CLASS_PATH, flute.config.Config.JDT_LEVEL, flute.config.Config.JAVA_VERSION);
//        File curFile = new File("D:\\Research\\Flute\\storage\\repositories\\git\\eclipse\\eclipse.pde.ui\\ui\\org.eclipse.pde.core\\src\\org\\eclipse\\pde\\internal\\core\\plugin\\AbbreviatedPluginHandler.java");
////        File curFile = new File("D:\\Research\\Flute\\storage\\repositories\\git\\netbeans\\ide\\html\\src\\org\\netbeans\\modules\\html\\palette\\items\\A.java");
//        FileParser fileParser = new FileParser(projectParser, curFile, flute.config.Config.TEST_POSITION);
//        try {
//            fileParser.setPosition(38, 0);
//        } catch (Exception e) {
////                e.printStackTrace();
////                System.out.println(curLineNum);  // always exception
//        }
//        Optional<String> methodScopeName = fileParser.getCurMethodScopeName();
//        if (methodScopeName.isPresent()) {
//            System.out.println("DMM");
//        }
//        Parser parser = new Parser("netbeans", "/ide");
//        parser.run();

        //            if (project.getName().equals("eclipse-platform-sources-4.17")) {
//                srcPath = "";
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
    }
}