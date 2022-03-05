package flute.data.type;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.*;

public class CustomVariableBinding implements IVariableBinding {
    private int modifier = 0;
    private String name = "";
    private ITypeBinding type, declareType = null;

    public CustomVariableBinding(int modifier, String name, ITypeBinding type, ITypeBinding declareType) {
        this.modifier = modifier;
        this.name = name;
        this.type = type;
        this.declareType = declareType;
    }

    /**
     * Returns whether this binding is for a field.
     * Note that this method returns <code>true</code> for constants,
     * including enum constants. This method returns <code>false</code>
     * for local variables.
     *
     * @return <code>true</code> if this is the binding for a field,
     * and <code>false</code> otherwise
     */
    @Override
    public boolean isField() {
        return false;
    }

    /**
     * Returns whether this binding is for an enum constant.
     * Note that this method returns <code>false</code> for local variables
     * and for fields other than enum constants.
     *
     * @return <code>true</code> if this is the binding for an enum constant,
     * and <code>false</code> otherwise
     * @since 3.1
     */
    @Override
    public boolean isEnumConstant() {
        return false;
    }

    /**
     * Returns whether this binding corresponds to a parameter.
     *
     * @return <code>true</code> if this is the binding for a parameter,
     * and <code>false</code> otherwise
     * @since 3.2
     */
    @Override
    public boolean isParameter() {
        return false;
    }

    /**
     * Returns the resolved declaration annotations associated with this binding.
     * <ul>
     * <li>Package bindings - these are annotations on a package declaration.
     * </li>
     * <li>Type bindings - these are annotations on a class, interface, enum,
     * or annotation type declaration. The result is the same regardless of
     * whether the type is parameterized.</li>
     * <li>Method bindings - these are annotations on a method or constructor
     * declaration. The result is the same regardless of whether the method is
     * parameterized.</li>
     * <li>Variable bindings - these are annotations on a field, enum constant,
     * or formal parameter declaration.</li>
     * <li>Annotation bindings - an empty array is always returned</li>
     * <li>Member value pair bindings - an empty array is always returned</li>
     * </ul>
     * <p>
     * <b>Note:</b> This method only returns declaration annotations.
     * <em>Type annotations</em> in the sense of JLS8 9.7.4 are <em>not</em> returned.
     * Type annotations can be retrieved via {@link ITypeBinding#getTypeAnnotations()}.
     * </p>
     *
     * @return the list of resolved declaration annotations, or the empty list if there are no
     * declaration annotations associated with the entity represented by this binding
     * @since 3.2
     */
    @Override
    public IAnnotationBinding[] getAnnotations() {
        return new IAnnotationBinding[0];
    }

    /**
     * Returns the kind of bindings this is. That is one of the kind constants:
     * <code>PACKAGE</code>,
     * <code>TYPE</code>,
     * <code>VARIABLE</code>,
     * <code>METHOD</code>,
     * <code>ANNOTATION</code>,
     * <code>MEMBER_VALUE_PAIR</code>, or
     * <code>MODULE</code>.
     * <p>
     * Note that additional kinds might be added in the
     * future, so clients should not assume this list is exhaustive and
     * should program defensively, e.g. by having a reasonable default
     * in a switch statement.
     * </p>
     *
     * @return one of the kind constants
     */
    @Override
    public int getKind() {
        return 0;
    }

    /**
     * Returns the name of the field or local variable declared in this binding.
     * The name is always a simple identifier.
     *
     * @return the name of this field or local variable
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the modifiers for this binding.
     * <p>
     * Note that 'deprecated' is not included among the modifiers.
     * Use <code>isDeprecated</code> to find out whether a binding is deprecated.
     * </p>
     *
     * @return the bit-wise or of <code>Modifier</code> constants
     * @see Modifier
     */
    @Override
    public int getModifiers() {
        return modifier;
    }

    /**
     * Return whether this binding is for something that is deprecated.
     * A deprecated class, interface, field, method, or constructor is one that
     * is marked with the 'deprecated' tag in its Javadoc comment.
     *
     * @return <code>true</code> if this binding is deprecated, and
     * <code>false</code> otherwise
     */
    @Override
    public boolean isDeprecated() {
        return false;
    }

    /**
     * Return whether this binding is created because the bindings recovery is enabled. This binding is considered
     * to be incomplete. Its internal state might be incomplete.
     *
     * @return <code>true</code> if this binding is a recovered binding, and
     * <code>false</code> otherwise
     * @since 3.3
     */
    @Override
    public boolean isRecovered() {
        return false;
    }

    /**
     * Returns whether this binding is synthetic. A synthetic binding is one that
     * was made up by the compiler, rather than something declared in the
     * source code. Note that default constructors (the 0-argument constructor that
     * the compiler generates for class declarations with no explicit constructors
     * declarations) are not generally considered synthetic (although they
     * may be if the class itself is synthetic).
     * But see {@link IMethodBinding#isDefaultConstructor() IMethodBinding.isDefaultConstructor}
     * for cases where the compiled-generated default constructor can be recognized
     * instead.
     *
     * @return <code>true</code> if this binding is synthetic, and
     * <code>false</code> otherwise
     * @see IMethodBinding#isDefaultConstructor()
     */
    @Override
    public boolean isSynthetic() {
        return false;
    }

