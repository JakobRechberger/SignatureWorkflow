package database.service;

import database.models.Link;

public class UserSignatureRequest {

    private Link link;
    private byte[] signature;
    private byte[] publicKey;
    private byte[] timestamp;
    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }
    public Link getLink() {
        return link;
    }

    public void setLink(Link link) {
        this.link = link;
    }
    public byte[] getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(byte[] timestamp) {
        this.timestamp = timestamp;
    }

}
