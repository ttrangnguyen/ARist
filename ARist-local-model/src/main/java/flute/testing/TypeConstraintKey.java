package flute.testing;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class TypeConstraintKey {
    final public static List<String> NUM_TYPES = Arrays.asList(new String[]{"Ljava/lang/Byte;", "Ljava/lang/Char;", "Ljava/lang/Short;", "Ljava/lang/Integer;",
            "Ljava/lang/Long;", "Ljava/lang/Float;", "Ljava/lang/Double;", "B", "S", "C", "I", "J", "F", "D"});

    final public static List<String> NUM_PRIMITIVE_TYPES = Arrays.asList(new String[]{"B", "S", "C", "I", "J", "F", "D"});

    final public static List<String> NUM_WRAP_TYPES = Arrays.asList(new String[]{"Ljava/lang/Byte;", "Ljava/lang/Char;", "Ljava/lang/Short;", "Ljava/lang/Integer;",
            "Ljava/lang/Long;", "Ljava/lang/Float;", "Ljava/lang/Double;"});

    final public static List<String> BOOL_TYPES = Arrays.asList(new String[]{"Ljava/lang/Boolean;", "Z"});

    final public static String MAP_TYPES = "Ljava/util/Map<";
    final public static String HASHMAP_TYPES = "Ljava/util/HashMap<";

    final public static String OBJECT_TYPE = "Ljava/lang/Object;";
    final public static String STRING_TYPE = "Ljava/lang/String;";
    final public static String CHAR_SEQUE_TYPE = "Ljava/lang/CharSequence;";

    final public static String CLASS_TYPE = "Ljava/lang/Class<>;";

    final public static List<String> WRAP_TYPES = Arrays.asList(new String[]{"Ljava/lang/Object;", "Ljava/io/Serializable;"});

    public static boolean assignWith(String key, String assignToKey) {
        if (assignToKey.equals(OBJECT_TYPE)) return true;
        if (key.equals(assignToKey)) return true;
        List<List<String>> typeConstraintKeys = Arrays.asList(NUM_WRAP_TYPES, NUM_PRIMITIVE_TYPES);

        if (BOOL_TYPES.contains(key) && BOOL_TYPES.contains(assignToKey)) {
            return true;
        }

        //upcasting number
        for (List<String> typeConstraintKey :
                typeConstraintKeys) {
            if (typeConstraintKey.contains(key) && typeConstraintKey.contains(assignToKey)) {
                if (typeConstraintKey.indexOf(key) < typeConstraintKey.indexOf(assignToKey)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static HashSet<String> assignWith(String key) {
        HashSet<String> result = new HashSet<>();
        if (BOOL_TYPES.contains(key)) {
            for (String booleanType : BOOL_TYPES) {
                result.add(booleanType);
            }
        } else {
            List<List<String>> typeConstraintKeys = Arrays.asList(NUM_WRAP_TYPES, NUM_PRIMITIVE_TYPES);
            for (List<String> typeConstraintKey :
                    typeConstraintKeys) {
                if (typeConstraintKey.contains(key))
                    result.addAll(typeConstraintKey.subList(0, typeConstraintKey.indexOf(key)));
            }
        }

        result.add(key);
        return result;
    }
}