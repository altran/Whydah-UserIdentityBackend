package net.whydah.identity.user.identity;

import net.whydah.sso.user.types.UserIdentity;

@Deprecated  // Use UIBUserIdentity and UserIdentityMapper in TypeLib
public class UIBUserIdentityRepresentation extends UserIdentity {
    protected transient String password;

    public String getPassword() {
        return password;
    }


    public void setPassword(String password) {
        this.password = password;
    }


    public String getCellPhone() {
        return this.cellPhone;
    }
}
