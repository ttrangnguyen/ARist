package flute.analysis;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.*;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionFieldDeclaration;

import java.util.HashMap;

public enum ExpressionOrigin {
    REFLECT_FIELD, FIELD, PARAM, LOCAL_VAR, LIT, ENUM_CONST, CLASS, NULL, OBJ_CREATION, ARR_CREATION, THIS, SUPER, OTHERS;

    private static HashMap<Class, ExpressionOrigin> classMap;

    static {
        classMap = new HashMap<>();
        classMap.put(StringLiteralExpr.class, LIT);
        classMap.put(TextBlockLiteralExpr.class, LIT);
        classMap.put(IntegerLiteralExpr.class, LIT);
        classMap.put(LongLiteralExpr.class, LIT);
        classMap.put(DoubleLiteralExpr.class, LIT);
        classMap.put(CharLiteralExpr.class, LIT);
        classMap.put(ClassExpr.class, CLASS);
        classMap.put(BooleanLiteralExpr.class, LIT);
        classMap.put(NullLiteralExpr.class, NULL);
        classMap.put(ObjectCreationExpr.class, OBJ_CREATION);
        classMap.put(ArrayCreationExpr.class, ARR_CREATION);
        classMap.put(ThisExpr.class, THIS);
        classMap.put(SuperExpr.class, SUPER);

        classMap.put(JavaParserFieldDeclaration.class, FIELD);
        classMap.put(ReflectionFieldDeclaration.class, REFLECT_FIELD);
        classMap.put(JavaParserParameterDeclaration.class, PARAM);
        classMap.put(JavaParserSymbolDeclaration.class, LOCAL_VAR);
        classMap.put(JavaParserClassDeclaration.class, OTHERS);
        classMap.put(JavaParserEnumConstantDeclaration.class, ENUM_CONST);
    }

    public static ExpressionOrigin get(Expression expr) {
        ExpressionOrigin origin = classMap.getOrDefault(expr.getClass(), null);
        if (origin != null) return origin;
        if (expr.findFirst(NameExpr.class).isPresent()) {
            NameExpr name = expr.findFirst(NameExpr.class).get();
            try {
                ResolvedValueDeclaration resolve = name.resolve();
                return classMap.getOrDefault(resolve, OTHERS);
            } catch (Exception e) {
                return OTHERS;
            }
        }
        return null;
    }
}
