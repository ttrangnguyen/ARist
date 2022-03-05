package flute.jdtparser.utils;

import flute.data.ParserCompareValue;
import flute.data.constraint.ParserConstant;

import java.util.List;

public class ParserCompare {

    public static boolean isTrue(ParserCompareValue value) {
        if (value.contains(ParserConstant.TRUE_VALUE)
                || value.contains(ParserConstant.VARARGS_TRUE_VALUE)) return true;
        return false;
    }

    public static boolean isVarArgs(ParserCompareValue value) {
        if (value.contains(ParserConstant.VARARGS_TRUE_VALUE)) return true;
        return false;
    }

    public static boolean isArrayType(ParserCompareValue value) {
        if (value.contains(ParserConstant.IS_ARRAY_VALUE)) return true;
        return false;
    }

    public static boolean canBeCast(ParserCompareValue value) {
        if (value.contains(ParserConstant.CAN_BE_CAST_VALUE)) return true;
        return false;
    }

    public static boolean isFalse(ParserCompareValue value) {
        if (value.contains(ParserConstant.FALSE_VALUE)) return true;
        return false;
    }
}