    /**
     * Returns the Java element that corresponds to this binding.
     * Returns <code>null</code> if this binding has no corresponding
     * Java element.
     * <p>
     * For array types, this method returns the Java element that corresponds
     * to the array's element type. For raw and parameterized types, this method
     * returns the Java element of the erasure. For annotations, this method
     * returns the Java element of the annotation (i.e. an {@link IAnnotation}).
     * </p>
     * <p>
     * Here are the cases where a <code>null</code> should be expected:
     * <ul>
     * <li>primitive types, including void</li>
     * <li>null type</li>
     * <li>wildcard types</li>
     * <li>capture types</li>
     * <li>array types of any of the above</li>
     * <li>the "length" field of an array type</li>
     * <li>the default constructor of a source class</li>
     * <li>the constructor of an anonymous class</li>
     * <li>member value pairs</li>
     * <li>synthetic bindings</li>
     * <li>problem package bindings (since Java 9)</li>
     * </ul>
     * <p>
     * For all other kind of type, method, variable, annotation and package bindings,
     * this method returns non-<code>null</code>.
     * </p>
     *
     * @return the Java element that corresponds to this binding,
     * or <code>null</code> if none
     * @since 3.1
     */
    @Override
    public IJavaElement getJavaElement() {
        return null;
    }

    /**
     * Returns the key for this binding.
     * <p>
     * Within a single cluster of bindings (produced by the same call to an
     * {@code ASTParser#create*(*)} method)), each binding has a distinct key.
     * The keys are generated in a manner that is predictable and as
     * stable as possible. This last property makes these keys useful for
     * comparing bindings between different clusters of bindings (for example,
     * the bindings between the "before" and "after" ASTs of the same
     * compilation unit).
     * </p>
     * <p>
     * The exact details of how the keys are generated is unspecified.
     * However, it is a function of the following information:
     * <ul>
     * <li>packages - the name of the package (for an unnamed package,
     *   some internal id)</li>
     * <li>classes or interfaces - the VM name of the type and the key
     *   of its package</li>
     * <li>array types - the key of the component type and number of
     *   dimensions</li>
     * <li>primitive types - the name of the primitive type</li>
     * <li>fields - the name of the field and the key of its declaring
     *   type</li>
     * <li>methods - the name of the method, the key of its declaring
     *   type, and the keys of the parameter types</li>
     * <li>constructors - the key of its declaring class, and the
     *   keys of the parameter types</li>
     * <li>local variables - the name of the local variable, the index of the
     *   declaring block relative to its parent, the key of its method</li>
     * <li>local types - the name of the type, the index of the declaring
     *   block relative to its parent, the key of its method</li>
     * <li>anonymous types - the occurrence count of the anonymous
     *   type relative to its declaring type, the key of its declaring type</li>
     * <li>enum types - treated like classes</li>
     * <li>annotation types - treated like interfaces</li>
     * <li>type variables - the name of the type variable and
     * the key of the generic type or generic method that declares that
     * type variable</li>
     * <li>wildcard types - the key of the optional wildcard type bound</li>
     * <li>capture type bindings - the key of the wildcard captured</li>
     * <li>generic type instances - the key of the generic type and the keys
     * of the type arguments used to instantiate it, and whether the
     * instance is explicit (a parameterized type reference) or
     * implicit (a raw type reference)</li>
     * <li>generic method instances - the key of the generic method and the keys
     * of the type arguments used to instantiate it, and whether the
     * instance is explicit (a parameterized method reference) or
     * implicit (a raw method reference)</li>
     * <li>members of generic type instances - the key of the generic type
     * instance and the key of the corresponding member in the generic
     * type</li>
     * <li>annotations - the key of the annotated element and the key of
     * the annotation type</li>
     * </ul>
     * <p>
     * The key for a type binding does <em>not</em> contain {@link ITypeBinding#getTypeAnnotations() type annotations},
     * so type bindings with different type annotations may have the same key (iff they denote the same un-annotated type).
     * By construction, this also applies to method bindings if their declaring types contain type annotations.
     * </p>
     * <p>Note that the key for member value pair bindings is
     * not yet implemented. This method returns <code>null</code> for that kind of bindings.<br>
     * Recovered bindings have a unique key.
     * </p>
     *
     * @return the key for this binding
     */
    @Override
    public String getKey() {
        return null;
    }

