package flute.preprocessing;

import flute.config.Config;
import flute.jdtparser.callsequence.node.cfg.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FileFilter {
    public static List<File> filter(List<File> files) {
        files = files.stream().filter(file -> {
            return !Utils.checkTestFileWithoutLib(file);
        }).collect(Collectors.toList());
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
