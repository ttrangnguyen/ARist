package flute.jdtparser;

import flute.config.Config;
import flute.data.typemodel.Member;
import flute.data.typemodel.ClassModel;
import flute.utils.ProgressBar;
import flute.utils.logging.Timer;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;
import flute.utils.file_processing.DirProcessor;
import flute.utils.file_processing.FileProcessor;
import flute.utils.parsing.CommonUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class ProjectParser {
    public HashMap<String, ClassModel> classListModel = new HashMap<>();

    private String projectDir;
    private String[] sourcePaths;
    private String[] encodeSources;
    private String[] classPaths;
    private int jdtLevel = 13;
    private String javaVersion = "13";

    public ProjectParser(String projectDir, String[] sourcePaths, String[] encodeSources, String[] classPaths, int jdtLevel, String javaVersion) {
        this.projectDir = projectDir;
        this.sourcePaths = sourcePaths;
        this.classPaths = classPaths;
        this.encodeSources = encodeSources;
        this.jdtLevel = jdtLevel;
        this.javaVersion = javaVersion;
    }

    public HashMap<String, ClassModel> getListAccess(ITypeBinding clazz) {
        HashMap<String, ClassModel> listAccess = new HashMap<>();
        for (Map.Entry<String, ClassModel> entry : classListModel.entrySet()) {
            String key = entry.getKey();
            ClassModel classModel = entry.getValue();

            if (clazz == classModel.getOrgType()) {
                listAccess.put(clazz.getKey(), classModel.clone());
                continue;
            } else {
                boolean extended = clazz.isSubTypeCompatible(classModel.getOrgType());

                String fromPackage = clazz.getPackage().getName();
                String toPackage = "-1";
                if (classModel.getOrgType().getPackage() != null) {
                    toPackage = classModel.getOrgType().getPackage().getName();
                }

                if (CommonUtils.checkVisibleMember(classModel.getOrgType().getModifiers(), fromPackage, toPackage, extended)) {
                    ClassModel cloneModel = classModel.clone();
                    listAccess.put(key, cloneModel);
                    List<Member> toRemoves = new ArrayList<>();
                    String finalToPackage = toPackage;
                    classModel.getMembers().forEach(member -> {
                        if (!CommonUtils.checkVisibleMember(member.getMember().getModifiers(), fromPackage, finalToPackage, extended)) {
                            toRemoves.add(member);
                        }
                    });
                    cloneModel.getMembers().removeAll(toRemoves);
                }
            }

        }
        return listAccess;
    }

    public void parseClass(ITypeBinding iTypeBinding) {
        if (iTypeBinding == null) return;
        if (iTypeBinding.isPrimitive()) return;
        ClassModel classModel = new ClassModel(iTypeBinding);

        classListModel.put(classModel.getOrgType().getKey(), classModel);
    }

    public CompilationUnit createCU(File file) {
        return createCU(file.getName(), FileProcessor.read(file));
    }

    public CompilationUnit createCU(String fileName, String fileData) {
        ASTParser parser = ASTParser.newParser(jdtLevel); //choose source code analyzing strategy

        parser.setResolveBindings(true); // turn on binding strategy
        parser.setKind(ASTParser.K_COMPILATION_UNIT);// the source code is a file .java
        parser.setBindingsRecovery(true);
        parser.setStatementsRecovery(true);
        Hashtable<String, String> options = JavaCore.getOptions();

        JavaCore.setComplianceOptions(javaVersion, options);

        parser.setCompilerOptions(options);
        parser.setEnvironment(classPaths, sourcePaths, encodeSources, true);

        parser.setUnitName(fileName);
        parser.setSource(fileData.toCharArray());
        CompilationUnit cu = (CompilationUnit) parser.createAST(new NullProgressMonitor());
        return cu;
    }

    public void bindingTest() {
        List<File> allJavaFiles = DirProcessor.walkJavaFile(projectDir);
        int problemCount = 0;
        int bindingProblemCount = 0;
        int fileBindingErrorCount = 0;

        int fileCount = 0;
        float percent = -1;
        float oldPercent = -1;

        List<File> javaFiles = allJavaFiles.stream().filter(file -> {
            return file.getAbsolutePath().contains("src")
                    && !file.getAbsolutePath().contains("examples")
                    && !file.getAbsolutePath().contains("test")
                    && !file.getAbsolutePath().contains("demo");
        }).collect(Collectors.toList());

        System.out.println("===============SOURCE PATHS===============");
        for (String sourcePath : sourcePaths) {
            System.out.println(sourcePath);
        }
        System.out.println("===============CLASS PATHS===============");
        for (String classPath : classPaths) {
            System.out.println(classPath);
        }
        System.out.println("=========================================");

        System.out.println("Number of source paths: " + sourcePaths.length);
        System.out.println("Number of jar files: " + classPaths.length);
        System.out.println("===============START BINDING==============");


        Timer timer = new Timer();
        timer.startCounter();

        for (File file : javaFiles) {
            CompilationUnit cu = createCU(file);
            boolean isBindingErrorFile = false;
            for (IProblem problem :
                    cu.getProblems()) {
                if (problem.isError()) {
                    problemCount++;
                    System.out.println(problem);

                    if (problem.toString().indexOf("cannot be resolved") != -1) {
                        bindingProblemCount++;
                        isBindingErrorFile = true;
                    }
                }
            }

            if (isBindingErrorFile) {
                fileBindingErrorCount++;
                System.out.println("ERROR BINDING FILE: " + file.getAbsolutePath());
            }

            fileCount++;
            percent = (float) fileCount / javaFiles.size();
            if (percent - oldPercent > Config.PRINT_PROGRESS_DELTA) {
                System.out.printf("%05.2f", percent * 100);
                System.out.print("% ");
                ProgressBar.printProcessBar(percent * 100, Config.PROGRESS_SIZE);
                System.out.printf(" - %" + String.valueOf(javaFiles.size()).length() + "d/" + javaFiles.size() + " files ", fileCount);
                long runTime = timer.getCurrentTime().getTime() - timer.getLastTime().getTime();
                System.out.println("- ETA: " + Timer.formatTime(((long) (runTime / percent) - runTime) / 1000));
                oldPercent = percent;
            }
        }

        System.out.println("Number of java file: " + javaFiles.size());
        System.out.println("Number of problem: " + problemCount);
        System.out.println("Number of binding problem: " + bindingProblemCount);
        System.out.println("Number of file binding error: " + fileBindingErrorCount);
        System.out.printf("Binding pass: %.2f%%\n", (javaFiles.size() - fileBindingErrorCount) * 100f / javaFiles.size());
    }

    public void parse() {
        List<File> javaFiles = DirProcessor.walkJavaFile(projectDir);

        for (File file : javaFiles) {
            CompilationUnit cu = createCU(file);
            // Now binding is activated. Do something else
            cu.accept(new TypeVisitor(this));
        }
    }
}