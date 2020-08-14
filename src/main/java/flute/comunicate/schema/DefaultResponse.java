package flute.comunicate.schema;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class DefaultResponse {

    @SerializedName("type")
    @Expose
    private String type;

    @SerializedName("data")
    @Expose
    private Object data;

    public String getType() {
        return type;
    }

    public Object getData() {
        return data;
    }

}
