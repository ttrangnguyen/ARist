package flute.jdtparser;

import flute.data.Member;
import flute.data.ClassModel;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import flute.utils.file_processing.DirProcessor;
import flute.utils.file_processing.FileProcessor;
import flute.utils.parsing.CommonUtils;

import java.io.File;
import java.util.*;

public class ProjectParser {
    public HashMap<String, ClassModel> classListModel = new HashMap<>();

    private String projectDir;
    private String[] sourcePaths;
    private String[] encodeSources;
    private String[] classPaths;
    private int jdtLevel = 13;
    private String javaVersion = "13";
    ASTParser parser = ASTParser.newParser(jdtLevel); //choose source code analyzing strategy

    public ProjectParser(String projectDir, String[] sourcePaths, String[] encodeSources, String[] classPaths, int jdtLevel, String javaVersion) {
        this.projectDir = projectDir;
        this.sourcePaths = sourcePaths;
        this.classPaths = classPaths;
        this.encodeSources = encodeSources;
        this.jdtLevel = jdtLevel;
        this.javaVersion = javaVersion;

        parser.setResolveBindings(true); // turn on binding strategy
        parser.setKind(ASTParser.K_COMPILATION_UNIT);// the source code is a file .java
        parser.setBindingsRecovery(true);
        parser.setStatementsRecovery(true);
        Hashtable<String, String> options = JavaCore.getOptions();

        JavaCore.setComplianceOptions(javaVersion, options);

        parser.setCompilerOptions(options);
        parser.setEnvironment(classPaths, sourcePaths, encodeSources, true);
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
        parser.setUnitName(file.getName());
        parser.setSource(FileProcessor.read(file).toCharArray());
        CompilationUnit cu = (CompilationUnit) parser.createAST(new NullProgressMonitor());
        return cu;
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