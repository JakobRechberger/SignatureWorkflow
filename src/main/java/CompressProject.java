import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CompressProject {
    public static void main(String[] args) {
        Set<String> emails = getContributorsFromProjectWithinTimeFrame("", "2022-07-23", "2024-09-10");
        emails.stream().sorted().forEach(System.out::println);
    }
    public static File initZipProject(String projectPath){
        String[] parts = projectPath.split("[\\\\/]");
        StringBuilder zipFilePath = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++){
            zipFilePath.append(parts[i]).append("/");
        }
        zipFilePath.append(parts[parts.length - 1]).append(".zip");
        File zipFile = new File(zipFilePath.toString());
        try{
            zipDirectory(Paths.get(projectPath), Paths.get(zipFile.getAbsolutePath()));
            System.out.println("Directory successfully zipped.");
        } catch(IOException e){
            e.printStackTrace();
        }
        return zipFile;
    }
    public static void zipDirectory(Path sourceDirPath, Path zipFilePath) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipFilePath));
             Stream<Path> paths = Files.walk(sourceDirPath)) {
            paths
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(sourceDirPath.relativize(path).toString());
                        try {
                            zipOutputStream.putNextEntry(zipEntry);
                            Files.copy(path, zipOutputStream);
                            zipOutputStream.closeEntry();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    private static void writeTimeStampToFile(long time){
        try {
            FileWriter fw
                    = new FileWriter("C:\\Users\\jakob\\IdeaProjects\\timestamp.txt");
            fw.write(Long.toString(time));
            fw.close();
        }
        catch (Exception e) {
            e.getStackTrace();
        }
    }
    //--since="2022-01-01" --until="2022-12-31"
    //YYYY-MM-DD
    public static Set<String> getContributorsFromProjectWithinTimeFrame(String targetPath, String startDate, String endDate) {
        Set<String> emails = new HashSet<>();
        try {
            File gitDir = new File(targetPath);
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "git", "log", "--format=%ae",
                    "--since=" + startDate, "--until=" + endDate
            );
            processBuilder.directory(gitDir);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                emails.add(line);
            }
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return emails;
    }
}
