package flute.candidate;

public class Modifier {
    public static boolean isAbstract(int flags) {
        return (flags & 1024) != 0;
    }

    public static boolean isFinal(int flags) {
        return (flags & 16) != 0;
    }

    public static boolean isNative(int flags) {
        return (flags & 256) != 0;
    }

    public static boolean isPrivate(int flags) {
        return (flags & 2) != 0;
    }

    public static boolean isProtected(int flags) {
        return (flags & 4) != 0;
    }

    public static boolean isPublic(int flags) {
        return (flags & 1) != 0;
    }

    public static boolean isStatic(int flags) {
        return (flags & 8) != 0;
    }

    public static boolean isStrictfp(int flags) {
        return (flags & 2048) != 0;
    }

    public static boolean isSynchronized(int flags) {
        return (flags & 32) != 0;
    }

    public static boolean isTransient(int flags) {
        return (flags & 128) != 0;
    }

    public static boolean isVolatile(int flags) {
        return (flags & 64) != 0;
    }

    public static boolean isDefault(int flags) {
        return (flags & 65536) != 0;
    }

    public static boolean isSealed(int flags) {
        return (flags & 512) != 0;
    }

    public static boolean isNonSealed(int flags) {
        return (flags & 4096) != 0;
    }
}
