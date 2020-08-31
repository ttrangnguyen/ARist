package flute.communicate.schema;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class DefaultResponse extends Response {

    @SerializedName("data")
    @Expose
    private Object data;

    public Object getData() {
        return data;
    }

}
