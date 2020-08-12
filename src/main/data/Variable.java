package data;

import org.eclipse.jdt.core.dom.ITypeBinding;

public class Variable {
    private boolean isStatic = false;
    private ITypeBinding typeBinding;
    private String name;

    public Variable(ITypeBinding typeBinding, String name) {
        this.typeBinding = typeBinding;
        this.name = name;
    }

    public ITypeBinding getTypeBinding() {
        return typeBinding;
    }

    public void setTypeBinding(ITypeBinding typeBinding) {
        this.typeBinding = typeBinding;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }

    @Override
    public String toString() {
        return typeBinding.getName() + " " + name;
    }
}
