package spring;

import database.models.Link;
import database.models.Project;
import database.service.LinkService;
import database.service.ProjectRequest;
import database.service.ProjectService;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.aspectj.util.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api")
public class WorkflowInitialization {
    @Autowired
    private ProjectService projectService;

    @Autowired
    private LinkService linkService;
    @Transactional
    @PostMapping("/supervisor/initWorkflow")
    public ResponseEntity<String> handleGitRepository(
            @RequestParam("repoUrl") String repoUrl,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        File tempDir = null;
        try {

            tempDir = File.createTempFile("git_repo", "");
            if (!tempDir.delete() || !tempDir.mkdir()) {
                throw new IllegalStateException("Failed to create temporary directory.");
            }
            System.out.println("Start Date: " + startDate);
            System.out.println("End Date: " + endDate);
            //Step 1: get the current state of repository
            cloneGitRepository(repoUrl, tempDir);
            //Step 2: compress state into zip file
            File zipFile = File.createTempFile("zipped_repository", ".zip");
            zipDirectory(tempDir.toPath(), zipFile.toPath());
            byte[] zipFileData = Files.readAllBytes(zipFile.toPath());
            //Step 3: calculate hash value of zip file
            byte[] zipFileHash = generateSHA256Hash(zipFileData);
            System.out.println(hashToHexString(zipFileHash));
            //Step 4: get all contributors in given timeframe
            Set<String> contributors = getGitContributors(tempDir, startDate, endDate);
            String repositoryName = getGitRepositoryName(tempDir);
            //Step 5: set up review process: mail to contributors with hash value, generate unique links
            ProjectRequest payload = new ProjectRequest(repositoryName, hashToHexString(zipFileHash), zipFileData, contributors.stream().toList());
            projectService.createProjectWithUsers(payload);
            //send email

            deleteDirectory(tempDir);
            deleteDirectory(zipFile);

            if (contributors.isEmpty()) {
                return ResponseEntity.ok("No contributors found in" + repositoryName);
            } else {
                return ResponseEntity.ok("Repository: "+ repositoryName +"\nContributors:\n" + String.join("\n", contributors));
            }

        } catch (Exception e) {
            if (tempDir != null) {
                deleteDirectory(tempDir);
            }
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred: " + e.getMessage());
        }
    }

    @GetMapping("/contributor")
    public ResponseEntity<String> validateToken(@RequestParam String token) {
        try{
            Link link = linkService.getLinksByToken(token)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid token"));
            String response = "Token is valid for contributor: " +
                    link.getUser().getEmail() +
                    " with timestamp: " +
                    link.getExpiryTimestamp() +
                    " for project ID: " +
                    link.getProject().getFileName();
            return ResponseEntity.ok(response);
        }
         catch (ResponseStatusException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Invalid token: " + ex.getReason());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred: " + ex.getMessage());
        }
    }

    @GetMapping("/contributor/downloadFile")
    public ResponseEntity<Resource> testFile(@RequestParam String token) throws IOException, NoSuchAlgorithmException {

        Link link = linkService.getLinksByToken(token)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid token"));

        byte[] storedZipData = link.getProject().getFileData();
        String storedHash = link.getProject().getFileHash();
        LocalDateTime timestamp = link.getExpiryTimestamp();
        if(!timestamp.isAfter(LocalDateTime.now())){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Token no longer valid");
        }

        if (storedZipData == null || storedZipData.length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No file data available for this token");
        }
        byte[] recalculatedHash = generateSHA256Hash(storedZipData);
        String recalculatedHashString = hashToHexString(recalculatedHash);

        if (!storedHash.equals(recalculatedHashString)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File integrity validation failed");
        }

        File reconstructedZipFile = File.createTempFile("reconstructed_repository", ".zip");
        Files.write(reconstructedZipFile.toPath(), storedZipData);

        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(reconstructedZipFile.toPath()));

        // Build and return the response
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"repository_to_audit_" + link.getProject().getFileName() + ".zip\"")
                .contentLength(storedZipData.length)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }


        private void cloneGitRepository(String repoUrl, File cloneDir) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder("git", "clone", repoUrl, cloneDir.getAbsolutePath());
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Failed to clone the repository. Git exit code: " + exitCode);
        }
    }
    private Set<String> getGitContributors(File repoDir, String startDate, String endDate) throws Exception {
        Set<String> emails = new HashSet<>();

        ProcessBuilder processBuilder = new ProcessBuilder("git", "log", "--format=%ae", "--since=" + startDate, "--until=" + endDate);
        processBuilder.directory(repoDir);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if(!line.contains("noreply")){
                    emails.add(line);
                }

            }
        }

        process.waitFor();
        return emails;
    }
    private String getGitRepositoryName(File repoDir) throws Exception {
        String repositoryName = "";
        ProcessBuilder processBuilder = new ProcessBuilder("git", "remote", "-v");
        processBuilder.directory(repoDir);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            if ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");
                if (parts.length > 1) {
                    String remoteUrl = parts[1];
                    if (remoteUrl.endsWith(".git")) {
                        remoteUrl = remoteUrl.substring(0, remoteUrl.length() - 4);
                    }
                    repositoryName = remoteUrl.substring(remoteUrl.lastIndexOf("/") + 1);
                }
            }
        }
        process.waitFor();
        return repositoryName;
    }
    private void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            for (File file : Objects.requireNonNull(directory.listFiles())) {
                deleteDirectory(file);
            }
        }
        directory.delete();
    }

    private static void zipDirectory(Path sourceDirPath, Path zipFilePath) throws IOException {
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
    private static String hashToHexString(byte[] hash){
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    public byte[] generateSHA256Hash(byte[] data) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Failed to calculate hash", e);
        }
    }


}
