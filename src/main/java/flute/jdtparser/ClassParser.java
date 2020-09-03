package flute.jdtparser;

import flute.utils.parsing.CommonUtils;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClassParser {
    private ITypeBinding orgType;
    private List<IMethodBinding> methods;
    private List<IVariableBinding> fields;

    private void parseSuperMember(ITypeBinding superClass) {
        if (superClass == null) return;

        IMethodBinding[] superMethods = superClass.getDeclaredMethods();
        IVariableBinding[] superFields = superClass.getDeclaredFields();
        for (int i = 0; i < superFields.length; i++) {
            boolean find = false;
            if (Modifier.isPrivate(superFields[i].getModifiers()) && Modifier.isDefault(superFields[i].getModifiers()))
                continue;
            for (int j = 0; j < fields.size(); j++) {
                if (superFields[i].getName() == fields.get(j).getName()) {
                    find = true;
                    break;
                }
            }
            if (!find) fields.add(superFields[i]);
        }

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

        parseSuperMember(superClass.getSuperclass());
    }

    boolean compareMethod(IMethodBinding method, IMethodBinding coMethod) {
        if (method.getParameterTypes().length != coMethod.getParameterTypes().length) return false;

        for (int i = 0; i < method.getParameterTypes().length; i++) {
            if (method.getParameterTypes()[i] != coMethod.getParameterTypes()[i]) return false;
        }

        if (method.isConstructor() && coMethod.isConstructor()) {
            return true;
        }

        if (method.getName() == method.getName()) return true;

        return false;
    }

    public ClassParser(ITypeBinding orgType) {
        this.orgType = orgType;

        methods = new ArrayList<>(Arrays.asList(orgType.getDeclaredMethods()));
        fields = new ArrayList<>(Arrays.asList(orgType.getDeclaredFields()));

        parseSuperMember(orgType.getSuperclass());
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

    public boolean canSeenFrom(int modifier, ITypeBinding iTypeBinding) {
        if (iTypeBinding == orgType) {
            return true;
        } else {
            boolean extended = iTypeBinding.isSubTypeCompatible(orgType);

            String fromPackage = iTypeBinding.getPackage().getName();
            String toPackage = "-1";
            if (orgType.getPackage() != null) {
                toPackage = orgType.getPackage().getName();
            }

            if (CommonUtils.checkVisibleMember(orgType.getModifiers(), fromPackage, toPackage, extended)) {
                if (CommonUtils.checkVisibleMember(modifier, fromPackage, toPackage, extended)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return orgType.getQualifiedName();
    }
}
