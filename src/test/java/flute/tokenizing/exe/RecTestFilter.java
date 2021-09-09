package flute.tokenizing.exe;

import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import flute.config.Config;
import flute.tokenizing.excode_data.NodeSequenceInfo;

import java.util.Arrays;
import java.util.List;

public class RecTestFilter {
    public static boolean predictable(List<NodeSequenceInfo> nodeSequenceList) {
        for (NodeSequenceInfo excode: nodeSequenceList) {
            // TODO: Ignore null literal for now
            if (NodeSequenceInfo.isLiteral(excode, "null") && !Config.FEATURE_PARAM_TYPE_NULL_LIT) return false;
            if (NodeSequenceInfo.isMethodAccess(excode) && !Config.FEATURE_PARAM_TYPE_METHOD_INVOC) return false;
            if (NodeSequenceInfo.isOpenBrak(excode) && !Config.FEATURE_PARAM_TYPE_ARRAY_ACCESS) return false;
            if (NodeSequenceInfo.isCast(excode)) {
                if (!Config.FEATURE_PARAM_TYPE_CAST) return false;
                // Only accept (Class) object
                if (!(nodeSequenceList.size() == 2 && NodeSequenceInfo.isVar(nodeSequenceList.get(1)))) return false;
            }
            if (NodeSequenceInfo.isObjectCreation(excode)) {
                if (!Config.FEATURE_PARAM_TYPE_OBJ_CREATION) return false;
                // Not accept Primitive wrapper classes
                List primitiveWrapperClasses = Arrays.asList("Byte", "Short", "Integer", "Long", "Float", "Double", "Character", "Boolean");
                if (primitiveWrapperClasses.contains(excode.getAttachedAccess())) return false;
            };
            if (NodeSequenceInfo.isArrayCreation(excode)) {
                if (!Config.FEATURE_PARAM_TYPE_ARR_CREATION) return false;
            }
            if (!Config.FEATURE_PARAM_TYPE_COMPOUND) {
                if (NodeSequenceInfo.isAssign(excode)) return false;
                if (NodeSequenceInfo.isOperator(excode)) return false;
                if (NodeSequenceInfo.isUnaryOperator(excode)) return false;
                if (NodeSequenceInfo.isConditionalExpr(excode)) return false;

                // For EnclosedExpr
                if (excode == nodeSequenceList.get(0) && NodeSequenceInfo.isOpenPart(excode)) return false;
            }
            if (NodeSequenceInfo.isClassExpr(excode) && !Config.FEATURE_PARAM_TYPE_TYPE_LIT) return false;
            if (NodeSequenceInfo.isLambda(excode) && !Config.FEATURE_PARAM_TYPE_LAMBDA) return false;
            if (NodeSequenceInfo.isMethodReference(excode) && !Config.FEATURE_PARAM_TYPE_METHOD_REF) return false;

            // For static field access
            if (NodeSequenceInfo.isFieldAccess(excode) && !Config.FEATURE_PARAM_STATIC_FIELD_ACCESS_FROM_CLASS) {
                FieldAccessExpr fieldAccess = (FieldAccessExpr) excode.oriNode;
                boolean isScopeAClass = false;
                if (fieldAccess.getScope() instanceof NameExpr) {
                    try {
                        ((NameExpr) fieldAccess.getScope()).resolve();
                    }
                    // Field access from generic type?
                    catch (IllegalStateException ise) {
                        isScopeAClass = true;
                    }
                    // Field access from a class
                    catch (UnsolvedSymbolException use) {
                        isScopeAClass = true;
                    }
                    // ???
                    catch (UnsupportedOperationException uoe) {
                        isScopeAClass = true;
                    }
                    // ???
                    catch (RuntimeException re) {
                        isScopeAClass = true;
                    }
                } else if (fieldAccess.getScope() instanceof FieldAccessExpr) {
                    isScopeAClass = true;
                }
                if (isScopeAClass) {
                    String scope = fieldAccess.getScope().toString();
                    if (scope.indexOf('.') >= 0) {
                        scope = scope.substring(scope.lastIndexOf('.') + 1);
                    }
                    if (Character.isUpperCase(scope.charAt(0))) {
                        try {
                            ResolvedFieldDeclaration resolve = fieldAccess.resolve().asField();
                            if (resolve.isStatic() || resolve.declaringType().isInterface()) {
                                //System.out.println("Detected: " + excode.oriNode);
                                return false;
                            }
                        }
                        // Not an actual field
                        catch (IllegalStateException | UnsolvedSymbolException | UnsupportedOperationException e) {
                            if (fieldAccess.getNameAsString().matches("^[A-Z]+(?:_[A-Z]+)*$")) {
                                //System.out.println("Detected: " + excode.oriNode);
                                return false;
                            } else {
                                //System.out.println(fieldAccess);
                                //e.printStackTrace();
                            }
                        }
                    } else {
                        //use.printStackTrace();
                    }
                }
            }
        }
        return true;
    }
}
