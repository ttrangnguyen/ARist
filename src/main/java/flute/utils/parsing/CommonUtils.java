package flute.utils.parsing;

import org.eclipse.jdt.core.dom.Modifier;

import java.util.Arrays;

public class CommonUtils {
    public static void main(String arg[]) {
    }

    public static String normalizeId(String id) {
        return id.replaceAll("\\<.*\\>(\\(.*\\))?(\\{.*\\})?", "");
    }

    public static int getValueAccessModifier(int modifers) {
        if (Modifier.isPrivate(modifers)) {
            return 0;
        } else if (Modifier.isProtected(modifers)) {
            return 2;
        } else if (Modifier.isPublic(modifers)) {
            return 3;
        } else {
            return 1;
        }
    }

    public static String getPackageName(String classId) {
        String[] classIdArr = classId.split("\\.");
        if (classIdArr.length == 0) return classId;
        classIdArr = Arrays.copyOf(classIdArr, classIdArr.length - 1);
        String classPackageName = String.join(".", classIdArr);
        return classPackageName;
    }

    public static boolean checkVisibleMember(int modifier, String fromPackage, String checkPackage, boolean isExtended) {
        if (Modifier.isPublic(modifier)) return true;
        if (Modifier.isPrivate(modifier)) return false;

        if (modifier == 0) return true;

        if (Modifier.isProtected(modifier)) {
            if (fromPackage.equals(checkPackage) || isExtended) return true;
        } else {
            if (fromPackage.equals(checkPackage)) {
                return true;
            }
        }
        return false;
    }
}
