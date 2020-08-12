package data;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClassModel {
    private ITypeBinding orgType;
    private List<IMethodBinding> methods = new ArrayList<>();
    private List<IVariableBinding> fields = new ArrayList<>();
    private List<Member> members = new ArrayList<>();

    private void parseMethodsAndConstructors(List<IMethodBinding> methodBindings) {
        for (int i = 0; i < methodBindings.size(); i++) {
            if (methodBindings.get(i).isConstructor()) {
                members.add(new ConstructorMember(methodBindings.get(i)));
            } else {
                members.add(new MethodMember(methodBindings.get(i)));
            }
        }
    }

    private void parseFields(List<IVariableBinding> fields) {
        fields.forEach(field -> {
            members.add(new FieldMember(field));
        });
    }

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

    public ClassModel(ITypeBinding orgType) {
        this.orgType = orgType;

        methods = new ArrayList<>(Arrays.asList(orgType.getDeclaredMethods()));
        fields = new ArrayList<>(Arrays.asList(orgType.getDeclaredFields()));

        parseSuperMember(orgType.getSuperclass());

        parseMethodsAndConstructors(methods);

        parseFields(fields);
    }

    public ClassModel(ClassModel classModel) {
        this.orgType = classModel.getOrgType();
        this.members.addAll(classModel.getMembers());
    }


    public ITypeBinding getOrgType() {
        return orgType;
    }

    public void setOrgType(ITypeBinding orgType) {
        this.orgType = orgType;
    }

    public List<Member> getMembers() {
        return members;
    }

    public ClassModel clone() {
        ClassModel classModel = new ClassModel(this);
        return classModel;
    }

    @Override
    public String toString() {
        return orgType.getQualifiedName();
    }
}
