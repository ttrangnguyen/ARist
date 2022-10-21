package flute.preprocessing;

import flute.config.Config;
import flute.jdtparser.ProjectParser;

import java.io.File;
import java.io.IOException;

public class MethodExtractorForManualConfig extends MethodExtractor {
    @Override
    void setupProjectParser(File project) {
        try {
            Config.loadConfig(Config.STORAGE_DIR + "/json/" + project + ".json");
        } catch (IOException ioe) {
            System.err.println("WARNING: Config file does not exist!");
            System.err.println("Project Parser is now configured automatically.");
            Config.autoConfigure(project.getName(), project.getAbsolutePath());
        }
        parser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH, Config.ENCODE_SOURCE,
                Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);
    }

    public static void main(String[] args) {
        String inputFolder = "../../Kien/Flute-Kien-full/storage/repositories/git/" + args[0];
        String outputFolder = "../../Tannm/storage/" + "dataset-eclipse_netbeans/" + args[1] + "/" + args[0];
        String fileListPath = "../../Kien/Flute-Kien-full/docs/testFilePath/datapath/" + args[1] + "/" + args[0] + ".txt";

        Preprocessor preprocessor = new Preprocessor();

//        System.out.println("\nPreprocessing projects...");
//        preprocessor = new RemoveCommentDecorator(preprocessor);
//        preprocessor = new RemoveNewLineDecorator(preprocessor);
//        preprocessor = new RemoveIndentDecorator(preprocessor);
//        preprocessor.preprocessProjects(new File(inputFolder), new File(inputFolder));

        System.out.println("\nExtracting methods...");
        preprocessor = new MethodExtractorForManualConfig();
        preprocessor.preprocessProject(new File(inputFolder),
                new File(outputFolder),
                new File(fileListPath));
    }
}
