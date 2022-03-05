package flute.utils.file_reading;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class CSVReader {
    public static List<String> randomSet(String filename, String delimiter, int num) {
        List<String> allSet = read(filename, delimiter);
        List<String> result = new ArrayList<>();
        int[] randomIntsArray = IntStream.generate(() -> new Random().nextInt(allSet.size())).limit(num).toArray();
        for (int number : randomIntsArray) {
            result.add(allSet.get(number));
        }
        return result;
    }

    public static List<String> read(String filename, String delimiter) {
        List<String> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(CSVReader.class.getClassLoader().getResource(filename).getFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(delimiter);
                records.add(values[0]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return records;
    }
}
