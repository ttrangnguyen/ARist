package flute.communicate.schema.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PredictData {
    @SerializedName("result")
    @Expose
    private List<String> result = null;
    @SerializedName("runtime")
    @Expose
    private Double runtime;

    public List<String> getResult() {
        return result;
    }

    public void setResult(List<String> result) {
        this.result = result;
    }

    public Double getRuntime() {
        return runtime;
    }

    public void setRuntime(Double runtime) {
        this.runtime = runtime;
    }
}
