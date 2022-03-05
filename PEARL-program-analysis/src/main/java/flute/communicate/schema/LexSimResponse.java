package flute.communicate.schema;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class LexSimResponse extends Response {
    @SerializedName("data")
    @Expose
    private float data;

    public float getData() {
        return data;
    }
}
