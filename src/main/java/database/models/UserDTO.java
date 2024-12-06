package database.models;

public class UserDTO {
    private String email;
    private String signatureStatus;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSignatureStatus() {
        return signatureStatus;
    }

    public void setSignatureStatus(String signatureStatus) {
        this.signatureStatus = signatureStatus;
    }

    public UserDTO(String email, boolean isSigned) {
        this.email = email;
        this.signatureStatus = isSigned ? "User signed" : "User not signed";
    }
}
