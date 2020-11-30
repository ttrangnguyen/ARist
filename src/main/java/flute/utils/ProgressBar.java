package flute.utils;

public class ProgressBar {
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
