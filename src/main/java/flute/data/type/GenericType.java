package flute.data.type;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.*;

public class GenericType implements ITypeBinding {
    public boolean canBeAssignmentBy(ITypeBinding iTypeBinding) {
        return false;
    }

    /**
     * Answer an array type binding using the receiver and the given dimension.
     *
     * <p>If the receiver is an array binding, then the resulting dimension is the given dimension
     * plus the dimension of the receiver. Otherwise the resulting dimension is the given
     * dimension.</p>
     *
     * @param dimension the given dimension
     * @return an array type binding
     * @throws IllegalArgumentException:<ul> <li>if the receiver represents the void type</li>
     *                                       <li>if the resulting dimensions is lower than one or greater than 255</li>
     *                                       </ul>
     * @since 3.3
     */
    @Override
    public ITypeBinding createArrayType(int dimension) {
        return null;
    }

    /**
     * Returns the binary name of this type binding.
     * The binary name of a class is defined in the Java Language
     * Specification 3rd edition, section 13.1.
     * <p>
     * Note that in some cases, the binary name may be unavailable.
     * This may happen, for example, for a local type declared in
     * unreachable code.
     * </p>
     *
     * @return the binary name of this type, or <code>null</code>
     * if the binary name is unknown
     * @since 3.0
     */
    @Override
    public String getBinaryName() {
        return null;
    }

    /**
     * Returns the bound of this wildcard type if it has one.
     * Returns <code>null</code> if this is not a wildcard type.
     *
     * @return the bound of this wildcard type, or <code>null</code> if none
     * @see #isWildcardType()
     * @see #isUpperbound()
     * @see #getTypeBounds()
     * @since 3.1
     */
    @Override
    public ITypeBinding getBound() {
        return null;
    }

    /**
     * Returns the generic type associated with this wildcard type, if it has one.
     * Returns <code>null</code> if this is not a wildcard type.
     *
     * @return the generic type associated with this wildcard type, or <code>null</code> if none
     * @see #isWildcardType()
     * @since 3.5
     */
    @Override
    public ITypeBinding getGenericTypeOfWildcardType() {
        return null;
    }

    /**
     * Returns the rank associated with this wildcard type. The rank of this wild card type is the relative
     * position of the wild card type in the parameterization of the associated generic type.
     * Returns <code>-1</code> if this is not a wildcard type.
     *
     * @return the rank associated with this wildcard type, or <code>-1</code> if none
     * @see #isWildcardType()
     * @since 3.5
     */
    @Override
    public int getRank() {
        return 0;
    }

    /**
     * Returns the binding representing the component type of this array type,
     * or <code>null</code> if this is not an array type binding. The component
     * type of an array might be an array type (with one dimension less than
     * this array type).
     *
     * @return the component type binding, or <code>null</code> if this is
     * not an array type
     * @see #getElementType()
     * @since 3.2
     */
    @Override
    public ITypeBinding getComponentType() {
        return null;
    }

    /**
     * Returns a list of bindings representing all the fields declared
     * as members of this class, interface, or enum type.
     *
     * <p>These include public, protected, default (package-private) access,
     * and private fields declared by the class, but excludes inherited fields.
     * Synthetic fields may or may not be included. Fields from binary types that
     * reference unresolved types may not be included.</p>
     *
     * <p>Returns an empty list if the class, interface, or enum declares no fields,
     * and for other kinds of type bindings that do not directly have members.</p>
     *
     * <p>The resulting bindings are in no particular order.</p>
     *
     * @return the list of bindings for the field members of this type,
     * or the empty list if this type does not have field members
     */
    @Override
    public IVariableBinding[] getDeclaredFields() {
        return new IVariableBinding[0];
    }

    /**
     * Returns a list of method bindings representing all the methods and
     * constructors declared for this class, interface, enum, or annotation
     * type.
     * <p>These include public, protected, default (package-private) access,
     * and private methods Synthetic methods and constructors may or may not be
     * included. Returns an empty list if the class, interface, or enum,
     * type declares no methods or constructors, if the annotation type declares
     * no members, or if this type binding represents some other kind of type
     * binding. Methods from binary types that reference unresolved types may
     * not be included.</p>
     * <p>The resulting bindings are in no particular order.</p>
     *
     * @return the list of method bindings for the methods and constructors
     * declared by this class, interface, enum type, or annotation type,
     * or the empty list if this type does not declare any methods or constructors
     */
    @Override
    public IMethodBinding[] getDeclaredMethods() {
        return new IMethodBinding[0];
    }

    /**
     * Returns the declared modifiers for this class or interface binding
     * as specified in the original source declaration of the class or
     * interface. The result may not correspond to the modifiers in the compiled
     * binary, since the compiler may change them (in particular, for inner
     * class emulation). The <code>getModifiers</code> method should be used if
     * the compiled modifiers are needed. Returns -1 if this type does not
     * represent a class or interface.
     *
     * @return the bit-wise or of <code>Modifier</code> constants
     * @see #getModifiers()
     * @see Modifier
     * @deprecated Use {@link #getModifiers()} instead.
     * This method was never implemented properly and historically has simply
     * delegated to the method <code>getModifiers</code>. Clients should call
     * <code>getModifiers</code> method directly.
     */
    @Override
    public int getDeclaredModifiers() {
        return 0;
    }

