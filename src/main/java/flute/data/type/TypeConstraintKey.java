package flute.data.type;

import java.util.Arrays;
import java.util.List;

public class TypeConstraintKey {
    final public static List<String> NUM_TYPES = Arrays.asList(new String[]{"Ljava/lang/Byte;", "Ljava/lang/Char;", "Ljava/lang/Short;", "Ljava/lang/Integer;",
            "Ljava/lang/Long;", "Ljava/lang/Float;", "Ljava/lang/Double;", "B", "S", "C", "I", "J", "F", "D"});

    final public static List<String> NUM_PRIMITIVE_TYPES = Arrays.asList(new String[]{"B", "S", "C", "I", "J", "F", "D"});

    final public static List<String> NUM_WRAP_TYPES = Arrays.asList(new String[]{"Ljava/lang/Byte;", "Ljava/lang/Char;", "Ljava/lang/Short;", "Ljava/lang/Integer;",
            "Ljava/lang/Long;", "Ljava/lang/Float;", "Ljava/lang/Double;"});

    final public static List<String> BOOL_TYPES = Arrays.asList(new String[]{"Ljava/lang/Boolean;", "Z"});

    final public static String OBJECT_TYPE = "Ljava/lang/Object;";
    final public static String STRING_TYPE = "Ljava/lang/String;";

    final public static String CLASS_TYPE = "Ljava/lang/Class;";

    final public static List<String> WRAP_TYPES = Arrays.asList(new String[]{"Ljava/lang/Object;", "Ljava/io/Serializable;"});
}
