package flute.modeling;

import flute.config.ModelConfig;
import slp.core.modeling.runners.ModelRunner;

import java.io.*;

public class ModelManager {
    public static final Integer CREATING = 0;
    public static final Integer READING = 1;

    public ModelRunner modelRunner;
    public String modelPath;

    public ModelManager(int mode) {
        initModelPath();
        if (mode == ModelManager.CREATING) {
            createModel();
        }
        if (mode == ModelManager.READING) {
            readModel();
        }
    }

    void initModelPath() {}
    void createModel() {}
    void train() {}

    public void saveModel() throws IOException {
        FileOutputStream f = new FileOutputStream(modelPath);
        ObjectOutputStream o = new ObjectOutputStream(f);
        o.writeObject(modelRunner);
        o.close();
        f.close();
    }

    public void readModel() {
        try {
            System.out.println("Model path: " + modelPath);
            FileInputStream fi = new FileInputStream(modelPath);
            ObjectInputStream oi = new ObjectInputStream(fi);
            modelRunner = (ModelRunner) oi.readObject();
            oi.close();
            fi.close();
        } catch (IOException|ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
