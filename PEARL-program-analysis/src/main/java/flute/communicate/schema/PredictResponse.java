package flute.communicate.schema;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import flute.communicate.schema.data.PredictResponseData;

public class PredictResponse extends Response {
    @SerializedName("data")
    @Expose
    private PredictResponseData data;

    public PredictResponseData getData() {
        return data;
    }
}
