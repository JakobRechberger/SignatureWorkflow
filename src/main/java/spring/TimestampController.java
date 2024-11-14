package spring;

import certificate.FileHashGenerator;
import certificate.TimestampRequestGenerator;
import certificate.TimestampResponseSaver;
import certificate.TimestampServiceClient;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static certificate.FileHashGenerator.generateSHA256Hash;
import static certificate.TimestampFile.timestampFile;
import static certificate.TimestampValidator.verifyTimestamp;

public class TimestampController {
    @RestController
    @RequestMapping("/api")
    public static class FileUploadController {

        @PostMapping("/timestamp")
        public ResponseEntity<Resource> handleFileUpload(@RequestParam("file") MultipartFile file) {
            try {
                String fileName = file.getOriginalFilename();
                System.out.println("Received file: " + fileName);
                byte[] fileBytes = file.getBytes();
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                TimeStampRequest tsRequest = TimestampRequestGenerator.createTimestampRequest(digest.digest(fileBytes));
                TimeStampResponse tsResponse = TimestampServiceClient.sendTimestampRequest(tsRequest);
                File timestampFile = TimestampResponseSaver.saveTimestampResponse(tsResponse);
                System.out.println(timestampFile.getPath());

                Resource resource = new FileSystemResource(timestampFile);
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + timestampFile.getName());

                return ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(timestampFile.length())
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        }
        @PostMapping("/verify")
        public ResponseEntity<String> verifyTimestampFromFile(
                @RequestParam("file1") MultipartFile file1,
                @RequestParam("file2") MultipartFile file2) throws IOException {

            String fileName1 = file1.getOriginalFilename();
            System.out.println("Received file1: " + fileName1);

            String fileName2 = file2.getOriginalFilename();
            assert fileName2 != null;
            if (!fileName2.endsWith(".tsr")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Second file must have a .tsr extension.");
            }
            System.out.println("Received file2: " + fileName2);
            File convertedFile1 = convertMultipartFileToFile(file1);
            File convertedFile2 = convertMultipartFileToFile(file2);
            String response = verifyTimestamp( convertedFile1,  convertedFile2);

            return ResponseEntity.ok(response);

        }
    }
    private static File convertMultipartFileToFile(MultipartFile multipartFile) throws IOException {
        File convFile = File.createTempFile("upload_", "_" + multipartFile.getOriginalFilename());
        multipartFile.transferTo(convFile);
        return convFile;
    }
}
