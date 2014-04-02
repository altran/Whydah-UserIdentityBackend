package net.whydah.identity.user.identity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import net.whydah.identity.audit.ActionPerformed;
import net.whydah.identity.audit.AuditLogRepository;
import net.whydah.identity.user.ChangePasswordToken;
import net.whydah.identity.user.email.PasswordSender;
import net.whydah.identity.util.PasswordGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 29.03.14
 */
@Singleton
public class UserAuthenticationService {
    private static final Logger log = LoggerFactory.getLogger(UserAuthenticationService.class);

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");

    //@Inject @Named("internal") private LdapAuthenticatorImpl internalLdapAuthenticator;
    private final LdapAuthenticatorImpl externalLdapAuthenticator;
    private final LDAPHelper ldapHelper;
    private final AuditLogRepository auditLogRepository;

    private final PasswordGenerator passwordGenerator;
    private final PasswordSender passwordSender;


    @Inject
    public UserAuthenticationService(@Named("external") LdapAuthenticatorImpl externalLdapAuthenticator,
                                     LDAPHelper ldapHelper, AuditLogRepository auditLogRepository, PasswordGenerator passwordGenerator,
                                     PasswordSender passwordSender) {
        this.externalLdapAuthenticator = externalLdapAuthenticator;
        this.ldapHelper = ldapHelper;
        this.auditLogRepository = auditLogRepository;
        this.passwordGenerator = passwordGenerator;
        this.passwordSender = passwordSender;
    }

    public WhydahUserIdentity getUserinfo(String username) throws NamingException {
        return ldapHelper.getUserinfo(username);
    }


    public WhydahUserIdentity authenticate(final String username, final String password) {
        return externalLdapAuthenticator.authenticate(username, password);
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


    public void changePassword(String username, String userUid, String newPassword) {
        ldapHelper.changePassword(username, newPassword);
        audit(ActionPerformed.MODIFIED, "password", userUid);
    }


    public void addUserToLdap(WhydahUserIdentity userIdentity) {
        userIdentity.setPassword(passwordGenerator.generate());
        userIdentity.setUid(UUID.randomUUID().toString());

        String username = userIdentity.getUsername();
        try {
            if (ldapHelper.usernameExist(username)) {
                //return Response.status(Response.Status.NOT_ACCEPTABLE).build();
                throw new IllegalStateException("User already exists, could not create user " + username);
            }
        } catch (NamingException e) {
            throw new RuntimeException("usernameExist failed for username=" + username, e);
        }

        try {
            ldapHelper.addWhydahUserIdentity(userIdentity);
        } catch (NamingException e) {
            throw new RuntimeException("addWhydahUserIdentity failed for " + userIdentity.toString(), e);
        }
        log.info("Added new user to LDAP: {}", username);
    }

    public void updateUser(String username, WhydahUserIdentity newuser) {
        ldapHelper.updateUser(username, newuser);
    }


    public void deleteUser(String username) {
        ldapHelper.deleteUser(username);
    }

    public void resetPassword(String username, String uid, String userEmail) {
        String newPassword = passwordGenerator.generate();
        String salt = passwordGenerator.generate();
        ldapHelper.setTempPassword(username, newPassword, salt);

        ChangePasswordToken changePasswordToken = new ChangePasswordToken(username, newPassword);
        String token;
        try {
            token = changePasswordToken.generateTokenString(salt.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        passwordSender.sendResetPasswordEmail(username, token, userEmail);
        audit(ActionPerformed.MODIFIED, "resetpassword", uid);
    }

    private void audit(String action, String what, String value) {
        String now = sdf.format(new Date());
        ActionPerformed actionPerformed = new ActionPerformed(value, now, action, what, value);
        auditLogRepository.store(actionPerformed);
    }
}
