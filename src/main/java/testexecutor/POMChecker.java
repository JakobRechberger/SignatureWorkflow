package testexecutor;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class POMChecker {
    public static boolean checkDependenciesOfPom(String path) {
        try {
            File inputFile = new File(path);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            return checkDependencies(doc);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    private static boolean checkDependencies(Document doc) {
        String[][] dependencies = {
                {"commons-codec", "commons-codec"},
                {"org.junit.jupiter", "junit-jupiter"},
                {"org.apache.maven.shared", "maven-invoker"},
                {"org.apache.xmlgraphics", "fop"},
                {"com.openhtmltopdf", "openhtmltopdf-pdfbox"},
                {"org.benf", "cfr"}
        };

        Map<String, String> dependenciesMap = new HashMap<>();
        NodeList nodeList = doc.getElementsByTagName("dependency");

        for (int i = 0; i < nodeList.getLength(); i++) {
            String groupId = nodeList.item(i).getChildNodes().item(1).getTextContent();
            String artifactId = nodeList.item(i).getChildNodes().item(3).getTextContent();
            dependenciesMap.put(groupId, artifactId);
        }

        for (String[] dependency : dependencies) {
            if (!dependenciesMap.containsKey(dependency[0]) || !dependenciesMap.get(dependency[0]).equals(dependency[1])) {
                return false;
            }
        }

        return true;
    }
}
