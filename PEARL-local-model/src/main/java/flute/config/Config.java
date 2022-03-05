package flute.config;

import flute.testing.TestFilesManager;

public class Config {
    public enum Mode {
        TRAIN, TEST, EVALUATE
    }
    public static Mode mode;

    public static void init() {
        ModelConfig.init();
        ProjectConfig.init();
        if (mode != Config.Mode.EVALUATE) {
            TestFilesManager.init();
        }
        if (mode != Config.Mode.TRAIN) {
            TestConfig.init();
        }
    }
}
