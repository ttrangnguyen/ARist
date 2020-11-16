package flute.utils.file_processing;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DirProcessor {
    public static boolean exists(String path) {
        File dir = new File(path);
        return dir.exists();
    }

    public static List<File> walkJavaFile(String path) {
        List<File> listJavaFile = new ArrayList<File>();
        return walkJavaFileRecursive(path, listJavaFile);
    }

    public static List<File> getAllSubdirs(File file) {
        List<File> subdirs = Arrays.asList(file.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory();
            }
        }));
        subdirs = new ArrayList<File>(subdirs);

        List<File> deepSubdirs = new ArrayList<File>();
        for (File subdir : subdirs) {
            deepSubdirs.addAll(getAllSubdirs(subdir));
        }
        subdirs.addAll(deepSubdirs);
        return subdirs;
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

    public static List<File> walkData(String path) {
        List<File> list = new ArrayList<>();
        File root = new File(path);
        File[] listFile = root.listFiles();
        if (listFile == null) return list;

        for (File f : listFile) {
            if (f.isDirectory() && !f.isHidden()) {
                list.add(f);
            }
        }

        return list;
    }

}
