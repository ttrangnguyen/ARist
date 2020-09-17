package flute.data.type;

public class IntPrimitiveType extends NumPrimitiveType {
    protected static final String KEY = "I";

    @Override
    protected String getInnerKey() {
        return KEY;
    }
}
