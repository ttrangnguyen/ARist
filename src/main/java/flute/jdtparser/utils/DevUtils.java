package maytinhdibo.eclipse.jdt.core.dom;

import org.eclipse.jdt.core.dom.IMethodBinding;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class DevUtils {
    public static List<String> getResolver(IMethodBinding methodBinding) throws Exception {

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
}
