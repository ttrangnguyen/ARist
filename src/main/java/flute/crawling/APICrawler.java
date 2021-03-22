package flute.crawling;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class APICrawler {
    public static List<String> swtLink(String classString, String methodString) throws Exception {
        String url = "https://help.eclipse.org/2021-03/nftopic/org.eclipse.platform.doc.isv/reference/api/"
                + classString.replace(".", "/") + ".html";
        Document document = Jsoup.connect(url).get();
        Element methodTable = document.select("a[id=\"method.summary\"]").first()
                .parent().select(".memberSummary").first();

        Elements rows = methodTable.select("tr[id]");

        for (Element row : rows) {
            //remove zwsp
            String method = row.select(".colSecond").first().text().replaceAll("\u200B", "");
            String paramString = method.substring(method.indexOf("(") + 1, method.length() - 1);
            String methodNameString = method.substring(0, method.indexOf("("));
            List<String> paramTypes = new ArrayList<>();
            List<String> paramNames = new ArrayList<>();
            Arrays.stream(paramString.split(", ")).forEach(param -> {
                if (param.length() > 0) {
                    paramTypes.add(param.split(" ")[0]);
                    paramNames.add(param.split(" ")[1]);
                }
            });
            String methodStringGen = methodNameString + "(" + String.join(",", paramTypes) + ")";
            if (methodString.equals(methodStringGen)) {
                return paramNames;
            }
        }
        throw new Exception("Can not found");
    }

    public static List<String> awtswingLink(String classString, String methodString) throws Exception {
        String url = "https://docs.oracle.com/javase/8/docs/api/"
                + classString.replace(".", "/") + ".html";
        Document document = Jsoup.connect(url).get();
        Element methodTable = document.select("a[name=\"method.summary\"]").first()
                .parent().select(".memberSummary").first();

        Elements rows = methodTable.select("tr[id]");

        for (Element row : rows) {
            String method = row.select(".colLast").first().select("code").first().text();
            String paramString = method.substring(method.indexOf("(") + 1, method.length() - 1);
            String methodNameString = method.substring(0, method.indexOf("("));
            List<String> paramTypes = new ArrayList<>();
            List<String> paramNames = new ArrayList<>();
            Arrays.stream(paramString.split(", ")).forEach(param -> {
                if (param.length() > 0) {
                    paramTypes.add(param.split(" ")[0]);
                    paramNames.add(param.split(" ")[1]);
                }
            });
            String methodStringGen = methodNameString + "(" + String.join(",", paramTypes) + ")";

            if (methodString.equals(methodStringGen)) {
                return paramNames;
            }
        }
        throw new Exception("Can not found");
    }

    public static List<String> paramNames(String classString, String methodString) throws Exception {
        if (classString.startsWith("org.eclipse.swt.")) {
            return swtLink(classString, methodString);
        } else if (classString.startsWith("java.awt.") || classString.startsWith("javax.swing.")) {
            return awtswingLink(classString, methodString);
        }
        throw new Exception("Can not found");
    }

    public static void main(String[] args) throws Exception {
        System.out.println(swtLink("org.eclipse.swt.widgets.Display", "asyncExec(Runnable)"));
        System.out.println(awtswingLink("java.awt.AWTEventMulticaster", "add(ActionListener,ActionListener)"));
    }

}