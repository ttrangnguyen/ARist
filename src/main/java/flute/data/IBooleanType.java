package flute.data;

import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.Arrays;
import java.util.List;

public class IBooleanType extends IGenericType {
    @Override
    public boolean canBeAssignmentBy(ITypeBinding iTypeBinding) {
        List<String> boolType = Arrays.asList(new String[]{"Ljava/lang/Boolean;", "Z"});
        if (boolType.contains(iTypeBinding.getKey())) {
            return true;
        }
        return false;
    }
}
