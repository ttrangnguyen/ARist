package flute.tokenizing.excode_data;

public abstract class MethodCallRecTest extends RecTest {
    /**
     * For filtering
     */
    private String methodInvocClassQualifiedName;
    private String methodInvocOrigin;

    public String getMethodInvocClassQualifiedName() {
        return methodInvocClassQualifiedName;
    }

    public void setMethodInvocClassQualifiedName(String methodInvocClassQualifiedName) {
        this.methodInvocClassQualifiedName = methodInvocClassQualifiedName;
    }

    public String getMethodInvocOrigin() {
        return methodInvocOrigin;
    }

    public void setMethodInvocOrigin(String methodInvocOrigin) {
        this.methodInvocOrigin = methodInvocOrigin;
    }
}
