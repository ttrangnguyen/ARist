package flute.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiMap {
    private HashMap<String, List<String>> multiMap;

    public MultiMap() {
        multiMap = new HashMap<>();
    }

    public HashMap<String, List<String>> getValue() {
        return multiMap;
    }

    public void put(String key, String value) {
        List<String> keyValue = multiMap.get(key);
        if (keyValue == null) {
            keyValue = new ArrayList<>();
            keyValue.add(value);
            multiMap.put(key, keyValue);
        } else {
            if (!keyValue.contains(value)) {
                keyValue.add(value);
            }
        }
    }

    public List<String> get(String key) {
        return multiMap.get(key);
    }

    public List<List<Boolean>> convertLocalVariableMap(List<String> localVariables) {
        List<List<Boolean>> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : multiMap.entrySet()) {
            List<String> value = entry.getValue();
            List<Boolean> convertValue = new ArrayList<>();
            for (String lex : value) {
                if (localVariables.contains(lex)
                        || (lex.split(".").length > 0 && localVariables.contains(lex.split(".")[0]))) {
                    convertValue.add(true);
                } else {
                    convertValue.add(false);
                }
            }
            result.add(convertValue);
        }

        return result;
    }
}
