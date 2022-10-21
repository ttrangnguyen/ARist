package flute.feature.ps;

import java.util.Objects;

public class PsClass {
    public String className;
    public String packagename;

    public PsClass(String className, String packagename) {
        this.className = className;
        this.packagename = packagename;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PsClass psClass = (PsClass) o;
        return className.equals(psClass.className) && Objects.equals(packagename, psClass.packagename);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, packagename);
    }
}
