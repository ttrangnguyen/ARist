package flute.analysis;

import flute.analysis.enumeration.ExpressionOrigin;
import flute.analysis.enumeration.ExpressionType;
import flute.analysis.structure.DataFrame;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javassistmodel.JavassistMethodDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import flute.analysis.config.Config;
import flute.analysis.structure.*;
import flute.utils.ProgressBar;
import flute.utils.file_processing.*;
import flute.utils.logging.Logger;
import flute.utils.logging.Timer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @deprecated use {@link flute.analysis.analysers.JavaAnalyser} instead.
 */
@Deprecated
public class JavaAnalyser {
    private List<File> projects;

    private DataFrame dataFrame = new DataFrame();

    private StringCounter methodCallCounter = new StringCounter();
    private StringCounter javaMethodCallCounter = new StringCounter();
    private StringCounter libMethodCallCounter = new StringCounter();

    public boolean doCountJavaFiles = false;
    public boolean doAnalyseMethodCalls = false;
    public boolean doAnalyseNumOfArgs = false;
    public boolean doAnalyseExpressionTypeOfParams = false;
    public boolean doAnalyseOriginOfArgs = false;
    public boolean doAnalyseUsingVariable = false;
    public boolean doAnalyseCasting = false;
    public boolean doLogClassName = false;
    public boolean doLogFieldName = false;
    public boolean doLogMethodName = false;

    public long analyseMethodCallsTime = 0;
    public long analyseNumOfArgsTime = 0;
    public long analyseExpressionTypeOfParamsTime = 0;
    public long analyseOriginOfArgsTime = 0;
    public long analyseUsingVariableTime = 0;
    public long analyseCastingTime = 0;

    public List<String> blackListFile = new ArrayList<>();

    private String outputDir = null;

    public JavaAnalyser(String repoDirectory) {
        this.projects = getProjects(repoDirectory);
    }

    public static List<File> getProjects(String directory) {
        List<File> dir = DirProcessor.walkData(directory);
        List<File> projects = new ArrayList<>();
        loopDir:
        for (File f : dir) {
            if (Config.BLACKLIST_FOLDER_SRC.contains(f.getName())) continue loopDir;
            projects.add(f);
        }
        return projects;
    }

    public void analyseAll(boolean doLogProgress) {
        int numOfProjects = projects.size();
        long startTime = System.nanoTime();
        for (int i = 0; i < numOfProjects; ++i) {
            analyseProject(projects.get(i));
            if (doLogProgress) {
                if ((i + 1) % ((numOfProjects - 1) / 100 + 1) == 0) {
                    float percent = (i + 1) * 100f / numOfProjects;
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("%05.2f%% ", percent));
                    sb.append(ProgressBar.genProgressBar(percent, Config.PROGRESS_SIZE));
                    sb.append(String.format("\nanalyseMethodCalls: %d s", analyseMethodCallsTime / 1000000000));
                    sb.append(String.format("\nanalyseNumOfArgs: %d s", analyseNumOfArgsTime / 1000000000));
                    sb.append(String.format("\nanalyseExpressionTypeOfParams: %d s", analyseExpressionTypeOfParamsTime / 1000000000));
                    sb.append(String.format("\nanalyseOriginOfArgs: %d s", analyseOriginOfArgsTime / 1000000000));
                    sb.append(String.format("\nanalyseUsingVariable: %d s", analyseUsingVariableTime / 1000000000));
                    sb.append(String.format("\nanalyseCasting: %d s", analyseCastingTime / 1000000000));
                    sb.append(String.format("\nTotal time: %d s", (System.nanoTime() - startTime) / 1000000000));
                    System.out.println(sb);
                }
            }
        }

        Logger.write("List of examined projects:");
        for (File f : projects) Logger.write('\t' + f.getName());
        Logger.write("");

