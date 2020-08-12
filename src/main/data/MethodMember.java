package data;

import org.eclipse.jdt.core.dom.IMethodBinding;

public class MethodMember extends Member {

    public MethodMember(IMethodBinding member) {
        super.setMember(member);
    }
}
