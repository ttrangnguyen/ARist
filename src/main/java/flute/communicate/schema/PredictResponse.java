package flute.communicate.schema;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PredictResponse extends Response {
    @SerializedName("data")
    @Expose
    private List<String> data = null;

    public List<String> getData() {
        return data;
    }
}
