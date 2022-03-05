package flute.communicate.schema;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TokenizeResponse extends Response {
    @SerializedName("data")
    @Expose
    private List<String> data;

    public List<String> getData() {
        return data;
    }
}
