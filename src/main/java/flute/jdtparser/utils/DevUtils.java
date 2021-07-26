package flute.jdtparser.utils;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.internal.compiler.impl.ReferenceContext;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;

import java.lang.reflect.Field;

public class DevUtils {
    public static ReferenceContext getResolver(IMethodBinding methodBinding) throws Exception {
        Class myClass = methodBinding.getClass();
        Field resolver = myClass.getDeclaredField("resolver");
        resolver.setAccessible(true);

        Field scope = resolver.get(methodBinding).getClass().getDeclaredField("scope");
        scope.setAccessible(true);
        return ((CompilationUnitScope) scope.get(resolver.get(methodBinding))).referenceContext().getCompilationUnitDeclaration();
    }
}
