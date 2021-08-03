package flute.data.typemodel.method;

import flute.data.typemodel.BinType;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;

public class BinMethodBinding implements IMethodBinding {
    private MethodBinding binMethodBinding;

    public BinMethodBinding(MethodBinding binMethodBinding) {
        this.binMethodBinding = binMethodBinding;
    }

    @Override
    public boolean isConstructor() {
        return false;
    }

    @Override
    public boolean isCompactConstructor() {
        return false;
    }

    @Override
    public boolean isCanonicalConstructor() {
        return false;
    }

    @Override
    public boolean isDefaultConstructor() {
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
        return String.valueOf(binMethodBinding.readableName())
                .replaceAll("\\s*\\([^\\)]*\\)\\s*", "");
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
    public ITypeBinding getDeclaringClass() {
        return null;
    }

    @Override
    public IBinding getDeclaringMember() {
        return null;
    }

    @Override
    public Object getDefaultValue() {
        return null;
    }

    @Override
    public IAnnotationBinding[] getParameterAnnotations(int i) {
        return new IAnnotationBinding[0];
    }

    @Override
    public ITypeBinding[] getParameterTypes() {
        return new ITypeBinding[0];
    }

    @Override
    public ITypeBinding getDeclaredReceiverType() {
        return null;
    }

    @Override
    public ITypeBinding getReturnType() {
        return new BinType(binMethodBinding.returnType);
    }

    @Override
    public ITypeBinding[] getExceptionTypes() {
        return new ITypeBinding[0];
    }

    @Override
    public ITypeBinding[] getTypeParameters() {
        return new ITypeBinding[0];
    }

    @Override
    public boolean isAnnotationMember() {
        return false;
    }

    @Override
    public boolean isGenericMethod() {
        return false;
    }

    @Override
    public boolean isParameterizedMethod() {
        return false;
    }

    @Override
    public ITypeBinding[] getTypeArguments() {
        return new ITypeBinding[0];
    }

    @Override
    public IMethodBinding getMethodDeclaration() {
        return null;
    }

    @Override
    public boolean isRawMethod() {
        return false;
    }

    @Override
    public boolean isSubsignature(IMethodBinding iMethodBinding) {
        return false;
    }

    @Override
    public boolean isVarargs() {
        return false;
    }

    @Override
    public boolean overrides(IMethodBinding iMethodBinding) {
        return false;
    }

    @Override
    public IVariableBinding[] getSyntheticOuterLocals() {
        return new IVariableBinding[0];
    }

    @Override
    public boolean isSyntheticRecordMethod() {
        return false;
    }
}
