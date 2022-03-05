package flute.jdtparser;

public class PublicStaticMember {
    public String key;
    public String excode;
    public String lexical;
    public int modifier;
    public String packageName;
    public String project;
    public String parentTypeKey;

    public PublicStaticMember(String key, String excode, String lexical, int modifier, String packageName, String project, String parentTypeKey) {
        this.key = key;
        this.excode = excode;
        this.lexical = lexical;
        this.modifier = modifier;
        this.packageName = packageName;
        this.project = project;
        this.parentTypeKey = parentTypeKey;
    }
}
