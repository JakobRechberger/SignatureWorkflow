package testexecutor;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class XmlToPdf {
    public static void convertXmlToPdf(File filepath, String contentRoot, Map<String, String> testCaseMap) {
        String htmlContent = htmlStringBuilder(filepath, testCaseMap);
        File testDirectory = new File(contentRoot, "test-reports");

        if (!testDirectory.exists()) {
            testDirectory.mkdir();
        }
        try{
            FileUtils.copyFile(filepath, new File(testDirectory+ File.separator+ changeFileSuffix(filepath.getName(), "xml")));
        }
        catch(IOException e){
            e.printStackTrace();
        }

        String htmlFilePath = testDirectory+ File.separator+ changeFileSuffix(filepath.getName(), "html");
        String pdfFilePath = testDirectory+ File.separator+ changeFileSuffix(filepath.getName(), "pdf");
        try{
            FileWriter writer = new FileWriter(htmlFilePath);
            writer.write(htmlContent);
            writer.close();
            System.out.println("HTML report with code snippets generated successfully!");
        }
        catch (Exception e){
            e.printStackTrace();
        }
        try (OutputStream os = new FileOutputStream(pdfFilePath)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContent, new File(htmlFilePath).getParent());
            builder.toStream(os);
            builder.run();
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }
    public static String htmlStringBuilder(File filepath, Map<String, String> testCaseMap) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(filepath);
            doc.getDocumentElement().normalize();

            StringBuilder htmlContent = new StringBuilder();
            htmlContent.append("<html><head><title>Test Report</title>");
            htmlContent.append("<style>");
            htmlContent.append("table { width: 100%; border-collapse: collapse; font-size: 10px;}");
            htmlContent.append("th, td { border: 1px solid black; padding: 8px; text-align: left; font-size: 10px; }");
            htmlContent.append("th { background-color: #f2f2f2; font-size: 10px;}");
            htmlContent.append("tr:nth-child(even) { background-color: #f9f9f9; }");
            htmlContent.append("pre { white-space: pre-wrap; word-wrap: break-word; }");
            htmlContent.append("</style></head><body>");
            htmlContent.append("<h1>Test Report</h1>");
            htmlContent.append("<h2>Test Suite: ").append(doc.getDocumentElement().getAttribute("name")).append("</h2>");
            htmlContent.append("<p>Report created: ").append(formatDate()).append("</p>");
            htmlContent.append("<table><tr><th>Test Case</th><th>Class</th><th>Time</th><th>Result</th><th>Message</th><th>Code</th></tr>");

            NodeList testCaseNodes = doc.getElementsByTagName("testcase");
            for (int i = 0; i < testCaseNodes.getLength(); i++) {
                Node testCaseNode = testCaseNodes.item(i);
                if (testCaseNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element testCaseElement = (Element) testCaseNode;

                    String testName = testCaseElement.getAttribute("name");
                    String testClass = testCaseElement.getAttribute("classname");
                    String testTime = testCaseElement.getAttribute("time");
                    boolean isFailed = testCaseElement.getElementsByTagName("failure").getLength() > 0;
                    String testResult = isFailed ? "Failed" : "Passed";
                    String resultColor = isFailed ? "red" : "green";
                    String failureMessage = isFailed ? escapeHtml(testCaseElement.getElementsByTagName("failure").item(0).getTextContent()) : "";
                    String codeSnippet = escapeHtml(testCaseMap.getOrDefault(testName, ""));

                    htmlContent.append("<tr>")
                            .append("<td>").append(testName).append("</td>")
                            .append("<td>").append(testClass).append("</td>")
                            .append("<td>").append(testTime).append("s</td>")
                            .append("<td style='background-color:").append(resultColor).append(";'>").append(testResult).append("</td>")
                            .append("<td>").append(failureMessage).append("</td>")
                            .append("<td><pre>").append(codeSnippet).append("</pre></td>")
                            .append("</tr>");
                }
            }
            htmlContent.append("</table></body></html>");
            return htmlContent.toString();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }
    public static String changeFileSuffix(String filepath, String suffix){
        return filepath.substring(0,filepath.length() - 3) + suffix;
    }
    public static String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
    public static String formatDate(){
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        return myDateObj.format(myFormatObj);
    }
}
