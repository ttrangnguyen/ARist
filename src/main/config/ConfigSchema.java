package config;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ConfigSchema {
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("project-dir")
    @Expose
    private String projectDir;
    @SerializedName("source-paths")
    @Expose
    private List<String> sourcePaths = null;
    @SerializedName("encode-sources")
    @Expose
    private List<String> encodeSources = null;
    @SerializedName("class-paths")
    @Expose
    private List<String> classPaths = null;
    @SerializedName("jdt-level")
    @Expose
    private Integer jdtLevel;
    @SerializedName("java-version")
    @Expose
    private String javaVersion;
    @SerializedName("ignore-files")
    @Expose
    private List<String> ignoreFiles = null;
    @SerializedName("test-file-path")
    @Expose
    private String testFilePath;
    @SerializedName("test-position")
    @Expose
    private Integer testPosition;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProjectDir() {
        return projectDir;
    }

    public void setProjectDir(String projectDir) {
        this.projectDir = projectDir;
    }

    public List<String> getSourcePaths() {
        return sourcePaths;
    }

    public void setSourcePaths(List<String> sourcePaths) {
        this.sourcePaths = sourcePaths;
    }

    public List<String> getEncodeSources() {
        return encodeSources;
    }

    public void setEncodeSources(List<String> encodeSources) {
        this.encodeSources = encodeSources;
    }

    public List<String> getClassPaths() {
        return classPaths;
    }

    public void setClassPaths(List<String> classPaths) {
        this.classPaths = classPaths;
    }

    public Integer getJdtLevel() {
        return jdtLevel;
    }

    public void setJdtLevel(Integer jdtLevel) {
        this.jdtLevel = jdtLevel;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public List<String> getIgnoreFiles() {
        return ignoreFiles;
    }

    public void setIgnoreFiles(List<String> ignoreFiles) {
        this.ignoreFiles = ignoreFiles;
    }

    public String getTestFilePath() {
        return testFilePath;
    }

    public void setTestFilePath(String testFilePath) {
        this.testFilePath = testFilePath;
    }

    public Integer getTestPosition() {
        return testPosition;
    }

    public void setTestPosition(Integer testPosition) {
        this.testPosition = testPosition;
    }

}