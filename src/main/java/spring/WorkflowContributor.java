package spring;

import certificate.TimestampRequestGenerator;
import certificate.TimestampResponseSaver;
import certificate.TimestampServiceClient;
import com.sun.mail.iap.Response;
import database.models.Link;
import database.models.User;
import database.service.LinkService;
import database.service.ProjectService;
import database.service.UserService;
import database.service.UserSignatureRequest;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import signandVerify.SignatureTemp;
import signandVerify.VerificationTemp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

import static certificate.TimestampValidator.verifyTimestamp;
import static sha256.SHA256Methods.hashToHexString;
import static sha256.SHA256Methods.generateSHA256Hash;

@RestController
@RequestMapping("/api")
public class WorkflowContributor {

    @Autowired
    private LinkService linkService;
    @Autowired
    private UserService userService;
    @GetMapping("/contributor")
    public ResponseEntity<String> validateToken(@RequestParam String token) {
        try{
            Link link = linkService.getLinksByToken(token)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid token"));
            LocalDateTime timestamp = link.getExpiryTimestamp();
            if(!timestamp.isAfter(LocalDateTime.now())){
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Token no longer valid");
            }
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
    public ResponseEntity<Resource> testFile(@RequestParam String token) throws IOException {

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
    @GetMapping("/contributor/verifyFileHash")
    public ResponseEntity<String> verifyFileHash(@RequestParam("token") String token, @RequestParam("hash") String hash) {
        Link link = linkService.getLinksByToken(token)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid token"));
        String storedHash = link.getProject().getFileHash();
        if (!storedHash.equals(hash)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File integrity validation failed");
        }
    return ResponseEntity.ok("Hash validated. This project hasn't been manipulated");
    }

    @PostMapping ("/contributor/signProject")
    public ResponseEntity<String> getSignature(@RequestParam("token") String token) throws Exception {
        Link link = linkService.getLinksByToken(token)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid token"));
        byte[] project = link.getProject().getFileData();
        File tempFile = File.createTempFile("upload_", "_" + link.getProject().getFileName());
        Files.write(tempFile.toPath(), project);
        List<File> signatureFiles = SignatureTemp.signFile(tempFile);
        byte[] timestampFile;

        UserSignatureRequest request = new UserSignatureRequest();
        if(signatureFiles != null && VerificationTemp.verifySignature(Files.readAllBytes(signatureFiles.get(0).toPath()), Files.readAllBytes(signatureFiles.get(1).toPath()), project)){
            try{
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                TimeStampRequest tsRequest = TimestampRequestGenerator.createTimestampRequest(digest.digest(Files.readAllBytes(signatureFiles.get(0).toPath())));
                TimeStampResponse tsResponse = TimestampServiceClient.sendTimestampRequest(tsRequest);
                timestampFile = Files.readAllBytes(TimestampResponseSaver.saveTimestampResponse(tsResponse).toPath());
                request.setSignature(Files.readAllBytes(signatureFiles.get(0).toPath()));
                request.setPublicKey(Files.readAllBytes(signatureFiles.get(1).toPath()));
                request.setTimestamp(timestampFile);
                request.setLink(link);
                userService.setUserSignature(request);
                System.out.println("User successfully updated");
                return ResponseEntity.ok("User updated successfully for token: " + token);
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        return ResponseEntity.ok("Failed to upload signature");

    }
    @PostMapping("/contributor/addSignatureToDatabase")
    public ResponseEntity<String> updateUserByToken(@RequestParam String token, @RequestBody UserSignatureRequest userSignatureRequest) {
        Link link = linkService.getLinksByToken(token)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid token"));
        userSignatureRequest.setLink(link);
        userService.setUserSignature(userSignatureRequest);
        return ResponseEntity.ok("User updated successfully for token: " + token);
    }
    @PostMapping("/contributor/verifySignature")
    public ResponseEntity<String> verifySignature(@RequestParam("token") String token) throws IOException {
        Link link = linkService.getLinksByToken(token)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid token"));
        byte[] signature = link.getUser().getSignature();
        byte[] publickey = link.getUser().getPublicKey();
        byte[] timestamp = link.getUser().getTimestamp();
        byte[] project = link.getProject().getFileData();
        File tempSignatureFile = File.createTempFile("signature", "");
        File tempTimestampFile = File.createTempFile("timestamp", ".tsr");
        Files.write(tempSignatureFile.toPath(), signature );
        Files.write(tempTimestampFile.toPath(), timestamp );
        String response = verifyTimestamp( tempSignatureFile,  tempTimestampFile);
        if(VerificationTemp.verifySignature(project, signature, publickey)){
            return ResponseEntity.ok("Signature valid for user: " + link.getUser().getEmail() + " for project:" + link.getProject().getFileName() +
                    "\n Timestamp validated with: " + response);
        }
        else{
            return ResponseEntity.ok("Signature verification failed for user: " + link.getUser().getEmail() + " for project:" + link.getProject().getFileName());
        }
    }

}
