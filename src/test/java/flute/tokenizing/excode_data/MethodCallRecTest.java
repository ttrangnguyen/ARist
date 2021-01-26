package flute.tokenizing.excode_data;

public abstract class MethodCallRecTest extends RecTest {
    /**
     * For filtering
     */
    private String methodInvocClassQualifiedName;

    public String getMethodInvocClassQualifiedName() {
        return methodInvocClassQualifiedName;
    }

    public void setMethodInvocClassQualifiedName(String methodInvocClassQualifiedName) {
        this.methodInvocClassQualifiedName = methodInvocClassQualifiedName;
    }
}
