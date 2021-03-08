package flute.jdtparser.callsequence.expr;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.Objects;

public class ThisExpressionCustom implements IBinding {
    private ITypeBinding declaringClass;

    public ThisExpressionCustom(ITypeBinding declaringClass) {
        this.declaringClass = declaringClass;
    }

    public ITypeBinding getDeclaringClass() {
        return declaringClass;
    }

    public void setDeclaringClass(ITypeBinding declaringClass) {
        this.declaringClass = declaringClass;
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
    public String toString() {
        return declaringClass.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThisExpressionCustom that = (ThisExpressionCustom) o;
        return Objects.equals(declaringClass, that.declaringClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(declaringClass);
    }
}
