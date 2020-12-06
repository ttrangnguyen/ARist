package flute.utils;

import flute.config.Config;
import flute.utils.logging.Timer;

public class ProgressBar {
    private Timer timer;
    private float progress;
    private long eta;

    public ProgressBar() {
        timer = new Timer();
    }

    public void setProgress(float progress, boolean print) {
        if (print && (this.progress - progress) > Config.PRINT_PROGRESS_DELTA) {
            long runTime = Timer.getCurrentTime().getTime() - timer.getLastTime().getTime();
            eta = (long) ((runTime / progress) - runTime) / 1000;
            System.out.printf("%5.2f %s - ETA: %s\n",
                    progress * 100f, genProgressBar(progress * 100, Config.PROGRESS_SIZE), Timer.formatTime(eta));
        }
        this.progress = progress;
    }

    public void setProgress(float progress) {
        setProgress(progress, false);
    }

    public static void printProcessBar(float percent, int size) {
        System.out.print(genProgressBar(percent, size));
    }

    public static String genProgressBar(float percent, int size) {
        int processedItem = (int) Math.ceil(percent / (100f / size));
        StringBuilder bar = new StringBuilder();
        bar.append("[");
        for (int i = 0; i < processedItem; i++) {
            if (i == processedItem - 1) {
                bar.append(">");
            } else {
                bar.append("=");
            }
        }

        for (int i = 0; i < size - processedItem; i++) {
            bar.append("-");
        }
        bar.append("]");
        return bar.toString();
    }

    public static void main(String[] args) {
        printProcessBar(60, 40);
    }
}
