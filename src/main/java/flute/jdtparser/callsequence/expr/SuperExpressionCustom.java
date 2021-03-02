package flute.jdtparser.callsequence.expr;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.ArrayList;
import java.util.List;

public class SuperExpressionCustom implements IBinding {
    public ITypeBinding declaringClass;

    public static List<SuperExpressionCustom> listSuper = new ArrayList<>();

    public static SuperExpressionCustom create(ITypeBinding declaringClass) {
        for (SuperExpressionCustom superItem : listSuper) {
            if (declaringClass == superItem.getDeclaringClass()) {
                return superItem;
            }
        }
        SuperExpressionCustom newSuper = new SuperExpressionCustom(declaringClass);
        listSuper.add(newSuper);
        return newSuper;
    }

    public SuperExpressionCustom(ITypeBinding declaringClass) {
        this.declaringClass = declaringClass;
    }

    public static void gc(){
        listSuper.clear();
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
