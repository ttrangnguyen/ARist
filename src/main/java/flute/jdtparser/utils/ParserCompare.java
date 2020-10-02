package flute.jdtparser.utils;

import flute.data.constraint.ParserConstant;

public class ParserCompare {

    public static boolean isTrue(int value) {
        if (value == ParserConstant.TRUE_VALUE
                || value == ParserConstant.VARARGS_TRUE_VALUE) return true;
        return false;
    }

    public static boolean isVarArgs(int value) {
        if (value == ParserConstant.VARARGS_TRUE_VALUE) return true;
        return false;
    }

    public static boolean canBeCast(int value) {
        if (value == ParserConstant.CAN_BE_CAST_VALUE) return true;
        return false;
    }
}
