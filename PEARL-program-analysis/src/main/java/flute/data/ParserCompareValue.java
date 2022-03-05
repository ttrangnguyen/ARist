package flute.data;

import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.ArrayList;
import java.util.List;

public class ParserCompareValue {
    List<Integer> values = new ArrayList<>();
    List<ITypeBinding> canBeCastType = new ArrayList<>();

    public boolean contains(int value) {
        return values.contains(value);
    }

    public void addValue(int value){
        values.add(value);
    }

    public List<ITypeBinding> getCanBeCastType() {
        return canBeCastType;
    }

    public void addCastType(ITypeBinding type){
        canBeCastType.add(type);
    }
}
