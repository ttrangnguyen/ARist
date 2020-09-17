package flute.data.type;

import org.eclipse.jdt.core.dom.ITypeBinding;

public class NumPrimitiveType extends GenericType {
    protected static final String KEY = "";

    protected String getInnerKey() {
        return KEY;
    }

    @Override
    public boolean isAssignmentCompatible(ITypeBinding iTypeBinding) {
        int intPos = TypeConstraintKey.NUM_WRAP_TYPES.indexOf(getInnerKey());
        int typePos = TypeConstraintKey.NUM_WRAP_TYPES.indexOf(iTypeBinding.getKey());
        typePos = typePos >= 0 ? typePos : TypeConstraintKey.NUM_PRIMITIVE_TYPES.indexOf(iTypeBinding.getKey());
        if (typePos >= intPos && typePos >= 0) return true;
        return false;
    }

    @Override
    public String getKey() {
        return getInnerKey();
    }
}
