package flute.data.typemodel;

import org.eclipse.jdt.core.dom.ITypeBinding;

public class Variable {
    private boolean isStatic = false;
    private ITypeBinding typeBinding;
    private String name;
    private boolean isField = false;
    private boolean isInitialized = false;
    private boolean isLocalVariable = false;
    private int localVariableLevel = 0;
    private int scopeDistance = -1;


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

    public boolean isInitialized() {
        return isInitialized;
    }

    public void setInitialized(boolean initialized) {
        isInitialized = initialized;
    }

    public boolean isLocalVariable() {
        return isLocalVariable;
    }

    public void setLocalVariable(boolean localVariable) {
        isLocalVariable = localVariable;
    }

    public boolean isField() {
        return isField;
    }

    public void setField(boolean field) {
        isField = field;
    }

    public int getLocalVariableLevel() {
        return localVariableLevel;
    }

    public void setLocalVariableLevel(int localVariableLevel) {
        this.localVariableLevel = localVariableLevel;
    }

    public int getScopeDistance() {
        return scopeDistance;
    }

    public void setScopeDistance(int scopeDistance) {
        this.scopeDistance = scopeDistance;
    }

    @Override
    public String toString() {
        return typeBinding.getName() + " " + name;
    }
}