    /**
     * Returns a list of type bindings representing all the types declared as
     * members of this class, interface, or enum type.
     * These include public, protected, default (package-private) access,
     * and private classes, interfaces, enum types, and annotation types
     * declared by the type, but excludes inherited types. Returns an empty
     * list if the type declares no type members, or if this type
     * binding represents an array type, a primitive type, a type variable,
     * a wildcard type, a capture, or the null type.
     * The resulting bindings are in no particular order.
     *
     * @return the list of type bindings for the member types of this type,
     * or the empty list if this type does not have member types
     */
    @Override
    public ITypeBinding[] getDeclaredTypes() {
        return new ITypeBinding[0];
    }

    /**
     * Returns the type binding representing the class, interface, or enum
     * that declares this binding.
     * <p>
     * The declaring class of a member class, interface, enum, annotation
     * type is the class, interface, or enum type of which it is a member.
     * The declaring class of a local class or interface (including anonymous
     * classes) is the innermost class or interface containing the expression
     * or statement in which this type is declared.
     * </p>
     * <p>The declaring class of a type variable is the class in which the type
     * variable is declared if it is declared on a type. It returns
     * <code>null</code> otherwise.
     * </p>
     * <p>The declaring class of a capture binding is the innermost class or
     * interface containing the expression or statement in which this capture is
     * declared.
     * </p>
     * <p>Array types, primitive types, the null type, top-level types,
     * wildcard types, recovered binding have no declaring class.
     * </p>
     *
     * @return the binding of the type that declares this type, or
     * <code>null</code> if none
     */
    @Override
    public ITypeBinding getDeclaringClass() {
        return null;
    }

    /**
     * Returns the method binding representing the method that declares this binding
     * of a local type or type variable.
     * <p>
     * The declaring method of a local class or interface (including anonymous
     * classes) is the innermost method containing the expression or statement in
     * which this type is declared. Returns <code>null</code> if the type
     * is declared in an initializer.
     * </p>
     * <p>
     * The declaring method of a type variable is the method in which the type
     * variable is declared if it is declared on a method. It
     * returns <code>null</code> otherwise.
     * </p>
     * <p>Array types, primitive types, the null type, top-level types,
     * wildcard types, capture bindings, and recovered binding have no
     * declaring method.
     * </p>
     *
     * @return the binding of the method that declares this type, or
     * <code>null</code> if none
     * @since 3.1
     */
    @Override
    public IMethodBinding getDeclaringMethod() {
        return null;
    }

    /**
     * If this type binding represents a local type, possibly an anonymous class, then:
     * <ul>
     * <li>If the local type is declared in the body of a method,
     *   answers the binding of that declaring method.
     * </li>
     * <li>Otherwise, if the local type (an anonymous class in this case) is declared
     *   in the initializer of a field, answers the binding of that declaring field.
     * </li>
     * <li>Otherwise, if the local type is declared in a static initializer or
     *   an instance initializer, a method binding is returned to represent that initializer
     *   (selector is an empty string in this case).
     * </li>
     * </ul>
     * <p>
     * If this type binding does not represent a local type, <code>null</code> is returned.
     * </p>
     *
     * @return a method binding or field binding representing the member that
     * contains the local type represented by this type binding,
     * or null for non-local type bindings.
     * @since 3.11
     */
    @Override
    public IBinding getDeclaringMember() {
        return null;
    }

    /**
     * Returns the dimensionality of this array type, or <code>0</code> if this
     * is not an array type binding.
     *
     * @return the number of dimension of this array type binding, or
     * <code>0</code> if this is not an array type
     */
    @Override
    public int getDimensions() {
        return 0;
    }

    /**
     * Returns the binding representing the element type of this array type,
     * or <code>null</code> if this is not an array type binding. The element
     * type of an array type is never itself an array type.
     * <p>
     * To get the type annotations on dimensions, clients should repeatedly
     * call getComponentType() and get the type annotations from there.
     *
     * @return the element type binding, or <code>null</code> if this is
     * not an array type
     */
    @Override
    public ITypeBinding getElementType() {
        return null;
    }

