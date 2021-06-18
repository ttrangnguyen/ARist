package flute.modeling.gpt;

import flute.config.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileFilter {
    public static List<File> filter(List<File> files) {
        files = filterTestFolders(files);
        return files;
    }

    public static List<File> filterTestFolders(List<File> files) {
        List<File> filteredFiles = new ArrayList<>();
        loopDir:
        for (File file: files) {
            String filePath = file.getAbsolutePath();
            for (String blacklistedFolder: Config.BLACKLIST_FOLDER_SRC) {
                if (filePath.contains('\\' + blacklistedFolder + '\\')) {
                    continue loopDir;
                }
            }
            filteredFiles.add(file);
        }
        return filteredFiles;
    }
}
