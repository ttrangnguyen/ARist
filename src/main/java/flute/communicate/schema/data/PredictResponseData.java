package flute.communicate.schema.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PredictResponseData {
    @SerializedName("rnn")
    @Expose
    public PredictData rnn;
    @SerializedName("ngram")
    @Expose
    public PredictData ngram;
}
