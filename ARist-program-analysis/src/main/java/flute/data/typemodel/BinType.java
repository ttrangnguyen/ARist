package flute.data.typemodel;

import flute.data.typemodel.method.BinMethodBinding;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class BinType implements ITypeBinding {
    private TypeBinding binType;

    public BinType(TypeBinding binType) {
        this.binType = binType;
    }

    @Override
    public ITypeBinding createArrayType(int i) {
        return null;
    }

    @Override
    public String getBinaryName() {
        return null;
    }

    @Override
    public ITypeBinding getBound() {
        return null;
    }

    @Override
    public ITypeBinding getGenericTypeOfWildcardType() {
        return null;
    }

    @Override
    public int getRank() {
        return 0;
    }

    @Override
    public ITypeBinding getComponentType() {
        return null;
    }

    @Override
    public IVariableBinding[] getDeclaredFields() {
        return new IVariableBinding[0];
    }

    @Override
    public IMethodBinding[] getDeclaredMethods() {
        if (binType instanceof BinaryTypeBinding) {
            BinaryTypeBinding binaryType = (BinaryTypeBinding) binType;
            IMethodBinding[] methods = new IMethodBinding[binaryType.methods().length];
            for (int i = 0; i < binaryType.methods().length; i++) {
                methods[i] = new BinMethodBinding(binaryType.methods()[i]);
            }
            return methods;
        }
        return new IMethodBinding[0];
    }

    /**
     * @deprecated
     */
    @Override
    public int getDeclaredModifiers() {
        return 0;
    }

    @Override
    public ITypeBinding[] getDeclaredTypes() {
        return new ITypeBinding[0];
    }

    @Override
    public ITypeBinding getDeclaringClass() {
        return null;
    }

    @Override
    public IMethodBinding getDeclaringMethod() {
        return null;
    }

    @Override
    public IBinding getDeclaringMember() {
        return null;
    }

    @Override
    public int getDimensions() {
        return 0;
    }

    @Override
    public ITypeBinding getElementType() {
        return null;
    }

    @Override
    public ITypeBinding getErasure() {
        return null;
    }

    @Override
    public IMethodBinding getFunctionalInterfaceMethod() {
        return null;
    }

    @Override
    public ITypeBinding[] getInterfaces() {
        return new ITypeBinding[0];
    }

    @Override
    public int getModifiers() {
        return 0;
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }

    @Override
    public boolean isRecovered() {
        return false;
    }

    @Override
    public boolean isSynthetic() {
        return false;
    }

    @Override
    public IJavaElement getJavaElement() {
        return null;
    }

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public boolean isEqualTo(IBinding iBinding) {
        return false;
    }

    @Override
    public IAnnotationBinding[] getAnnotations() {
        return new IAnnotationBinding[0];
    }

    @Override
    public int getKind() {
        return 0;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public IPackageBinding getPackage() {
        return null;
    }

    @Override
    public String getQualifiedName() {
        return null;
    }

    @Override
    public ITypeBinding getSuperclass() {
        return null;
    }

    @Override
    public IAnnotationBinding[] getTypeAnnotations() {
        return new IAnnotationBinding[0];
    }

    @Override
    public ITypeBinding[] getTypeArguments() {
        return new ITypeBinding[0];
    }

    @Override
    public ITypeBinding[] getTypeBounds() {
        return new ITypeBinding[0];
    }

    @Override
    public ITypeBinding getTypeDeclaration() {
        return null;
    }

    @Override
    public ITypeBinding[] getTypeParameters() {
        return new ITypeBinding[0];
    }

    @Override
    public ITypeBinding getWildcard() {
        return null;
    }

    @Override
    public boolean isAnnotation() {
        return false;
    }

    @Override
    public boolean isAnonymous() {
        return false;
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public boolean isAssignmentCompatible(ITypeBinding iTypeBinding) {
        if (String.valueOf(binType.sourceName()).equals(iTypeBinding.getName())) return true;
        return false;
    }

    @Override
    public boolean isCapture() {
        return false;
    }

    @Override
    public boolean isCastCompatible(ITypeBinding iTypeBinding) {
        return false;
    }

    @Override
    public boolean isClass() {
        return false;
    }

    @Override
    public boolean isEnum() {
        return false;
    }

    @Override
    public boolean isRecord() {
        return false;
    }

    @Override
    public boolean isFromSource() {
        return false;
    }

    @Override
    public boolean isGenericType() {
        return false;
    }

    @Override
    public boolean isInterface() {
        return false;
    }

    @Override
    public boolean isIntersectionType() {
        return false;
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public boolean isMember() {
        return false;
    }

    @Override
    public boolean isNested() {
        return false;
    }

    @Override
    public boolean isNullType() {
        return false;
    }

    @Override
    public boolean isParameterizedType() {
        return false;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean isRawType() {
        return false;
    }

    @Override
    public boolean isSubTypeCompatible(ITypeBinding iTypeBinding) {
        return false;
    }

    @Override
    public boolean isTopLevel() {
        return false;
    }

    @Override
    public boolean isTypeVariable() {
        return false;
    }

    @Override
    public boolean isUpperbound() {
        return false;
    }

    @Override
    public boolean isWildcardType() {
        return false;
    }
}
