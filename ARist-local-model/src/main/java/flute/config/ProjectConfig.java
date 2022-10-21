package flute.config;

public class ProjectConfig {
    public static String project;
    public static String projectSrcRoot;
    public static String generatedDataRoot;
    public static String projectGeneratedDataRoot;
    public static String publicStaticMemberRoot;
    public static String publicStaticMemberProject;
    public static String publicStaticMemberJre;

    // CugLM setting
    public static boolean CUGLM;
    public final static String cugLMTestProjectsPath = "/home/hieuvd/Kien/Flute-Kien-full/storage/repositories/git/four_hundred/";
    public final static String cugLMAllProjectsPath = "/home/hieuvd/CodeCompletion/dataset/CugLM/java_repos/";

    public static void init() {
        projectSrcRoot = "D:/Research/Flute/storage/repositories/git/" + project + "/";
        generatedDataRoot = "storage/gendata/";
        projectGeneratedDataRoot = "storage/gendata/" + project + "/";
        publicStaticMemberRoot = "storage/flute-ide/";
        publicStaticMemberProject = publicStaticMemberRoot + project + "_public_static_members.txt";
        publicStaticMemberJre = publicStaticMemberRoot + "rt_public_static_members.txt";
    }
}
