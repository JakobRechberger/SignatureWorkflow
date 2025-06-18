package spring;


import database.models.Link;
import database.models.Project;
import database.models.User;
import database.models.UserDTO;
import database.service.*;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static sha256.SHA256Methods.generateSHA256Hash;
import static sha256.SHA256Methods.hashToHexString;
import static testexecutor.ExecuteTest.executeTestsAndProcessReport;

@RestController
@RequestMapping("/api")
public class WorkflowInitialization {
    @Autowired
    private ProjectService projectService;

    @Autowired
    private LinkService linkService;
    @Autowired
    private UserService userService;
    @Transactional
    @PostMapping("/supervisor/initWorkflow")
    public ResponseEntity<String> handleGitRepository(
            @RequestParam("repoUrl") String repoUrl,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("executeTests") boolean executeTests ){
        File tempDir = null;
        try {

            tempDir = File.createTempFile("git_repo", "");
            if (!tempDir.delete() || !tempDir.mkdir()) {
                throw new IllegalStateException("Failed to create temporary directory.");
            }
            System.out.println("Start Date: " + startDate);
            System.out.println("End Date: " + endDate);
            cloneGitRepository(repoUrl, tempDir);
            if(executeTests){
                executeTestsAndProcessReport(tempDir);
            }
            File zipFile = File.createTempFile("zipped_repository", ".zip");
            zipDirectory(tempDir.toPath(), zipFile.toPath());
            byte[] zipFileData = Files.readAllBytes(zipFile.toPath());
            byte[] zipFileHash = generateSHA256Hash(zipFileData);
            System.out.println(hashToHexString(zipFileHash));
            Set<String> contributors = getGitContributors(tempDir, startDate, endDate);
            String repositoryName = getGitRepositoryName(tempDir);
            ProjectRequest payload = new ProjectRequest(repositoryName, hashToHexString(zipFileHash), zipFileData, contributors.stream().toList());
            //projectService.createProjectWithUsers(payload);



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
    @GetMapping("/supervisor/getProjectStatus")
    public ResponseEntity<List<UserDTO>> getProjectStatus(@RequestParam("repoName") String repoName){
        Project project = projectService.getProjectByName(repoName)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid Repo Name"));
        List<Link> links = linkService.getLinksByProjectID(project.getId());
        List<User> allUsersOfProject = links.stream()
                .map(Link::getUser)
                .toList();
        List<UserDTO> userDTOs = allUsersOfProject.stream()
                .map(user -> new UserDTO(
                        user.getEmail(),
                        user.getSignature() != null
                )).toList();
        if (allUsersOfProject.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(userDTOs);
    }

    @GetMapping("/supervisor/resendEmail")
    public ResponseEntity<String> resendEmailToContributor(@RequestParam("email") String email, @RequestParam("projectName") String projectName){
        List<User> users = userService.getUserByEmail(email);
        List<Link> links = linkService.getLinksByUserID(users.get(0).getId());
        Project project = projectService.getProjectByName(projectName).get(0);
        Optional<Link> link1 = links.stream()
                .filter(link -> link.getProject().getId().equals(project.getId()))
                .findFirst();
        Link link = link1.orElseThrow(()-> new RuntimeException("Link not found"));
        User user = link.getUser();
        System.out.println(user.getEmail() + link.getToken() + project.getFileName());
        //projectService.sendEmail(user, link, project);
        return ResponseEntity.ok("ok");
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


}

