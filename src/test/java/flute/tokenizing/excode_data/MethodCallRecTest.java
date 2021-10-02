package flute.tokenizing.excode_data;

public abstract class MethodCallRecTest extends RecTest {
    /**
     * For filtering
     */
    private String methodInvoc;
    private String methodInvocCaller;
    private String methodInvocClassQualifiedName;
    private String methodInvocOrigin;

    public String getMethodInvoc() {
        return methodInvoc;
    }

    public void setMethodInvoc(String methodInvoc) {
        this.methodInvoc = methodInvoc;
    }

    public String getMethodInvocCaller() {
        return methodInvocCaller;
    }

    public void setMethodInvocCaller(String methodInvocCaller) {
        this.methodInvocCaller = methodInvocCaller;
    }

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
