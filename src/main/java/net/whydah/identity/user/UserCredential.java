package net.whydah.identity.user;

//Not in use. Totto was sceptical to delete it, so maybe it will be used in the future.
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
