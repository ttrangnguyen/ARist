package flute.preprocessing;

import flute.config.ProjectConfig;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** usage: move generated processed files to correct packages
*/

public class PackageSolver {
    public static List<File> walkJavaFile(String path) {
        List<File> listJavaFile = new ArrayList<File>();
        return walkJavaFileRecursive(path, listJavaFile);
    }

    private static List<File> walkJavaFileRecursive(String path, List<File> listJavaFile) {
        File root = new File(path);
        File[] list = root.listFiles();

        if (list == null) return listJavaFile;

        for (File f : list) {
            if (f.isDirectory()) {
                walkJavaFileRecursive(f.getAbsolutePath(), listJavaFile);
//                System.out.println("Dir:" + f.getAbsoluteFile());
            } else {
//                System.out.println("File:" + f.getAbsoluteFile());
                if (f.getName().toLowerCase().endsWith(".java")) {
                    listJavaFile.add(f);
                }
            }
        }
        return listJavaFile;
    }

    public static void solveFrom10Folds(File file, File dest) {
        for (int i = 0; i < 10; ++i) {
            String src = "D:\\Research\\data_v3\\data_classform\\java\\10_folds\\fold_" + i + "\\"
                        + ProjectConfig.project + "\\" + file.getName();
            File ssrc = new File(src.substring(0, src.length() - 4) + "txt");
            if (ssrc.exists()) {
                try {
                    FileUtils.copyFile(ssrc, dest);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    public static void solveFromTrainTestValidate(File file, File dest) {
        ArrayList<String> groups = new ArrayList<>();
        groups.add("train");
        groups.add("test");
        groups.add("validate");
        for (String i : groups) {
            String src = "D:\\Research\\data_v3\\data_classform\\java\\" + i + "\\"
                        + ProjectConfig.project + "\\" + file.getName();
            File ssrc = new File(src.substring(0, src.length() - 4) + "txt");
            if (ssrc.exists()) {
                try {
                    FileUtils.copyFile(ssrc, dest);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    public static void main(String[] args) {
        List<File> javaFiles = walkJavaFile(ProjectConfig.projectSrcRoot);
        String oldRoot = "D:\\Research\\Flute\\storage\\repositories\\git\\";
        String newRoot = ProjectConfig.generatedDataRoot;
        for (File file : javaFiles) {
            String name = file.getAbsolutePath();
            String newPath = newRoot + name.substring(oldRoot.length());
            File dest = new File(newPath.substring(0, newPath.length() - 4) + "txt");
//            solveFrom10Folds(file, dest);
            solveFromTrainTestValidate(file, dest);
        }
    }
}
