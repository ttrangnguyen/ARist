package flute.jdtparser.utils;

import flute.data.typemodel.Variable;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ParserUtils {
    private static ITypeBinding curType;
    private static ITypeBinding nextType;

    public static List<IVariableBinding> getAllSuperFields(ITypeBinding iTypeBinding) {
        curType = iTypeBinding;
        return innerGetAllSuperFields(iTypeBinding);
    }

    private static List<IVariableBinding> innerGetAllSuperFields(ITypeBinding iTypeBinding) {
        ITypeBinding superClass = iTypeBinding.getSuperclass();
        if (superClass == null) return new ArrayList<>();
        List<IVariableBinding> variableBindings = new ArrayList<>();

        //add from parent
        nextType = superClass;
        addVariableToList(Arrays.asList(superClass.getDeclaredFields()), variableBindings);
        //add from interface
        nextType = curType;
        addVariableToList(getAllInterfaceFields(iTypeBinding), variableBindings);
        //add from parent of parent
        addVariableToList(getAllSuperFields(superClass), variableBindings);

        return variableBindings;
    }

    public static List<IVariableBinding> getAllInterfaceFields(ITypeBinding iTypeBinding) {
        List<IVariableBinding> variableBindings = new ArrayList<>();
        for (ITypeBinding anInterface : iTypeBinding.getInterfaces()) {
            Collections.addAll(variableBindings, anInterface.getDeclaredFields());
        }
        return variableBindings;
    }

    public static boolean checkVariableInList(IVariableBinding variableBinding, List<IVariableBinding> list) {
        for (IVariableBinding variableBindingItem : list) {
            if (variableBindingItem.getName().equals(variableBinding.getName())) return true;
        }
        return false;
    }

    public static void addVariableToList(List<IVariableBinding> variableBindings, List<IVariableBinding> list) {
        for (IVariableBinding declaredField : variableBindings) {
            if (!checkVariableInList(declaredField, list)
                    && !Modifier.isPrivate(declaredField.getModifiers())
                    && (curType.getPackage() == nextType.getPackage() || !Modifier.isDefault(declaredField.getModifiers()))) {
                list.add(declaredField);
            }
        }
    }
}
