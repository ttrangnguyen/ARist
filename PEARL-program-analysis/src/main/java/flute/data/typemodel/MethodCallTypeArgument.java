package flute.data.typemodel;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

public class MethodCallTypeArgument {
    private IMethodBinding methodBinding;
    private ITypeBinding expressionType;

    public MethodCallTypeArgument(ITypeBinding expressionType, IMethodBinding methodBinding) {
        this.methodBinding = methodBinding;
        this.expressionType = expressionType;
    }

    public IMethodBinding getMethodBinding() {
        return methodBinding;
    }

    public ITypeBinding getExpressionType() {
        return expressionType;
    }
}