    /**
     * Returns the erasure of this type binding.
     * <ul>
     * <li>For parameterized types ({@link #isParameterizedType()})
     * - returns the binding for the corresponding generic type.</li>
     * <li>For raw types ({@link #isRawType()})
     * - returns the binding for the corresponding generic type.</li>
     * <li>For wildcard types ({@link #isWildcardType()})
     * - returns the binding for the upper bound if it has one and
     * java.lang.Object in other cases.</li>
     * <li>For type variables ({@link #isTypeVariable()})
     * - returns the binding for the erasure of the leftmost bound
     * if it has bounds and java.lang.Object if it does not.</li>
     * <li>For captures ({@link #isCapture()})
     * - returns the binding for the erasure of the leftmost bound
     * if it has bounds and java.lang.Object if it does not.</li>
     * <li>For array types ({@link #isArray()}) - returns an array type of
     * the same dimension ({@link #getDimensions()}) as this type
     * binding for which the element type is the erasure of the element type
     * ({@link #getElementType()}) of this type binding.</li>
     * <li>For all other type bindings - returns the identical binding.</li>
     * </ul>
     *
     * @return the erasure type binding
     * @since 3.1
     */
    @Override
    public ITypeBinding getErasure() {
        return null;
    }

    /**
     * Returns the single abstract method that constitutes the single function
     * contract (aside from any redeclarations of methods of <code>java.lang.Object</code>)
     * of the receiver interface type or <code>null</code> if there is no such contract or if the receiver
     * is not an interface.
     * <p>
     * The returned method binding may be synthetic and its {@link #getDeclaringClass() declaring type}
     * may be a super interface type of this type binding.
     * </p>
     *
     * @return the single abstract method that represents the single function contract, or
     * <code>null</code> if the receiver is not a functional interface type
     * @since 3.10
     */
    @Override
    public IMethodBinding getFunctionalInterfaceMethod() {
        return null;
    }

    /**
     * Returns a list of type bindings representing the direct superinterfaces
     * of the class, interface, or enum type represented by this type binding.
     * <p>
     * If this type binding represents a class or enum type, the return value
     * is an array containing type bindings representing all interfaces
     * directly implemented by this class. The number and order of the interface
     * objects in the array corresponds to the number and order of the interface
     * names in the <code>implements</code> clause of the original declaration
     * of this type.
     * </p>
     * <p>
     * If this type binding represents an interface, the array contains
     * type bindings representing all interfaces directly extended by this
     * interface. The number and order of the interface objects in the array
     * corresponds to the number and order of the interface names in the
     * <code>extends</code> clause of the original declaration of this interface.
     * </p>
     * <p>
     * If the class or enum implements no interfaces, or the interface extends
     * no interfaces, or if this type binding represents an array type, a
     * primitive type, the null type, a type variable, an annotation type,
     * a wildcard type, or a capture binding, this method returns an array of
     * length 0.
     * </p>
     *
     * @return the list of type bindings for the interfaces extended by this
     * class or enum, or interfaces extended by this interface, or otherwise
     * the empty list
     */
    @Override
    public ITypeBinding[] getInterfaces() {
        return new ITypeBinding[0];
    }

