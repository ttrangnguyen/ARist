package flute.jdtparser;

public class ObjectMappingMember {
    private String methodKey;
    private int param;
    private int mappingId;

    public ObjectMappingMember(String methodKey, int param, int mappingId) {
        this.methodKey = methodKey;
        this.param = param;
        this.mappingId = mappingId;
    }

    public String getMethodKey() {
        return methodKey;
    }

    public void setMethodKey(String methodKey) {
        this.methodKey = methodKey;
    }

    public int getParam() {
        return param;
    }

    public void setParam(int param) {
        this.param = param;
    }

    public int getMappingId() {
        return mappingId;
    }

    public void setMappingId(int mappingId) {
        this.mappingId = mappingId;
    }
}
