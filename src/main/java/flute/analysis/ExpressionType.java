package flute.analysis;

import com.github.javaparser.ast.expr.*;

import java.util.HashMap;

public enum ExpressionType {
    NAME, METHOD_INVOC, FIELD_ACCESS, ARRAY_ACCESS, CAST, STRING_LIT, NUM_LIT, CHAR_LIT, TYPE_LIT, BOOL_LIT,
    NULL_LIT, OBJ_CREATION, ARR_CREATION, THIS, SUPER, COMPOUND, LAMBDA, OTHERS;

    private static HashMap<Class, ExpressionType> classMap;

    static {
        classMap = new HashMap<>();
        classMap.put(NameExpr.class, NAME);
        classMap.put(MethodCallExpr.class, METHOD_INVOC);
        classMap.put(FieldAccessExpr.class, FIELD_ACCESS);
        classMap.put(ArrayAccessExpr.class, ARRAY_ACCESS);
        classMap.put(CastExpr.class, CAST);
        classMap.put(StringLiteralExpr.class, STRING_LIT);
        classMap.put(TextBlockLiteralExpr.class, STRING_LIT);
        classMap.put(IntegerLiteralExpr.class, NUM_LIT);
        classMap.put(LongLiteralExpr.class, NUM_LIT);
        classMap.put(DoubleLiteralExpr.class, NUM_LIT);
        classMap.put(CharLiteralExpr.class, CHAR_LIT);
        classMap.put(ClassExpr.class, TYPE_LIT);
        classMap.put(BooleanLiteralExpr.class, BOOL_LIT);
        classMap.put(NullLiteralExpr.class, NULL_LIT);
        classMap.put(ObjectCreationExpr.class, OBJ_CREATION);
        classMap.put(ArrayCreationExpr.class, ARR_CREATION);
        classMap.put(ThisExpr.class, THIS);
        classMap.put(SuperExpr.class, SUPER);
        classMap.put(AssignExpr.class, COMPOUND);
        classMap.put(BinaryExpr.class, COMPOUND);
        classMap.put(ConditionalExpr.class, COMPOUND);
        classMap.put(EnclosedExpr.class, COMPOUND);
        classMap.put(InstanceOfExpr.class, COMPOUND);
        classMap.put(UnaryExpr.class, COMPOUND);
        classMap.put(LambdaExpr.class, LAMBDA);
    }

    public static ExpressionType get(Expression expr) {
        return classMap.getOrDefault(expr.getClass(), OTHERS);
    }
}