    /**
     * Returns the compiled modifiers for this class, interface, enum,
     * or annotation type binding.
     * The result may not correspond to the modifiers as declared in the
     * original source, since the compiler may change them (in particular,
     * for inner class emulation).
     * Returns 0 if this type does not represent a class, an interface, an enum, an annotation
     * type or a recovered type.
     *
     * @return the compiled modifiers for this type binding or 0
     * if this type does not represent a class, an interface, an enum, an annotation
     * type or a recovered type.
     */
    @Override
    public int getModifiers() {
        return 0;
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
     * There is no special definition of equality for bindings; equality is
     * simply object identity.  Within the context of a single cluster of
     * bindings (produced by the same call to an {@code ASTParser#create*(*)} method),
     * each binding is represented by a separate object. However,
     * between different clusters of bindings, the binding objects may or may
     * not be different; in these cases, the client should compare bindings
     * using {@link #isEqualTo(IBinding)}, which is functionally equivalent to
     * checking their keys for equality.
     * <p>
     * Since JLS8, type bindings can contain {@link ITypeBinding#getTypeAnnotations() type annotations}.
     * Note that type bindings that denote the same un-annotated type have the same {@link #getKey() key},
     * but they are not identical if they contain different type annotations.
     * Type bindings that contain the same type annotations may or may not be identical.
     * </p>
     *
     * @param obj {@inheritDoc}
     * @return {@inheritDoc}
     * @see ITypeBinding#getTypeDeclaration()
     */
    @Override
    public boolean equals(Object obj) {
        return false;
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
     * Returns a string representation of this binding suitable for debugging
     * purposes only.
     *
     * @return a debug string
     */
    @Override
    public String toString() {
        return null;
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
     * Returns the unqualified name of the type represented by this binding
     * if it has one.
     * <ul>
     * <li>For top-level types, member types, and local types,
     * the name is the simple name of the type.
     * Example: <code>"String"</code> or <code>"Collection"</code>.
     * Note that the type parameters of a generic type are not included.</li>
     * <li>For primitive types, the name is the keyword for the primitive type.
     * Example: <code>"int"</code>.</li>
     * <li>For the null type, the name is the string "null".</li>
     * <li>For anonymous classes, which do not have a name,
     * this method returns an empty string.</li>
     * <li>For array types, the name is the unqualified name of the component
     * type (as computed by this method) followed by "[]".
     * Example: <code>"String[]"</code>. Note that the component type is never an
     * an anonymous class.</li>
     * <li>For type variables, the name is just the simple name of the
     * type variable (type bounds are not included).
     * Example: <code>"X"</code>.</li>
     * <li>For type bindings that correspond to particular instances of a generic
     * type arising from a parameterized type reference,
     * the name is the unqualified name of the erasure type (as computed by this method)
     * followed by the names (again, as computed by this method) of the type arguments
     * surrounded by "&lt;&gt;" and separated by ",".
     * Example: <code>"Collection&lt;String&gt;"</code>.
     * </li>
     * <li>For type bindings that correspond to particular instances of a generic
     * type arising from a raw type reference, the name is the unqualified name of
     * the erasure type (as computed by this method).
     * Example: <code>"Collection"</code>.</li>
     * <li>For wildcard types, the name is "?" optionally followed by
     * a single space followed by the keyword "extends" or "super"
     * followed a single space followed by the name of the bound (as computed by
     * this method) when present.
     * Example: <code>"? extends InputStream"</code>.
     * </li>
     * <li>Capture types do not have a name. For these types,
     * and array types thereof, this method returns an empty string.</li>
     * </ul>
     *
     * @return the unqualified name of the type represented by this binding,
     * or the empty string if it has none
     * @see #getQualifiedName()
     */
    @Override
    public String getName() {
        return null;
    }

    /**
     * Returns the binding for the package in which this type is declared.
     *
     * <p>The package of a recovered type reference binding is either
     * the package of the enclosing type, or, if the type name is the name of a
     * {@linkplain AST#resolveWellKnownType(String) well-known type},
     * the package of the matching well-known type.</p>
     *
     * @return the binding for the package in which this class, interface,
     * enum, or annotation type is declared, or <code>null</code> if this type
     * binding represents a primitive type, an array type, the null type,
     * a type variable, a wildcard type, a capture binding.
     */
    @Override
    public IPackageBinding getPackage() {
        return null;
    }

    /**
     * Returns the fully qualified name of the type represented by this
     * binding if it has one.
     * <ul>
     * <li>For top-level types, the fully qualified name is the simple name of
     * the type preceded by the package name (or unqualified if in a default package)
     * and a ".".
     * Example: <code>"java.lang.String"</code> or <code>"java.util.Collection"</code>.
     * Note that the type parameters of a generic type are not included.</li>
     * <li>For members of top-level types, the fully qualified name is the
     * simple name of the type preceded by the fully qualified name of the
     * enclosing type (as computed by this method) and a ".".
     * Example: <code>"java.io.ObjectInputStream.GetField"</code>.
     * If the binding is for a member type that corresponds to a particular instance
     * of a generic type arising from a parameterized type reference, the simple
     * name of the type is followed by the fully qualified names of the type arguments
     * (as computed by this method) surrounded by "&lt;&gt;" and separated by ",".
     * Example: <code>"pkg.Outer.Inner&lt;java.lang.String&gt;"</code>.
     * </li>
     * <li>For primitive types, the fully qualified name is the keyword for
     * the primitive type.
     * Example: <code>"int"</code>.</li>
     * <li>For the null type, the fully qualified name is the string
     * "null".</li>
     * <li>Local types (including anonymous classes) and members of local
     * types do not have a fully qualified name. For these types, and array
     * types thereof, this method returns an empty string.</li>
     * <li>For array types whose component type has a fully qualified name,
     * the fully qualified name is the fully qualified name of the component
     * type (as computed by this method) followed by "[]".
     * Example: <code>"java.lang.String[]"</code>.</li>
     * <li>For type variables, the fully qualified name is just the name of the
     * type variable (type bounds are not included).
     * Example: <code>"X"</code>.</li>
     * <li>For type bindings that correspond to particular instances of a generic
     * type arising from a parameterized type reference,
     * the fully qualified name is the fully qualified name of the erasure
     * type followed by the fully qualified names of the type arguments surrounded by "&lt;&gt;" and separated by ",".
     * Example: <code>"java.util.Collection&lt;java.lang.String&gt;"</code>.
     * </li>
     * <li>For type bindings that correspond to particular instances of a generic
     * type arising from a raw type reference,
     * the fully qualified name is the fully qualified name of the erasure type.
     * Example: <code>"java.util.Collection"</code>. Note that the
     * the type parameters are omitted.</li>
     * <li>For wildcard types, the fully qualified name is "?" optionally followed by
     * a single space followed by the keyword "extends" or "super"
     * followed a single space followed by the fully qualified name of the bound
     * (as computed by this method) when present.
     * Example: <code>"? extends java.io.InputStream"</code>.
     * </li>
     * <li>Capture types do not have a fully qualified name. For these types,
     * and array types thereof, this method returns an empty string.</li>
     * </ul>
     *
     * @return the fully qualified name of the type represented by this
     * binding, or the empty string if it has none
     * @see #getName()
     * @since 2.1
     */
    @Override
    public String getQualifiedName() {
        return null;
    }

    /**
     * Returns the type binding for the superclass of the type represented
     * by this class binding.
     * <p>
     * If this type binding represents any class other than the class
     * <code>java.lang.Object</code>, then the type binding for the direct
     * superclass of this class is returned. If this type binding represents
     * the class <code>java.lang.Object</code>, then <code>null</code> is
     * returned.
     * <p>
     * Loops that ascend the class hierarchy need a suitable termination test.
     * Rather than test the superclass for <code>null</code>, it is more
     * transparent to check whether the class is <code>Object</code>, by
     * comparing whether the class binding is identical to
     * <code>ast.resolveWellKnownType("java.lang.Object")</code>.
     * </p>
     * <p>
     * If this type binding represents an interface, an array type, a
     * primitive type, the null type, a type variable, an enum type,
     * an annotation type, a wildcard type, or a capture binding then
     * <code>null</code> is returned.
     * </p>
     *
     * @return the superclass of the class represented by this type binding,
     * or <code>null</code> if none
     * @see AST#resolveWellKnownType(String)
     */
    @Override
    public ITypeBinding getSuperclass() {
        return null;
    }

    /**
     * Returns the type annotations that this type reference is annotated with. Since JLS8,
     * multiple instances of type bindings may be created if they are annotated with
     * different type use annotations.
     * <p>
     * For example, the following three type references would produce three distinct type
     * bindings for java.lang.String that share the same key:
     * <ul>
     * <li>java.lang.@Marker1 String</li>
     * <li>java.lang.@Marker2 String</li>
     * <li>java.lang.String</li>
     * </ul>
     * To get the type annotations on dimensions, clients should repeatedly call
     * {@link #getComponentType()} and get the type annotations from there.
     *
     * @return type annotations specified on this type reference, or an empty array if
     * no type use annotations are found.
     * @see #getTypeDeclaration()
     * @see #getKey()
     * @since 3.10
     */
    @Override
    public IAnnotationBinding[] getTypeAnnotations() {
        return new IAnnotationBinding[0];
    }

    /**
     * Returns the type arguments of this generic type instance, or the
     * empty list for other type bindings.
     * <p>
     * Note that type arguments only occur on a type binding that represents
     * an instance of a generic type corresponding to a parameterized type
     * reference (e.g., <code>Collection&lt;String&gt;</code>).
     * Do not confuse these with type parameters which only occur on the
     * type binding corresponding directly to the declaration of the
     * generic class or interface (e.g., <code>Collection&lt;T&gt;</code>).
     * </p>
     *
     * @return the list of type bindings for the type arguments used to
     * instantiate the corresponding generic type, or otherwise the empty list
     * @see #getTypeDeclaration()
     * @see #isGenericType()
     * @see #isParameterizedType()
     * @see #isRawType()
     * @since 3.1
     */
    @Override
    public ITypeBinding[] getTypeArguments() {
        return new ITypeBinding[0];
    }

    /**
     * Returns the upper type bounds of this type variable, wildcard, capture, or intersectionType.
     * If the variable, wildcard, or capture had no explicit bound, then it returns an empty list.
     * <p>
     * Note that per construction, it can only contain one class or array type,
     * at most, and then it is located in first position.
     * </p>
     * <p>
     * Also note that array type bound may only occur in the case of a capture
     * binding, e.g. <code>capture-of ? extends Object[]</code>
     * </p>
     *
     * @return the list of upper bounds for this type variable, wildcard, capture, or intersection type
     * or otherwise the empty list
     * @see #isTypeVariable()
     * @see #isWildcardType()
     * @see #isCapture()
     * @see #isIntersectionType()
     * @since 3.1
     */
    @Override
    public ITypeBinding[] getTypeBounds() {
        return new ITypeBinding[0];
    }

    /**
     * Returns the binding for the type declaration corresponding to this type
     * binding.
     * <p>For parameterized types ({@link #isParameterizedType()})
     * and most raw types ({@link #isRawType()}), this method returns the binding
     * for the corresponding generic type ({@link #isGenericType()}.</p>
     * <p>For raw member types ({@link #isRawType()}, {@link #isMember()})
     * of a raw declaring class, the type declaration is a generic or a non-generic
     * type.</p>
     * <p>A different non-generic binding will be returned when one of the declaring
     * types/methods was parameterized.</p>
     * <p>For other type bindings, this method returns the binding for the type declaration
     * corresponding to this type binding. In particular, for type bindings that
     * contain a {@link #getTypeAnnotations() type annotation}, this method returns the binding for the type
     * declaration, which does not contain the type annotations from the use site.</p>
     *
     * @return the declaration type binding
     * @see #isEqualTo(IBinding)
     * @since 3.1
     */
    @Override
    public ITypeBinding getTypeDeclaration() {
        return null;
    }

    /**
     * Returns the type parameters of this class or interface type binding.
     * <p>
     * Note that type parameters only occur on the binding of the
     * declaring generic class or interface; e.g., <code>Collection&lt;T&gt;</code>.
     * Type bindings corresponding to a raw or parameterized reference to a generic
     * type do not carry type parameters (they instead have non-empty type arguments
     * and non-trivial erasure).
     * </p>
     *
     * @return the list of binding for the type variables for the type
     * parameters of this type, or otherwise the empty list
     * @see #isTypeVariable()
     * @since 3.1
     */
    @Override
    public ITypeBinding[] getTypeParameters() {
        return new ITypeBinding[0];
    }

    /**
     * Returns the corresponding wildcard binding of this capture binding.
     * Returns <code>null</code> if this type bindings does not represent
     * a capture binding.
     *
     * @return the corresponding wildcard binding for a capture
     * binding, <code>null</code> otherwise
     * @since 3.1
     */
    @Override
    public ITypeBinding getWildcard() {
        return null;
    }

    /**
     * Returns whether this type binding represents an annotation type.
     * <p>
     * Note that an annotation type is always an interface.
     * </p>
     *
     * @return <code>true</code> if this object represents an annotation type,
     * and <code>false</code> otherwise
     * @since 3.1
     */
    @Override
    public boolean isAnnotation() {
        return false;
    }

    /**
     * Returns whether this type binding represents an anonymous class.
     * <p>
     * An anonymous class is a subspecies of local class, and therefore mutually
     * exclusive with member types. Note that anonymous classes have no name
     * (<code>getName</code> returns the empty string).
     * </p>
     *
     * @return <code>true</code> if this type binding is for an anonymous class,
     * and <code>false</code> otherwise
     */
    @Override
    public boolean isAnonymous() {
        return false;
    }

    /**
     * Returns whether this type binding represents an array type.
     *
     * @return <code>true</code> if this type binding is for an array type,
     * and <code>false</code> otherwise
     * @see #getElementType()
     * @see #getDimensions()
     */
    @Override
    public boolean isArray() {
        return false;
    }

    /**
     * Returns whether an expression of this type can be assigned to a variable
     * of the given type, as specified in section 5.2 of <em>The Java Language
     * Specification, Third Edition</em> (JLS3).
     *
     * <p>If the receiver or the argument is a recovered type, the answer is always false,
     * unless the two types are identical or the argument is <code>java.lang.Object</code>.</p>
     *
     * @param variableType the type of a variable to check compatibility against
     * @return <code>true</code> if an expression of this type can be assigned to a
     * variable of the given type, and <code>false</code> otherwise
     * @since 3.1
     */
    @Override
    public boolean isAssignmentCompatible(ITypeBinding variableType) {
        return false;
    }

    /**
     * Returns whether this type binding represents a capture binding.
     * <p>
     * Capture bindings result from capture conversion as specified
     * in section 5.1.10 of <em>The Java Language Specification,
     * Third Edition</em> (JLS3).
     * </p>
     * <p>
     * A capture binding may have upper bounds and a lower bound.
     * Upper bounds may be accessed using {@link #getTypeBounds()},
     * the lower bound must be accessed indirectly through the associated
     * wildcard {@link #getWildcard()} when it is a lower bound wildcard.
     * </p>
     * <p>
     * Note that capture bindings are distinct from type variables
     * (even though they are often depicted as synthetic type
     * variables); as such, {@link #isTypeVariable()} answers
     * <code>false</code> for capture bindings, and
     * {@link #isCapture()} answers <code>false</code> for type variables.
     * </p>
     *
     * @return <code>true</code> if this type binding is a capture,
     * and <code>false</code> otherwise
     * @see #getTypeBounds()
     * @see #getWildcard()
     * @since 3.1
     */
    @Override
    public boolean isCapture() {
        return false;
    }

    /**
     * Returns whether this type is cast compatible with the given type,
     * as specified in section 5.5 of <em>The Java Language
     * Specification, Third Edition</em> (JLS3).
     * <p>
     * NOTE: The cast compatibility check performs backwards.
     * When testing whether type B can be cast to type A, one would use:
     * <code>A.isCastCompatible(B)</code>
     * </p>
     *
     * <p>If the receiver or the argument is a recovered type, the answer is always false,
     * unless the two types are identical or the argument is <code>java.lang.Object</code>.</p>
     *
     * @param type the type to check compatibility against
     * @return <code>true</code> if this type is cast compatible with the
     * given type, and <code>false</code> otherwise
     * @since 3.1
     */
    @Override
    public boolean isCastCompatible(ITypeBinding type) {
        return false;
    }

    /**
     * Returns whether this type binding represents a class type or a recovered binding.
     *
     * @return <code>true</code> if this object represents a class or a recovered binding,
     * and <code>false</code> otherwise
     */
    @Override
    public boolean isClass() {
        return false;
    }

    /**
     * Returns whether this type binding represents an enum type.
     *
     * @return <code>true</code> if this object represents an enum type,
     * and <code>false</code> otherwise
     * @since 3.1
     */
    @Override
    public boolean isEnum() {
        return false;
    }

    /**
     * Returns whether this type binding represents a record type.
     *
     * @return <code>true</code> if this object represents a record type,
     * and <code>false</code> otherwise
     * @noreference
     */
    @Override
    public boolean isRecord() {
        return false;
    }

    /**
     * Returns whether this type binding originated in source code.
     * Returns <code>false</code> for all primitive types, the null type,
     * array types, and for all classes, interfaces, enums, annotation
     * types, type variables, parameterized type references,
     * raw type references, wildcard types, and capture bindings
     * whose information came from a pre-compiled binary class file.
     *
     * @return <code>true</code> if the type is in source code,
     * and <code>false</code> otherwise
     */
    @Override
    public boolean isFromSource() {
        return false;
    }

    /**
     * Returns whether this type binding represents a declaration of
     * a generic class or interface.
     * <p>
     * Note that type parameters only occur on the binding of the
     * declaring generic class or interface; e.g., <code>Collection&lt;T&gt;</code>.
     * Type bindings corresponding to a raw or parameterized reference to a generic
     * type do not carry type parameters (they instead have non-empty type arguments
     * and non-trivial erasure).
     * This method is fully equivalent to <code>getTypeParameters().length &gt; 0)</code>.
     * </p>
     * <p>
     * Note that {@link #isGenericType()},
     * {@link #isParameterizedType()},
     * and {@link #isRawType()} are mutually exclusive.
     * </p>
     *
     * @return <code>true</code> if this type binding represents a
     * declaration of a generic class or interface, and <code>false</code> otherwise
     * @see #getTypeParameters()
     * @since 3.1
     */
    @Override
    public boolean isGenericType() {
        return false;
    }

    /**
     * Returns whether this type binding represents an interface type.
     * <p>
     * Note that an interface can also be an annotation type.
     * </p>
     *
     * @return <code>true</code> if this object represents an interface,
     * and <code>false</code> otherwise
     */
    @Override
    public boolean isInterface() {
        return false;
    }

    /**
     * Returns whether this type binding represents an intersection binding.
     * <p>
     * Intersection types can be derived from type parameter bounds and cast
     * expressions; they also arise in the processes of capture conversion
     * and least upper bound computation as specified in section 4.9 of
     * <em>The Java Language Specification, Java SE 8 Edition</em> (JLS8).
     * </p>
     * <p>
     * All the types in the intersection type can be accessed using
     * {@link #getTypeBounds()}. Wildcard types with more than one
     * bound will also be reported as intersection type. To check whether this
     * is a wildcard type, use {@link #isWildcardType()}.
     * </p>
     *
     * @return <code>true</code> if this type binding is an intersecting type,
     * and <code>false</code> otherwise
     * @see #getTypeBounds()
     * @see ITypeBinding#isWildcardType()
     * @since 3.12
     */
    @Override
    public boolean isIntersectionType() {
        return false;
    }

    /**
     * Returns whether this type binding represents a local class.
     * <p>
     * A local class is any nested class or enum type not declared as a member
     * of another class or interface. A local class is a subspecies of nested
     * type, and mutually exclusive with member types. For anonymous
     * classes, which are considered a subspecies of local classes, this method
     * returns true.
     * </p>
     * <p>
     * Note: This deviates from JLS3 14.3, which states that anonymous types are
     * not local types since they do not have a name. Also note that interfaces
     * and annotation types cannot be local.
     * </p>
     *
     * @return <code>true</code> if this type binding is for a local class or
     * enum type, and <code>false</code> otherwise
     */
    @Override
    public boolean isLocal() {
        return false;
    }

    /**
     * Returns whether this type binding represents a member class or
     * interface.
     * <p>
     * A member type is any type declared as a member of
     * another type. A member type is a subspecies of nested
     * type, and mutually exclusive with local types.
     * </p>
     *
     * @return <code>true</code> if this type binding is for a member class,
     * interface, enum, or annotation type, and <code>false</code> otherwise
     */
    @Override
    public boolean isMember() {
        return false;
    }

    /**
     * Returns whether this type binding represents a nested class, interface,
     * enum, or annotation type.
     * <p>
     * A nested type is any type whose declaration occurs within
     * the body of another. The set of nested types is disjoint from the set of
     * top-level types. Nested types further subdivide into member types, local
     * types, and anonymous types.
     * </p>
     *
     * @return <code>true</code> if this type binding is for a nested class,
     * interface, enum, or annotation type, and <code>false</code> otherwise
     */
    @Override
    public boolean isNested() {
        return false;
    }

    /**
     * Returns whether this type binding represents the null type.
     * <p>
     * The null type is the type of a <code>NullLiteral</code> node.
     * </p>
     *
     * @return <code>true</code> if this type binding is for the null type,
     * and <code>false</code> otherwise
     */
    @Override
    public boolean isNullType() {
        return false;
    }

    /**
     * Returns whether this type binding represents an instance of
     * a generic type corresponding to a parameterized type reference.
     * <p>
     * For example, an AST type like
     * <code>Collection&lt;String&gt;</code> typically resolves to a
     * type binding whose type argument is the type binding for the
     * class <code>java.lang.String</code> and whose erasure is the type
     * binding for the generic type <code>java.util.Collection</code>.
     * </p>
     * <p>
     * Note that {@link #isGenericType()},
     * {@link #isParameterizedType()},
     * and {@link #isRawType()} are mutually exclusive.
     * </p>
     *
     * @return <code>true</code> if this type binding represents a
     * an instance of a generic type corresponding to a parameterized
     * type reference, and <code>false</code> otherwise
     * @see #getTypeArguments()
     * @see #getTypeDeclaration()
     * @since 3.1
     */
    @Override
    public boolean isParameterizedType() {
        return false;
    }

    /**
     * Returns whether this type binding represents a primitive type.
     * <p>
     * There are nine predefined type bindings to represent the eight primitive
     * types and <code>void</code>. These have the same names as the primitive
     * types that they represent, namely boolean, byte, char, short, int,
     * long, float, and double, and void.
     * </p>
     *
     * @return <code>true</code> if this type binding is for a primitive type,
     * and <code>false</code> otherwise
     */
    @Override
    public boolean isPrimitive() {
        return false;
    }

    /**
     * Returns whether this type binding represents an instance of
     * a generic type corresponding to a raw type reference.
     * <p>
     * For example, an AST type like
     * <code>Collection</code> typically resolves to a
     * type binding whose type argument is the type binding for
     * the class <code>java.lang.Object</code> (the
     * default bound for the single type parameter of
     * <code>java.util.Collection</code>) and whose erasure is the
     * type binding for the generic type
     * <code>java.util.Collection</code>.
     * </p>
     * <p>
     * Note that {@link #isGenericType()},
     * {@link #isParameterizedType()},
     * and {@link #isRawType()} are mutually exclusive.
     * </p>
     *
     * @return <code>true</code> if this type binding represents a
     * an instance of a generic type corresponding to a raw
     * type reference, and <code>false</code> otherwise
     * @see #getTypeDeclaration()
     * @see #getTypeArguments()
     * @since 3.1
     */
    @Override
    public boolean isRawType() {
        return false;
    }

    /**
     * Returns whether this type is subtype compatible with the given type,
     * as specified in section 4.10 of <em>The Java Language
     * Specification, Third Edition</em> (JLS3).
     *
     * <p>If the receiver or the argument is a recovered type, the answer is always false,
     * unless the two types are identical or the argument is <code>java.lang.Object</code>.</p>
     *
     * @param type the type to check compatibility against
     * @return <code>true</code> if this type is subtype compatible with the
     * given type, and <code>false</code> otherwise
     * @since 3.1
     */
    @Override
    public boolean isSubTypeCompatible(ITypeBinding type) {
        return false;
    }

    /**
     * Returns whether this type binding represents a top-level class,
     * interface, enum, or annotation type.
     * <p>
     * A top-level type is any type whose declaration does not occur within the
     * body of another type declaration. The set of top level types is disjoint
     * from the set of nested types.
     * </p>
     *
     * @return <code>true</code> if this type binding is for a top-level class,
     * interface, enum, or annotation type, and <code>false</code> otherwise
     */
    @Override
    public boolean isTopLevel() {
        return false;
    }

    /**
     * Returns whether this type binding represents a type variable.
     * Type variables bindings carry the type variable's bounds.
     * <p>
     * Note that type variables are distinct from capture bindings
     * (even though capture bindings are often depicted as synthetic
     * type variables); as such, {@link #isTypeVariable()} answers
     * <code>false</code> for capture bindings, and
     * {@link #isCapture()} answers <code>false</code> for type variables.
     * </p>
     *
     * @return <code>true</code> if this type binding is for a type variable,
     * and <code>false</code> otherwise
     * @see #getName()
     * @see #getTypeBounds()
     * @since 3.1
     */
    @Override
    public boolean isTypeVariable() {
        return false;
    }

    /**
     * Returns whether this wildcard type is an upper bound
     * ("extends") as opposed to a lower bound ("super").
     * Note that this property is only relevant for wildcards
     * that have a bound.
     *
     * @return <code>true</code> if this wildcard type has a bound that is
     * an upper bound, and <code>false</code> in all other cases
     * @see #isWildcardType()
     * @see #getBound()
     * @since 3.1
     */
    @Override
    public boolean isUpperbound() {
        return false;
    }

    /**
     * Returns whether this type binding represents a wildcard type. A wildcard
     * type occurs only as an argument to a parameterized type reference.
     * <p>
     * For example, an AST type like
     * <code>Collection&lt;? extends Number&gt;</code> typically resolves to a
     * parameterized type binding whose type argument is a wildcard type
     * with upper type bound <code>java.lang.Number</code>.
     * </p>
     *
     * @return <code>true</code> if this object represents a wildcard type,
     * and <code>false</code> otherwise
     * @see #getBound()
     * @see #isUpperbound()
     * @since 3.1
     */
    @Override
    public boolean isWildcardType() {
        return false;
    }
}
