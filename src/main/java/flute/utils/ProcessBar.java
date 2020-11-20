package flute.utils;

public class ProcessBar {
    public static void printProcessBar(float percent, int size) {
        int processedItem = (int) Math.ceil(percent / (100f / size));
        System.out.print("[");
        for (int i = 0; i < processedItem; i++) {
            if (i == processedItem - 1) {
                System.out.print(">");
            } else {
                System.out.print("=");
            }
        }

        for (int i = 0; i < size - processedItem; i++) {
            System.out.print("-");
        }
        System.out.print("]");
    }

    public static void main(String[] args) {
        printProcessBar(60, 40);
    }
}