        printStatistics();
    }

    public void analyseAll() {
        analyseAll(true);
    }

    public void analyseProject(File project) {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
        combinedTypeSolver.add(new JavaParserTypeSolver(project));

        List<File> rawJavaFiles = DirProcessor.walkJavaFile(project.getAbsolutePath());

        List<File> javaFiles = rawJavaFiles.stream().filter(javaFile -> {
            for (String bString : Config.BLACKLIST_NAME_SRC) {
                if (javaFile.getAbsolutePath().contains(bString)) return false;
            }
            for (String bFile : blackListFile) {
                if (javaFile.getAbsolutePath().endsWith(bFile)) return false;
            }
            return true;
        }).collect(Collectors.toList());
        if (doCountJavaFiles) dataFrame.insert("Num of Java files", javaFiles.size());

        Timer timer = new Timer();
        timer.startCounter();
        int fileCount = 0;
        float percent = 0, oldPercent = 0;

        for (File file : javaFiles) {
            percent = (float) fileCount / javaFiles.size();
            if (percent - oldPercent > flute.config.Config.PRINT_PROGRESS_DELTA) {
                System.out.printf("%05.2f", percent * 100);
                System.out.print("% ");
                ProgressBar.printProcessBar(percent * 100, flute.config.Config.PROGRESS_SIZE);
                System.out.printf(" - %" + String.valueOf(javaFiles.size()).length() + "d/" + javaFiles.size() + " files of folder [%s] "
                        , fileCount, project.getName());
                long runTime = timer.getCurrentTime().getTime() - timer.getLastTime().getTime();
                System.out.println("- ETA: " + Timer.formatTime(((long) (runTime / percent) - runTime) / 1000));
                oldPercent = percent;
            }
            System.out.println(file.getAbsolutePath());
            analyseFile(file);
            fileCount++;

        }
    }

    private void analyseFile(File file) {
        String data = FileProcessor.read(file);

        try {
            CompilationUnit cu = StaticJavaParser.parse(data);
            long currentTime = System.nanoTime();

            if (doAnalyseMethodCalls) {
                analyseMethodCalls(cu);
                analyseMethodCallsTime += System.nanoTime() - currentTime;
                currentTime = System.nanoTime();
            }
            if (doAnalyseNumOfArgs) {
                analyseNumOfArgs(cu);
                analyseNumOfArgsTime += System.nanoTime() - currentTime;
                currentTime = System.nanoTime();
            }
            if (doAnalyseExpressionTypeOfParams) {
                analyseExpressionTypeOfParams(cu);
                analyseExpressionTypeOfParamsTime += System.nanoTime() - currentTime;
                currentTime = System.nanoTime();
            }
            if (doAnalyseOriginOfArgs) {
                analyseOriginOfArgs(cu);
                analyseOriginOfArgsTime += System.nanoTime() - currentTime;
                currentTime = System.nanoTime();
            }
            if (doAnalyseUsingVariable) {
                analyseUsingVariable(cu);
                analyseUsingVariableTime += System.nanoTime() - currentTime;
                currentTime = System.nanoTime();
            }
            if (doAnalyseCasting) {
                analyseCasting(cu);
                analyseCastingTime += System.nanoTime() - currentTime;
                currentTime = System.nanoTime();
            }
            if (outputDir != null) {
                if (doLogClassName) logClassName(cu, outputDir + "class_names.txt");
                if (doLogFieldName) logFieldName(cu, outputDir + "field_names.txt");
                if (doLogMethodName) logMethodName(cu, outputDir + "method_names.txt");
            }
        } catch (ParseProblemException e) {
            //e.printStackTrace();
        }
    }

    private void printStatistics() {
        if (doCountJavaFiles) printStatistics(dataFrame, "Java file");

        if (doAnalyseMethodCalls) printStatisticsOnMethodCalls();
        if (doAnalyseNumOfArgs) printStatistics(dataFrame, "argument");
        if (doAnalyseExpressionTypeOfParams) printStatisticsOnExpressionTypeOfParams();
        if (doAnalyseOriginOfArgs) printStatisticsOnOriginOfArgs();
        if (doAnalyseUsingVariable) printStatisticsOnUsingVariable();
        if (doAnalyseCasting) printStatisticsOnCasting();
    }

    public static void printStatistics(DataFrame dataFrame, String aspect, String context) {
        String label = "Num of " + aspect + "s" + (context == null ? "" : " " + context);
        Logger.write(dataFrame.describe(label));
        Logger.write("");
        DataFrame.Variable variable = dataFrame.getVariable(label);
        int i = 0;
        int step = 1;
        int threshold = 10;
        while (true) {
            if (step == 1) {
                Logger.write(String.format("\t%12d%s%7d%s%5.2f%%", i, " " + aspect + "s: ",
                        variable.countValue(i), " - ",
                        variable.getProportionOfValue(i, true)));
            } else {
                Logger.write(String.format("\t%4d%s%4d%s%7d%s%5.2f%%", i - step + 1, " to ", i, " " + aspect + "s: ",
                        variable.countRange(i - step + 1, i), " - ",
                        variable.getProportionOfRange(i - step + 1, i, true)));
            }

            if (i > variable.getMax()) break;

            if (i == threshold) {
                step *= 10;
                threshold *= 10;
            }
            i += step;
        }
        Logger.write("");
    }

    public static void printStatistics(DataFrame dataFrame, String aspect) {
        printStatistics(dataFrame, aspect, null);
    }

    private void analyseMethodCalls(CompilationUnit cu) {
        try {
            cu.findAll(MethodCallExpr.class).forEach(methodCallExpr -> {
                try {
                    ResolvedMethodDeclaration resolve = methodCallExpr.resolve();
                    if (resolve instanceof ReflectionMethodDeclaration) {
                        methodCallCounter.add("Java Method Call");
                        javaMethodCallCounter.add(resolve.getQualifiedSignature());
                    } else if (resolve instanceof JavassistMethodDeclaration) {
                        methodCallCounter.add("Lib Method Call");
                        libMethodCallCounter.add(resolve.getQualifiedSignature());
                    } else {
                        methodCallCounter.add("Inner Method Call");
                    }

                    String callerPackageName = null;
                    String calleePackageName = resolve.getPackageName();
                    Node methodCaller = getAscendantTypeMethod(methodCallExpr);
                    if (methodCaller instanceof MethodDeclaration) {
                        ResolvedMethodDeclaration resolveCaller = ((MethodDeclaration) methodCaller).resolve();
                        try {
                            callerPackageName = resolveCaller.getPackageName();
                        } catch (Exception e) {
                            //System.out.println(methodCaller);
                        }
                    } else if (methodCaller instanceof ConstructorDeclaration) {
                        ResolvedConstructorDeclaration resolveCaller = ((ConstructorDeclaration) methodCaller).resolve();
                        callerPackageName = resolveCaller.getPackageName();
                    } else {
                        ClassOrInterfaceDeclaration caller = getAscendantTypeClass(methodCallExpr);
                        callerPackageName = caller.resolve().getPackageName();
                    }

                    if (calleePackageName.equals(callerPackageName)) {
                        dataFrame.getVariable("Caller & Callee in same package").insert(1);
                    } else {
                        dataFrame.getVariable("Caller & Callee in same package").insert(0);
                    }
                } catch (Exception e) {
                    methodCallCounter.add("Lib Method Call");
                } catch (StackOverflowError err) {
                    //System.out.println(file.getAbsolutePath());
                }
            });
        } catch (ParseProblemException e) {
            e.printStackTrace();
        }
    }

    private void printStatisticsOnMethodCalls() {
        Logger.write("Num of inner method calls: " + methodCallCounter.getCount("Inner Method Call"));
        Logger.write("Num of java method calls: " + methodCallCounter.getCount("Java Method Call"));
        Logger.write("Num of lib method calls: " + methodCallCounter.getCount("Lib Method Call"));
        Logger.write("");

        Logger.write("Top 10 most called Java APIs:");
        for (String api : javaMethodCallCounter.getTop(10)) {
            Logger.write('\t' + api);
        }
        Logger.write("");

        Logger.write("Top 10 most called external libs' APIs:");
        for (String api : libMethodCallCounter.getTop(10)) {
            Logger.write('\t' + api);
        }
        Logger.write("");

        Logger.write(String.format("%s%.2f%%\n", "Rating of inner methods being called: ",
                methodCallCounter.getProportion("Inner Method Call", true)));

        Logger.write(String.format("%s%.2f%%\n", "Rating of caller & callee being in the same package: ",
                dataFrame.getVariable("Caller & Callee in same package").getProportionOfValue(1, true)));
    }

    private void analyseNumOfArgs(CompilationUnit cu) {
        try {
            cu.findAll(MethodCallExpr.class).forEach(methodCallExpr -> {
                int numOfArg = methodCallExpr.getArguments().size();
                dataFrame.insert("Num of arguments", numOfArg);
                if (numOfArg > 10) {
                    //logMethodCall(methodCallExpr, file, "logHighNumOfArg.txt", true);
                }
            });
        } catch (ParseProblemException e) {
            e.printStackTrace();
        }
    }

    private void analyseExpressionTypeOfParams(CompilationUnit cu) {
        try {
            cu.findAll(MethodCallExpr.class).forEach(methodCallExpr -> {
                boolean methodCallLogged = false;
                for (Expression arg : methodCallExpr.getArguments()) {
                    dataFrame.insert("Expression type of param", ExpressionType.get(arg).ordinal());
                    if (ExpressionType.get(arg) == ExpressionType.STRING_LIT) {
                        if (!methodCallLogged) {
                            methodCallLogged = true;
                            //logMethodCall(methodCallExpr, file, "logStringArg.txt", false);
                        }
                    }
                }
            });
        } catch (ParseProblemException e) {
            e.printStackTrace();
        }
    }

    private void printStatisticsOnExpressionTypeOfParams() {
        Logger.write("Statistics on Expression types of params:");
        for (ExpressionType exprType : ExpressionType.values()) {
            //if (exprType == ExpressionType.OTHERS) break;
            Logger.write(String.format("\t%-14s%14.2f%%", exprType + ":", dataFrame.getVariable("Expression type of param").getProportionOfValue(exprType.ordinal(), true)));
        }
        Logger.write("");
    }

    private void analyseOriginOfArgs(CompilationUnit cu) {
        try {
            cu.findAll(MethodCallExpr.class).forEach(methodCallExpr -> {
                for (Expression arg : methodCallExpr.getArguments()) {
                    ExpressionOrigin origin = ExpressionOrigin.get(arg);
                    if (origin != null) dataFrame.insert("Origin of arg", ExpressionOrigin.get(arg).ordinal());
                }
            });
        } catch (ParseProblemException e) {
            e.printStackTrace();
        }
    }

    private void printStatisticsOnOriginOfArgs() {
        Logger.write("Statistics on origin of arguments:");
        for (ExpressionOrigin origin : ExpressionOrigin.values()) {
            Logger.write(String.format("\t%-14s%14.2f%%", origin + ":", dataFrame.getVariable("Origin of arg").getProportionOfValue(origin.ordinal(), true)));
        }
        Logger.write("");
    }

    private void analyseUsingVariable(CompilationUnit cu) {
        try {
            cu.findAll(VariableDeclarator.class).forEach(variableDeclarator -> {
                String variable = variableDeclarator.getNameAsString();
                int count = 0;
                Node scope = getScope(variableDeclarator);
                if (scope == null) {
                    System.out.println(variable);
                    System.out.println(variableDeclarator.getParentNode().get().getParentNode().get());
                    System.out.println();
                }
                for (SimpleName simpleName : scope.findAll(SimpleName.class)) {
                    if (simpleName.getIdentifier().equals(variable)) ++count;
                }
                dataFrame.insert("Num of variable uses", count - 1);

                int declarationIdx = scope.toString().indexOf(variableDeclarator.toString());
                declarationIdx = scope.toString().indexOf(variable, declarationIdx);
                int firstUseIdx = scope.toString().indexOf(variable, declarationIdx + 1);
                if (firstUseIdx < 0) return;
                int numOfLinesBeforeUse = 0;
                String tmp = scope.toString().substring(declarationIdx, firstUseIdx);
                while (tmp.indexOf('\n') >= 0) {
                    ++numOfLinesBeforeUse;
                    tmp = tmp.substring(tmp.indexOf('\n') + 1);
                }
                dataFrame.insert("Num of lines before first use", numOfLinesBeforeUse);
            });
        } catch (ParseProblemException e) {
            e.printStackTrace();
        }
    }

    private void printStatisticsOnUsingVariable() {
        printStatistics(dataFrame, "variable use");
        printStatistics(dataFrame, "line", "before first use");
    }

    private void analyseCasting(CompilationUnit cu) {
        try {
            cu.findAll(MethodCallExpr.class).forEach(methodCallExpr -> {
                methodCallExpr.getArguments().forEach(argument -> {
                    dataFrame.insert("Casting argument",
                            argument.findFirst(CastExpr.class).isPresent() ? 1 : 0);
                });
            });

            cu.findAll(AssignExpr.class).forEach(assignExpr -> {
                dataFrame.insert("Casting in Assignment",
                        assignExpr.findFirst(CastExpr.class).isPresent() ? 1 : 0);
            });

            cu.findAll(VariableDeclarationExpr.class).forEach(variableDeclarationExpr -> {
                dataFrame.insert("Casting in Variable Declaration",
                        variableDeclarationExpr.findFirst(CastExpr.class).isPresent() ? 1 : 0);

                variableDeclarationExpr.getVariables().forEach(variableDeclarator -> {
                    if (variableDeclarator.getInitializer().isPresent() && variableDeclarator.getInitializer().get().isCastExpr()) {
                        CastExpr castExpr = variableDeclarator.getInitializer().get().asCastExpr();
                        String variableDeclaratorType = variableDeclarator.getType().asString();
                        String castType = castExpr.getType().asString();
                        if (variableDeclaratorType.contains(castType) || castType.contains(variableDeclaratorType)) {
                            dataFrame.insert("Casting mismatched type", 0);
                        } else {
                            dataFrame.insert("Casting mismatched type", 1);
                            //System.out.println(variableDeclarator.getType() + " " + castExpr.getType());
                        }
                    }
                });
            });

            cu.findAll(ReturnStmt.class).forEach(returnStmt -> {
                if (!returnStmt.findFirst(AssignExpr.class).isPresent()) {
                    dataFrame.insert("Casting in Return Statement",
                            returnStmt.findFirst(CastExpr.class).isPresent() ? 1 : 0);
                }
            });
        } catch (ParseProblemException e) {
            e.printStackTrace();
        }
    }

    private void printStatisticsOnCasting() {
        Logger.write("Rate of casting argument: " + dataFrame.getVariable("Casting argument").getMean());
        Logger.write("Rate of casting in assignment: " + dataFrame.getVariable("Casting in Assignment").getMean());
        Logger.write("Rate of casting in variable declaration: " + dataFrame.getVariable("Casting in Variable Declaration").getMean());
        Logger.write("Rate of casting in return statement: " + dataFrame.getVariable("Casting in Return Statement").getMean());
        Logger.write("Rate of casting mismatched type: " + dataFrame.getVariable("Casting mismatched type").getMean());
        Logger.write("");
    }

    private void setOutputDir(String path) {
        File f = new File(Config.LOG_DIR + path);
        if (f.isDirectory()) {
            this.outputDir = path;
        } else throw new IllegalArgumentException("Invalid path: " + f.getAbsolutePath());
    }

    public static void logClassName(CompilationUnit cu, String output) {
        StringBuilder sb = new StringBuilder();
        try {
            cu.findAll(ObjectCreationExpr.class).forEach(objectCreationExpr -> {
                objectCreationExpr.getType().findAll(ClassOrInterfaceType.class).forEach(classOrInterfaceType -> {
                    sb.append(classOrInterfaceType.getNameAsString() + '\n');
                });
            });

            cu.findAll(MethodCallExpr.class).forEach(methodCallExpr -> {
                try {
                    ResolvedMethodDeclaration resolve = methodCallExpr.resolve();
                    String callerClass = resolve.getClassName();
                    while (callerClass.indexOf('.') >= 0) {
                        sb.append(callerClass.substring(0, callerClass.indexOf('.')) + '\n');
                        callerClass = callerClass.substring(callerClass.indexOf('.') + 1);
                    }
                    sb.append(callerClass + '\n');
                } catch (Exception e) {
                    //e.printStackTrace();
                } catch (StackOverflowError err) {
                    //System.out.println(file.getAbsolutePath());
                }
            });

            cu.findAll(FieldAccessExpr.class).forEach(fieldAccessExpr -> {
                try {
                    Expression scope = fieldAccessExpr.getScope();
                    if (scope instanceof NameExpr) {
                        ResolvedValueDeclaration resolve = ((NameExpr) scope).resolve();
                        sb.append(extractResolvedReferenceTypeSimpleName(resolve.getType()) + '\n');
                    } else if (scope instanceof MethodCallExpr) {
                        ResolvedMethodDeclaration resolve = ((MethodCallExpr) scope).resolve();
                        sb.append(extractResolvedReferenceTypeSimpleName(resolve.getReturnType()) + '\n');
                    } else if (scope instanceof FieldAccessExpr) {
                        ResolvedValueDeclaration resolve = ((FieldAccessExpr) scope).resolve();
                        sb.append(extractResolvedReferenceTypeSimpleName(resolve.getType()) + '\n');
                    } else if (scope instanceof ThisExpr) {
                        if (((ThisExpr) scope).getTypeName().isPresent()) {
                            sb.append(((ThisExpr) scope).getTypeName().get().getIdentifier() + '\n');
                        } else {
                            sb.append(getAscendantTypeClass(scope).getNameAsString() + '\n');
                        }
                    } else if (scope instanceof SuperExpr) {
                        sb.append(extractResolvedReferenceTypeSimpleName(scope.calculateResolvedType()) + '\n');
                    } else {
                        sb.append(extractResolvedReferenceTypeSimpleName(scope.calculateResolvedType()) + '\n');
                    }
                } catch (Exception e) {
                    //e.printStackTrace();
                } catch (StackOverflowError err) {
                    //System.out.println(file.getAbsolutePath());
                }
            });
        } catch (ParseProblemException e) {
            e.printStackTrace();
        }
        Logger.write(sb.toString(), output);
    }

    public static void logFieldName(CompilationUnit cu, String output) {
        StringBuilder sb = new StringBuilder();
        try {
            cu.findAll(FieldDeclaration.class).forEach(fieldDeclaration -> {
                fieldDeclaration.getVariables().forEach(variableDeclarator -> {
                    sb.append(variableDeclarator.getNameAsString() + '\n');
                });
            });

            cu.findAll(FieldAccessExpr.class).forEach(fieldAccessExpr -> {
                sb.append(fieldAccessExpr.getNameAsString() + '\n');
            });
        } catch (ParseProblemException e) {
            e.printStackTrace();
        }
        Logger.write(sb.toString(), output);
    }

    public static void logMethodName(CompilationUnit cu, String output) {
        StringBuilder sb = new StringBuilder();
        try {
            cu.findAll(MethodDeclaration.class).forEach(methodDeclaration -> {
                sb.append(methodDeclaration.getNameAsString() + '\n');
            });

            cu.findAll(MethodCallExpr.class).forEach(methodCallExpr -> {
                sb.append(methodCallExpr.getNameAsString() + '\n');
            });
        } catch (ParseProblemException e) {
            e.printStackTrace();
        }
        Logger.write(sb.toString(), output);
    }

    private static Node getScope(Node n) {
        while ((n != null)
                && (!(n instanceof SwitchStmt))
                && (!(n instanceof TryStmt)) && (!(n instanceof CatchClause))
                && (!(n instanceof IfStmt))
                && (!(n instanceof ForStmt)) && (!(n instanceof ForEachStmt))
                && (!(n instanceof WhileStmt)) && (!(n instanceof DoStmt))
                && (!(n instanceof BlockStmt))
                && (!(n instanceof SynchronizedStmt))
                && (!(n instanceof MethodDeclaration)) && (!(n instanceof ConstructorDeclaration))
                && (!(n instanceof ClassOrInterfaceDeclaration)) && (!(n instanceof EnumDeclaration)) && (!(n instanceof AnnotationDeclaration))
        ) {
            n = n.getParentNode().orElse(null);
        }
        return n;
    }

    private static Node getAscendantTypeMethod(Node n) {
        while ((n != null) && (!(n instanceof MethodDeclaration)) && (!(n instanceof ConstructorDeclaration))) {
            n = n.getParentNode().orElse(null);
        }
        return n;
    }

    private static ClassOrInterfaceDeclaration getAscendantTypeClass(Node n) {
        while ((n != null) && !(n instanceof ClassOrInterfaceDeclaration)) {
            n = n.getParentNode().orElse(null);
        }
        return (ClassOrInterfaceDeclaration) n;
    }

    private static String extractResolvedReferenceTypeSimpleName(ResolvedType resolvedType) {
        String qualifiedName = resolvedType.asReferenceType().getQualifiedName();
        return qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
    }

    private static void logMethodCall(MethodCallExpr methodCallExpr, File sourceFile, String output, Boolean showContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("File path: " + sourceFile.getAbsolutePath() + "\n");
        sb.append("Method call: \n");
        sb.append("\t" + methodCallExpr + "\n");
        if (showContext) {
            sb.append("Context: \n");
            sb.append(getAscendantTypeMethod(methodCallExpr) + "\n\n");
        }
        Logger.write(sb.toString(), output);
    }

    public static void main(String[] args) {
        //git clone some repo
        //GitCloner.bulkCloneRepo(CSVReader.randomSet("repository_state.csv", "\t", Config.NUM_REPO_LIMIT));
        String projectName = "netbeans";

        JavaAnalyser analyser = new JavaAnalyser(Config.REPO_DIR + "git/netbeans/ide/");

        analyser.blackListFile.addAll(
                Arrays.asList(FileProcessor.read(new File("docs/blackListFile/" + projectName + ".txt")).split("\n"))
        );

        analyser.doCountJavaFiles = true;
        analyser.doAnalyseMethodCalls = true;
        analyser.doAnalyseNumOfArgs = true;
        analyser.doAnalyseExpressionTypeOfParams = true;
        analyser.doAnalyseOriginOfArgs = true;
        analyser.doAnalyseUsingVariable = true;
        analyser.doAnalyseCasting = true;
        analyser.doLogClassName = false;
        analyser.doLogFieldName = false;
        analyser.doLogMethodName = false;
        analyser.setOutputDir("../../src/main/python/name_stat/input/");
        analyser.analyseAll();
    }
}
