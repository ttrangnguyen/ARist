package flute.jdtparser;

import flute.data.type.CustomVariableBinding;
import flute.data.type.IntPrimitiveType;
import flute.jdtparser.utils.ParserUtils;
import flute.utils.parsing.CommonUtils;

import flute.config.Config;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static flute.jdtparser.utils.DevUtils.getSuperClass;

public class ClassParser {
    private ITypeBinding orgType;
    private final List<IMethodBinding> methods;
    private final List<IVariableBinding> fields;

    private void parseSuperMethod(ITypeBinding superClass) {
        if (superClass == null) return;

        IMethodBinding[] superMethods = superClass.getDeclaredMethods();
//        IVariableBinding[] superFields = superClass.getDeclaredFields();
//        for (int i = 0; i < superFields.length; i++) {
//            boolean find = false;
//            if (Modifier.isPrivate(superFields[i].getModifiers()) && Modifier.isDefault(superFields[i].getModifiers()))
//                continue;
//            for (int j = 0; j < fields.size(); j++) {
//                if (superFields[i].getName() == fields.get(j).getName()) {
//                    find = true;
//                    break;
//                }
//            }
//            if (!find) fields.add(superFields[i]);
//        }

        for (int i = 0; i < superMethods.length; i++) {
            boolean find = false;
            if (Modifier.isPrivate(superMethods[i].getModifiers()) && Modifier.isDefault(superMethods[i].getModifiers()))
                continue;
            for (int j = 0; j < methods.size(); j++) {
                if (compareMethod(superMethods[i], methods.get(j))) {
                    find = true;
                    break;
                }
            }
            if (!find) methods.add(superMethods[i]);
        }

        parseSuperMethod(getSuperClass(superClass));
    }

    public static boolean compareMethod(IMethodBinding method, IMethodBinding coMethod) {
        if (method.getParameterTypes().length != coMethod.getParameterTypes().length) return false;

        for (int i = 0; i < method.getParameterTypes().length; i++) {
            if (method.getParameterTypes()[i] != coMethod.getParameterTypes()[i]) return false;
        }

        if (method.isConstructor() && coMethod.isConstructor()) {
            return true;
        }

        return coMethod.getName().equals(method.getName());
    }

    public ClassParser(ITypeBinding orgType) {
        this.orgType = orgType;

        methods = new ArrayList<>(Arrays.asList(orgType.getDeclaredMethods()));
        fields = new ArrayList<>(Arrays.asList(orgType.getDeclaredFields()));

        if (orgType.isArray()) {
            //25 -> public static final
            fields.add(new CustomVariableBinding(25, "length", new IntPrimitiveType(), orgType));
        }

        ParserUtils.addVariableToList(ParserUtils.getAllSuperFields(orgType), fields);
        parseSuperMethod(getSuperClass(orgType));

        if (Config.FEATURE_ADD_FIELD_AND_METHOD_FROM_SUPER_INTERFACE) {
            for (ITypeBinding iType : orgType.getInterfaces()) {
                parseSuperMethod(iType);
            }
        }
    }

    public ITypeBinding getOrgType() {
        return orgType;
    }

    public void setOrgType(ITypeBinding orgType) {
        this.orgType = orgType;
    }

    public List<IMethodBinding> getMethods() {
        return methods;
    }

    public List<IMethodBinding> getMethodsFrom(ITypeBinding iTypeBinding) {
        return getMethodsFrom(iTypeBinding, false);
    }

    public List<IMethodBinding> getMethodsFrom(ITypeBinding iTypeBinding, boolean isStatic) {
        List<IMethodBinding> canSeenMethods = new ArrayList<>();
        methods.forEach(method -> {
            if (canSeenFrom(method.getModifiers(), iTypeBinding)
                    && (!isStatic || Modifier.isStatic(method.getModifiers())))
                canSeenMethods.add(method);
        });
        return canSeenMethods;
    }

    public List<IVariableBinding> getFields() {
        return fields;
    }

    public List<IVariableBinding> getFieldsFrom(ITypeBinding iTypeBinding) {
        return getFieldsFrom(iTypeBinding, false);
    }

    public List<IVariableBinding> getFieldsFrom(ITypeBinding iTypeBinding, boolean isStatic) {
        List<IVariableBinding> canSeenFields = new ArrayList<>();
        fields.forEach(field -> {
            if (canSeenFrom(field.getModifiers(), iTypeBinding)
                    && (!isStatic || Modifier.isStatic(field.getModifiers())))
                canSeenFields.add(field);
        });
        return canSeenFields;
    }

//    public List<IVariableBinding> getPublicStaticFields() {
//        List<IVariableBinding> staticFields = new ArrayList<>();
//        fields.forEach(field -> {
//            int modifier = field.getModifiers();
//            if (Modifier.isPublic(modifier) && Modifier.isStatic(modifier)) {
//                staticFields.add(field);
//            }
//        });
//        return staticFields;
//    }
//
//    public List<IMethodBinding> getPublicStaticMethods() {
//        List<IMethodBinding> staticMethods = new ArrayList<>();
//        methods.forEach(method -> {
//            int modifier = method.getModifiers();
//            if (!(method.getReturnType().getKey() == null || method.getReturnType().getKey().equals("V"))) {
//                if (!method.isConstructor()
//                        && Modifier.isPublic(modifier) && Modifier.isStatic(modifier)) {
//                    staticMethods.add(method);
//                }
//            }
//        });
//        return staticMethods;
//    }

    public boolean canSeenFrom(int modifier, ITypeBinding clientType) {
        ITypeBinding elementType = orgType.isArray() ? orgType.getElementType() : orgType;
        int classModifier = elementType.getModifiers();

        if (clientType == orgType || Arrays.asList(clientType.getDeclaredTypes()).contains(elementType)) {
            return true;
        } else {
            boolean extended = elementType.isSubTypeCompatible(orgType);

            String fromPackage = clientType.getPackage().getName();

            String toPackage = "-1";
            if (elementType.getPackage() != null) {
                toPackage = elementType.getPackage().getName();
            }
            if (CommonUtils.checkVisibleMember(classModifier, fromPackage, toPackage, extended)) {
                return CommonUtils.checkVisibleMember(modifier, fromPackage, toPackage, extended);
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return orgType.getQualifiedName();
    }
}
