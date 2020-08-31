package flute.communicate.schema;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Response {
    @SerializedName("type")
    @Expose
    private String type;

    public String getType() {
        return type;
    }
}
