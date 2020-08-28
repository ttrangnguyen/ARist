package flute.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MultiMap {
    private HashMap<String, List<String>> multiMap;

    public MultiMap() {
        multiMap = new HashMap<>();
    }

    public void put(String key, String value) {
        List<String> keyValue = multiMap.get(key);
        if (keyValue == null) {
            keyValue = new ArrayList<>();
            keyValue.add(value);
            multiMap.put(key, keyValue);
        } else {
            keyValue.add(value);
        }
    }

    public List<String> get(String key) {
        return multiMap.get(key);
    }
}
