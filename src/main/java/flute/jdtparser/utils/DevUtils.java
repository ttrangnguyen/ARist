package flute.jdtparser.utils;

import flute.data.typemodel.BinType;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class DevUtils {
    public static List<String> getParamNames(IMethodBinding methodBinding) throws Exception {

        Class myClass = methodBinding.getClass();
        Field binding = myClass.getDeclaredField("binding");
        binding.setAccessible(true);

        Object bindingField = binding.get(methodBinding);

        Class bindingClass = bindingField.getClass();
        Field parameterNames = bindingClass.getDeclaredField("parameterNames");
        parameterNames.setAccessible(true);

        ArrayList<String> parameterNameList = new ArrayList<>();
        char[][] paramNameArr = (char[][]) parameterNames.get(bindingField);
        for (int i = 0; i < paramNameArr.length; i++) {
            parameterNameList.add(String.valueOf(paramNameArr[i]));
        }
        return parameterNameList;
    }

    public static ITypeBinding getSuperClass(ITypeBinding clazz) {
//        if (clazz.getName().equals("ExecutableElement")) {
//            System.out.println("a");
//        }
        if (clazz == null || clazz.getName() == null) return null;
        if (clazz.getSuperclass() == null && !clazz.getName().equals("Object")) {
            System.out.println("a");
            try {
                Class myClass = clazz.getClass();
                Field binding = myClass.getDeclaredField("binding");
                binding.setAccessible(true);

                BinaryTypeBinding bindingField = (BinaryTypeBinding) binding.get(clazz);

                Class bindingClass = bindingField.getClass();
                Field superclass = bindingClass.getDeclaredField("superclass");
                superclass.setAccessible(true);

                BinaryTypeBinding superField = (BinaryTypeBinding) superclass.get(bindingField);
                return new BinType(superField);
            } catch (Exception e) {
//                e.printStackTrace();
            }
        }
        return clazz.getSuperclass();
    }
}
