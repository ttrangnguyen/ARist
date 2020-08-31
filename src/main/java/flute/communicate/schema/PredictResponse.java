package flute.communicate.schema;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PredictResponse extends Response {
    @SerializedName("data")
    @Expose
    private List<String> data = null;

    @SerializedName("runtime")
    @Expose
    private float runtime = -1;

    public List<String> getData() {
        return data;
    }

    public float getRuntime() {
        return runtime;
    }
}
