package flute.data;

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;

public class MethodMember extends Member {
    private IMethodBinding member;

    public MethodMember(IMethodBinding member) {
        this.member = member;;
    }

    public IMethodBinding getMember() {
        return member;
    }

    @Override
    public String toString() {
        return member.toString();
    }
}
