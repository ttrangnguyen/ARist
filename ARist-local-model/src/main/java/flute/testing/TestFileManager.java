package flute.testing;

import flute.config.ProjectConfig;
import flute.config.TestConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;

// UPDATE: obsolete by now

// use for static testing
// if use cache then dont run this!

public class TestFileManager {
    public static void jlexToJlextest(String pathNoExtension) {
        File oldFile = new File(pathNoExtension + "jlex");
        File newFile = new File(pathNoExtension + "jlextest");
        oldFile.renameTo(newFile);
        File file = new File(pathNoExtension + "jlex");
        try {
            file.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void jexcodeToJexcodetest(String pathNoExtension) {
        File oldFile = new File(pathNoExtension + "jexcode");
        File newFile = new File(pathNoExtension + "jexcodetest");
        oldFile.renameTo(newFile);
        File file = new File(pathNoExtension + "jexcode");
        try {
            file.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void jlextestToJlex(String pathNoExtension) {
        File file = new File(pathNoExtension + "jlex");
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        File oldFile = new File(pathNoExtension + "jlextest");
        File newFile = new File(pathNoExtension + "jlex");
        oldFile.renameTo(newFile);
    }

    public static void jexcodetestToJexcode(String pathNoExtension) {
        File file = new File(pathNoExtension + "jexcode");
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        File oldFile = new File(pathNoExtension + "jexcodetest");
        File newFile = new File(pathNoExtension + "jexcode");
        oldFile.renameTo(newFile);
    }

    public static void main(String[] args) {
        // two modes: normal and test mode
        // switch to test mode -> TRAIN -> TEST
        // after TEST, remember to switch to normal mode BEFORE changing fold
//        try {
//            File testFilePaths = new File(TestConfig.testFilePath);
//            FileReader fr = new FileReader(testFilePaths);
//            BufferedReader br = new BufferedReader(fr);
//            String path;
//            while ((path = br.readLine()) != null) {
//                String absolutePath = ProjectConfig.generatedDataRoot + path.substring(0, path.length()-4);
////                jlexToJlextest(absolutePath);
////                jexcodeToJexcodetest(absolutePath);
//
////                jlextestToJlex(absolutePath);
////                jexcodetestToJexcode(absolutePath);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}
