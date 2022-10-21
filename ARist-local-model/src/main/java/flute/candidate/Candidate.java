package flute.candidate;

import flute.config.ModelConfig;
import flute.feature.ps.PsClass;
import slp.core.lexing.code.JavaLexer;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Candidate {
    public String key;
    public String excode;
    public String lexical;
    public String packageName;
    public Integer modifier;
    public PsClass psClass;
    public Set<String> lexicalTokens;
    public Integer defRecentness;
    public Integer useRecentness;

    public Candidate(String lexical) {
        this.lexical = lexical;
        this.defRecentness = -1;
        this.useRecentness = -1;
    }

    public Candidate(String excode, String lexical) {
        this.excode = excode;
        this.lexical = lexical;
        this.defRecentness = -1;
        this.useRecentness = -1;
    }

    public void tokenizeSelf() {
        ModelConfig.TokenizedType tmp = ModelConfig.tokenizedType;
        ModelConfig.tokenizedType = ModelConfig.TokenizedType.SUB_TOKEN;
        JavaLexer lexer = new JavaLexer();
        this.lexicalTokens = lexer.lexLine(lexical).collect(Collectors.toSet());
        ModelConfig.tokenizedType = tmp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Candidate candidate = (Candidate) o;
        return  Objects.equals(key, candidate.key) &&
                Objects.equals(excode, candidate.excode) &&
                lexical.equals(candidate.lexical) &&
                Objects.equals(packageName, candidate.packageName) &&
                Objects.equals(psClass, candidate.psClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, excode, lexical, packageName, psClass);
    }
}