    /**
     * Returns whether this binding has the same key as that of the given
     * binding. Within the context of a single cluster of bindings
     * (produced by the same call to an {@code ASTParser#create*(*)} method), each
     * binding is represented by a distinct object. However, between
     * different clusters of bindings, the binding objects may or may
     * not be different objects; in these cases, the binding keys
     * are used where available.
     *
     * <p>
     * Note that type bindings that only differ in their {@link ITypeBinding#getTypeAnnotations() type annotations}
     * have the same {@link IBinding#getKey() key}, and hence this method returns
     * <code>true</code> for such type bindings. By construction of the key, this also applies
     * to method bindings if their declaring types contain type annotations.
     * </p>
     *
     * @param binding the other binding, or <code>null</code>
     * @return <code>true</code> if the given binding is the identical
     * object as this binding, or if the keys of both bindings are the
     * same string; <code>false</code> if the given binding is
     * <code>null</code>, or if the bindings do not have the same key,
     * or if one or both of the bindings have no key
     * @see #getKey()
     * @since 3.1
     */
    @Override
    public boolean isEqualTo(IBinding binding) {
        return false;
    }

    /**
     * Returns the type binding representing the class or interface
     * that declares this field.
     * <p>
     * The declaring class of a field is the class or interface of which it is
     * a member. Local variables have no declaring class. The field length of an
     * array type has no declaring class.
     * </p>
     *
     * @return the binding of the class or interface that declares this field,
     * or <code>null</code> if none
     */
    @Override
    public ITypeBinding getDeclaringClass() {
        return declareType;
    }

    /**
     * Returns the binding for the type of this field or local variable.
     *
     * @return the binding for the type of this field or local variable
     */
    @Override
    public ITypeBinding getType() {
        return type;
    }

    /**
     * Returns a small integer variable id for this variable binding.
     * <p>
     * <b>Local variables inside methods:</b> Local variables (and parameters)
     * declared within a single method are assigned ascending ids in normal
     * code reading order; var1.getVariableId()&lt;var2.getVariableId() means that var1 is
     * declared before var2.
     * </p>
     * <p>
     * <b>Local variables outside methods:</b> Local variables declared in a
     * type's static initializers (or initializer expressions of static fields)
     * are assigned ascending ids in normal code reading order. Local variables
     * declared in a type's instance initializers (or initializer expressions
     * of non-static fields) are assigned ascending ids in normal code reading
     * order. These ids are useful when checking definite assignment for
     * static initializers (JLS 16.7) and instance initializers (JLS 16.8),
     * respectively.
     * </p>
     * <p>
     * <b>Fields:</b> Fields declared as members of a type are assigned
     * ascending ids in normal code reading order;
     * field1.getVariableId()&lt;field2.getVariableId() means that field1 is declared before
     * field2.
     * </p>
     *
     * @return a small non-negative variable id
     */
    @Override
    public int getVariableId() {
        return 0;
    }

    /**
     * Returns this binding's constant value if it has one.
     * Some variables may have a value computed at compile-time. If the type of
     * the value is a primitive type, the result is the boxed equivalent (i.e.,
     * int returned as an <code>Integer</code>). If the type of the value is
     * <code>String</code>, the result is the string itself. If the variable has
     * no compile-time computed value, the result is <code>null</code>.
     * (Note: compile-time constant expressions cannot denote <code>null</code>;
     * JLS2 15.28.). The result is always <code>null</code> for enum constants.
     *
     * @return the constant value, or <code>null</code> if none
     * @since 3.0
     */
    @Override
    public Object getConstantValue() {
        return null;
    }

    /**
     * Returns the method binding representing the method containing the scope
     * in which this local variable is declared.
     * <p>
     * The declaring method of a method formal parameter is the method itself.
     * For a local variable declared somewhere within the body of a method,
     * the declaring method is the enclosing method. When local or anonymous
     * classes are involved, the declaring method is the innermost such method.
     * There is no declaring method for a field, or for a local variable
     * declared in a static or instance initializer; this method returns
     * <code>null</code> in those cases.
     * </p>
     *
     * @return the binding of the method or constructor that declares this
     * local variable, or <code>null</code> if none
     * @since 3.1
     */
    @Override
    public IMethodBinding getDeclaringMethod() {
        return null;
    }

    /**
     * Returns the binding for the variable declaration corresponding to this
     * variable binding. For a binding for a field declaration in an instance
     * of a generic type, this method returns the binding for the corresponding
     * field declaration in the generic type. For other variable bindings,
     * including all ones for local variables and parameters, this method
     * returns the same binding.
     *
     * @return the variable binding for the originating declaration
     * @since 3.1
     */
    @Override
    public IVariableBinding getVariableDeclaration() {
        return null;
    }

    /**
     * Returns whether this binding corresponds to an effectively final local
     * variable (JLS8 4.12.4). A variable is said to be effectively final if
     * it is not final and never assigned to after its initialization.
     *
     * @return <code>true</code> if this is an effectively final local variable
     * and <code>false</code> otherwise
     * @since 3.10
     */
    @Override
    public boolean isEffectivelyFinal() {
        return false;
    }
}
