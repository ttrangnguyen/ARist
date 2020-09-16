package flute.data.type;

import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.Arrays;
import java.util.List;

public class IBooleanType extends IGenericType {
    @Override
    public boolean canBeAssignmentBy(ITypeBinding iTypeBinding) {
        if (TypeConstraintKey.BOOL_TYPES.contains(iTypeBinding.getKey())) {
            return true;
        }
        return false;
    }
}
