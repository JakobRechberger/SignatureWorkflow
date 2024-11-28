package database.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class ProjectRequest {
    private String fileName;
    private String fileHash;
    private byte[] file;
    private List<String> userEmails;
    public ProjectRequest(String fileName, String fileHash, byte[] file, List<String> userEmails) throws IOException {
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.file = file;
        this.userEmails = userEmails;
    }
    public String getFileName(){return fileName;}
    public void setFileName(String fileName){this.fileName = fileName;}
    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public byte[] getFile() {
        return file;
    }

    public void setFile(byte[] file) {
        this.file = file;
    }

    public List<String> getUserEmails() {
        return userEmails;
    }

    public void setUserEmails(List<String> userEmails) {
        this.userEmails = userEmails;
    }


}
