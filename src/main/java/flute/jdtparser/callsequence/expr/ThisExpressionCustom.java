package flute.jdtparser.callsequence.expr;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.ArrayList;
import java.util.List;

public class ThisExpressionCustom implements IBinding {
    public ITypeBinding declaringClass;

    public static List<ThisExpressionCustom> listThis = new ArrayList<>();

    public static ThisExpressionCustom create(ITypeBinding declaringClass) {
        for (ThisExpressionCustom thisItem : listThis) {
            if (declaringClass == thisItem.getDeclaringClass()) {
                return thisItem;
            }
        }
        ThisExpressionCustom newThis = new ThisExpressionCustom(declaringClass);
        listThis.add(newThis);
        return newThis;
    }

    public ThisExpressionCustom(ITypeBinding declaringClass) {
        this.declaringClass = declaringClass;
    }

    public ITypeBinding getDeclaringClass() {
        return declaringClass;
    }

    public static void gc(){
        listThis.clear();
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
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public boolean isEqualTo(IBinding iBinding) {
        return false;
    }

    @Override
    public String toString() {
        return declaringClass.toString();
    }
}
