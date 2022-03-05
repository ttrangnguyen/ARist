package flute.utils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ResourceUtils {
    public static String getRelativePath(File target, File base) {
        Path targetPath = Paths.get(target.getAbsolutePath());
        Path basePath = Paths.get(base.getAbsolutePath());
        Path relativePath = basePath.relativize(targetPath);
        return relativePath.toString();
    }

    public static void main(String[] args) {
        System.out.println(ResourceUtils.getRelativePath(new File("storage/repositories/git/four_hundred/jpmml_jpmml-sparkml/src/main/java/org/jpmml/sparkml/ExpressionTranslator.java"),
                new File("storage/repositories/git/four_hundred/jpmml_jpmml-sparkml/")));
    }
}
