package net.whydah.identity.user.identity;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.whydah.identity.user.ChangePasswordToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.io.UnsupportedEncodingException;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 29.03.14
 */
public class UserAuthenticationService {
    private static final Logger log = LoggerFactory.getLogger(UserAuthenticationService.class);

    //@Inject @Named("internal") private LdapAuthenticatorImpl internalLdapAuthenticator;
    private LdapAuthenticatorImpl externalLdapAuthenticator;
    private LDAPHelper ldapHelper;


    @Inject
    public UserAuthenticationService(@Named("external") LdapAuthenticatorImpl externalLdapAuthenticator,
                                     LDAPHelper ldapHelper) {
        this.externalLdapAuthenticator = externalLdapAuthenticator;
        this.ldapHelper = ldapHelper;
    }

    public WhydahUserIdentity getUserinfo(String username) throws NamingException {
        return ldapHelper.getUserinfo(username);
    }


    public WhydahUserIdentity auth(final String username, final String password) {
        return externalLdapAuthenticator.auth(username, password);
    }

    public boolean authenticateWithTemporaryPassword(String username, String token) {
        byte[] saltAsBytes = null;
        try {
            String salt = ldapHelper.getSalt(username);
            saltAsBytes = salt.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e1) {
            log.error("Could not generate salt with username={}", username, e1);
        }

        log.debug("salt=" + new String(saltAsBytes));
        ChangePasswordToken changePasswordToken = ChangePasswordToken.fromTokenString(token, saltAsBytes);
        log.info("Passwordtoken for {} ok.", username);
        boolean ok = externalLdapAuthenticator.authenticateWithTemporaryPassword(username, changePasswordToken.getPassword());
        return ok;
    }


    public void changePassword(String username, String newpassword) {
        ldapHelper.changePassword(username, newpassword);
    }

    public void deleteUser(String username) {
        ldapHelper.deleteUser(username);
    }

    public void updateUser(String username, WhydahUserIdentity newuser) {
        ldapHelper.updateUser(username, newuser);
    }



}
