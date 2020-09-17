package flute.data.type;

import org.eclipse.jdt.core.dom.ITypeBinding;

public class BooleanPrimitiveType extends GenericType {
    @Override
    public boolean canBeAssignmentBy(ITypeBinding iTypeBinding) {
        if (TypeConstraintKey.BOOL_TYPES.contains(iTypeBinding.getKey())) {
            return true;
        }
        return false;
    }

    @Override
    public String getKey() {
        return "Z";
    }
}
