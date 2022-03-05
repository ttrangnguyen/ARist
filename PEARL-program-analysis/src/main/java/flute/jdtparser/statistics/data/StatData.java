package flute.jdtparser.statistics.data;

import java.util.HashMap;

public class StatData {
    private HashMap<DataType, Integer> data = new HashMap<>();

    public void increase(DataType recordName) {
        if (data.get(recordName) == null) {
            data.put(recordName, 1);
        } else {
            data.put(recordName, data.get(recordName) + 1);
        }
    }

    public void decrease(DataType recordName) {
        if (data.get(recordName) == null) {
            data.put(recordName, 0);
        } else {
            data.put(recordName, data.get(recordName) - 1);
        }
    }

    public int get(DataType recordName) {
        return data.get(recordName);
    }
}
