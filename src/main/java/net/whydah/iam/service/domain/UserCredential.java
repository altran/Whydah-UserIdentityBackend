package net.whydah.iam.service.domain;

public class UserCredential {

    private int credentialType;

    // Type 0
    private String password;

    public UserCredential(int credentialType, String password) {
        this.credentialType = credentialType;
        this.password = password;
    }

    public UserCredential() {
    }

    public int getCredentialType() {
        return credentialType;
    }

    public String getPassword() {
        return password;
    }

    public void setCredentialType(int credentialType) {
        this.credentialType = credentialType;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
