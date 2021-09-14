package flute.jdtparser;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import flute.config.Config;
import flute.data.typemodel.Member;
import flute.data.typemodel.ClassModel;
import flute.data.type.TypeConstraintKey;
import flute.jdtparser.callsequence.node.cfg.Utils;
import flute.utils.Pair;
import flute.utils.ProgressBar;
import flute.utils.logging.Timer;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;
import flute.utils.file_processing.DirProcessor;
import flute.utils.file_processing.FileProcessor;
import flute.utils.parsing.CommonUtils;

import java.io.*;
import java.lang.reflect.Type;
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
    private List<PublicStaticMember> publicStaticFieldList;
    private List<PublicStaticMember> publicStaticMethodList;

    public ProjectParser(String projectDir, String[] sourcePaths, String[] encodeSources, String[] classPaths, int jdtLevel, String javaVersion) {
        this.projectDir = projectDir;
        this.sourcePaths = sourcePaths;
        this.classPaths = classPaths;
        this.encodeSources = encodeSources;
        this.jdtLevel = jdtLevel;
        this.javaVersion = javaVersion;
        this.publicStaticFieldList = new ArrayList<>();
        this.publicStaticMethodList = new ArrayList<>();
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
            if (!file.getAbsolutePath().contains("src")) return false;

            for (String blackName : Config.BLACKLIST_NAME_SRC) {
                if (file.getAbsolutePath().contains(blackName)) return false;
            }

            return true;
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

        ProgressBar progressBar = new ProgressBar();

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
            progressBar.setProgress((float) fileCount / javaFiles.size(), true);
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
//            System.out.println(file.getAbsolutePath());
            CompilationUnit cu = createCU(file);
            // Now binding is activated. Do something else
            cu.accept(new TypeVisitor(this));
        }
    }

    public void addStaticMember(TypeDeclaration type, String hierachy, String packageName) {
        if (type.resolveBinding() == null) return;
        ClassParser classParser = new ClassParser(type.resolveBinding());
        String className = hierachy + type.getName();
        String nextHierachy = className + ".";
        List<IVariableBinding> fields = classParser.getPublicStaticFields();
        fields.forEach(field -> {
            String excode = String.format("VAR(%s) F_ACCESS(%s,%s)", className, className, field.getName());
            String lex = nextHierachy + field.getName();
            publicStaticFieldList.add(new PublicStaticMember(field.getType().getKey(), excode, lex, packageName));
        });
        List<IMethodBinding> methods = classParser.getPublicStaticMethods();
        methods.forEach(method -> {
            String excode = String.format("VAR(%s) M_ACCESS(%s,%s,%s) OPEN_PART",
                    className, className, method.getName(), method.getParameterTypes().length);
            String lex = nextHierachy + method.getName() + "(";
            publicStaticMethodList.add(new PublicStaticMember(method.getReturnType().getKey(), excode, lex, packageName));
        });

        TypeDeclaration[] inner = type.getTypes();
        for (TypeDeclaration t : inner) {
            addStaticMember(t, nextHierachy, packageName);
        }
    }

    public List<PublicStaticMember> getPublicStaticFieldList() {
        return publicStaticFieldList;
    }

    public List<PublicStaticMember> getPublicStaticMethodList() {
        return publicStaticMethodList;
    }

    public void initPublicStaticMembers() {
        File publicStaticMembersFile = new File(String.format(Config.PUBLIC_STATIC_MEMBER_PATH, Config.PROJECT_NAME));
        if (publicStaticMembersFile.isFile()) return;

        List<File> javaFiles = DirProcessor.walkJavaFile(Config.PROJECT_DIR);

        javaFiles = javaFiles.stream().filter(file -> {
            return !Utils.checkTestFile(file);
        }).collect(Collectors.toList());

        for (File file : javaFiles) {
            File curFile = new File(file.getAbsolutePath());
            FileParser fileParser = new FileParser(this, curFile, 6969669);
            List<?> types = fileParser.getCu().types();
            String packageName = Config.PROJECT_NAME.startsWith("rt") ? null
                    : fileParser.getCu().getPackage() == null ? null : fileParser.getCu().getPackage().getName().getFullyQualifiedName();
            for (Object type : types) {
                if (type instanceof TypeDeclaration) {
                    addStaticMember((TypeDeclaration) type, "", packageName);
                }
            }
        }
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(
                    String.format(Config.PUBLIC_STATIC_MEMBER_PATH, Config.PROJECT_NAME)));
            Gson gson = new Gson();
            bw.write(gson.toJson(getPublicStaticFieldList()));
            bw.newLine();
            bw.write(gson.toJson(getPublicStaticMethodList()));
            bw.newLine();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadPublicStaticMembers() {
        loadPublicStaticMembers(Config.PROJECT_NAME);
    }

    public void loadPublicStaticRTMembers() {
        loadPublicStaticMembers("rt");
    }

    public void loadPublicStaticMembers(String project) {
        try {
            Gson gson = new Gson();
            BufferedReader br = new BufferedReader(new FileReader(
                    String.format(Config.PUBLIC_STATIC_MEMBER_PATH, project)));
            Type publicStaticMemberType = new TypeToken<List<PublicStaticMember>>() {
            }.getType();
            publicStaticFieldList.addAll(gson.fromJson(br.readLine(), publicStaticMemberType));
            publicStaticMethodList.addAll(gson.fromJson(br.readLine(), publicStaticMemberType));
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<PublicStaticMember> getPublicStaticCandidates(String typeKey) {
        if (typeKey == null) return new ArrayList<PublicStaticMember>();
        List<PublicStaticMember> publicStaticMemberList = new ArrayList<>(publicStaticFieldList);
        publicStaticMemberList.addAll(publicStaticMethodList);
        return publicStaticMemberList.stream().filter(member -> {
            return TypeConstraintKey.assignWith(member.key, typeKey);
        }).collect(Collectors.toList());
    }

    private HashMap<String, List<Pair<String, String>>> publicStaticMemberHM;
    private List<Pair<String, String>> publicStaticMemberPairList = new ArrayList<>();


    public void initPublicStaticMemberHM() {
        publicStaticMemberHM = new HashMap<>();
        List<PublicStaticMember> publicStaticMemberList = new ArrayList<>(publicStaticFieldList);
        publicStaticMemberList.addAll(publicStaticMethodList);

        for (PublicStaticMember member : publicStaticMemberList) {
            if (publicStaticMemberHM.get(member.key) != null) {
                publicStaticMemberHM.get(member.key).add(new Pair<>(member.excode, member.lexical));
            } else {
                publicStaticMemberHM.put(member.key, new ArrayList<>());
                publicStaticMemberHM.get(member.key).add(new Pair<>(member.excode, member.lexical));
            }
            publicStaticMemberPairList.add(new Pair<>(member.excode, member.lexical));
        }
    }

    public List<Pair<String, String>> getFasterPublicStaticCandidates(String typeKey) {
        if (publicStaticMemberHM == null) initPublicStaticMemberHM();
        List<Pair<String, String>> result = new ArrayList<>();

        if (typeKey == null) return result;
        if (typeKey.equals(TypeConstraintKey.OBJECT_TYPE)) {
            return publicStaticMemberPairList;
        }

        TypeConstraintKey.assignWith(typeKey).forEach(type -> {
            if (publicStaticMemberHM.get(type) != null)
                result.addAll(publicStaticMemberHM.get(type));
        });

        return result;
    }

    public static void main(String[] args) throws IOException {
        String project = args[0];
        Timer timer = new Timer();
        System.out.println("Starting parse...");

        //auto load src and .jar file
        Config.loadConfig(String.format("docs/json/%s.json", project));
        System.out.print("Auto load binding time: ");
        System.out.printf("%.5fs\n", timer.getTimeCounter() / 1000.0);

        //gen and parse project
        ProjectParser projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
                Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);
        projectParser.initPublicStaticMembers();
//        projectParser.loadPublicStaticMembers();

        System.out.println("Project parse time: ");
        System.out.printf("%.5fs\n", timer.getTimeCounter() / 1000.0);
    }
}