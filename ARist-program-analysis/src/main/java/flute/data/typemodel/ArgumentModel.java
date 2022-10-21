package flute.data.typemodel;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;

public class ArgumentModel {
    private ITypeBinding resolveType;
    private Expression orgExpr;
    private String lexical;

    public ArgumentModel(Expression orgExpr) {
        this.orgExpr = orgExpr;
        this.resolveType = orgExpr.resolveTypeBinding();
    }

    public ITypeBinding resolveType() {
        return resolveType;
    }

    public void setResolveType(ITypeBinding resolveType) {
        this.resolveType = resolveType;
    }

    public Expression getOrgExpr() {
        return orgExpr;
    }

    public void setOrgExpr(Expression orgExpr) {
        this.orgExpr = orgExpr;
    }

    public String getLexical() {
        return lexical;
    }

    public void setLexical(String lexical) {
        this.lexical = lexical;
    }

    @Override
    public String toString() {
        if (orgExpr != null) return orgExpr.toString();
        else return lexical;
    }
}
