package net.whydah.identity.user.identity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.ConflictException;
import net.whydah.identity.audit.ActionPerformed;
import net.whydah.identity.audit.AuditLogRepository;
import net.whydah.identity.user.ChangePasswordToken;
import net.whydah.identity.user.email.PasswordSender;
import net.whydah.identity.user.search.Indexer;
import net.whydah.identity.user.search.Search;
import net.whydah.identity.util.PasswordGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.naming.NamingException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 29.03.14
 */
@Singleton
public class UserIdentityService {
    private static final Logger log = LoggerFactory.getLogger(UserIdentityService.class);

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");
    private static final String SALT_ENCODING = "UTF-8";

    //@Inject @Named("internal") private LdapAuthenticatorImpl internalLdapAuthenticator;
    private final LdapAuthenticatorImpl primaryLdapAuthenticator;
    private final LDAPHelper ldapHelper;
    private final AuditLogRepository auditLogRepository;

    private final PasswordGenerator passwordGenerator;
    private final PasswordSender passwordSender;

    private final Indexer indexer;
    private final Search searcher;


    @Inject
    public UserIdentityService(@Named("primaryLdap") LdapAuthenticatorImpl primaryLdapAuthenticator,
                               LDAPHelper ldapHelper, AuditLogRepository auditLogRepository, PasswordGenerator passwordGenerator,
                               PasswordSender passwordSender, Indexer indexer, Search searcher) {
        this.primaryLdapAuthenticator = primaryLdapAuthenticator;
        this.ldapHelper = ldapHelper;
        this.auditLogRepository = auditLogRepository;
        this.passwordGenerator = passwordGenerator;
        this.passwordSender = passwordSender;
        this.indexer = indexer;
        this.searcher = searcher;
    }

    public UserIdentity authenticate(final String username, final String password) {
        return primaryLdapAuthenticator.authenticate(username, password);
    }


    public void resetPassword(String username, String uid, String userEmail) {
        String token = setTempPassword(username, uid);
        passwordSender.sendResetPasswordEmail(username, token, userEmail);
    }
    private String setTempPassword(String username, String uid) {
        String newPassword = passwordGenerator.generate();
        String salt = passwordGenerator.generate();
        ldapHelper.setTempPassword(username, newPassword, salt);
        audit(ActionPerformed.MODIFIED, "resetpassword", uid);

        byte[] saltAsBytes;
        try {
            saltAsBytes = salt.getBytes(SALT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        ChangePasswordToken changePasswordToken = new ChangePasswordToken(username, newPassword);
        return changePasswordToken.generateTokenString(saltAsBytes);
    }

    /**
     * Authenticate using token generated when resetting the password
     * @param username  username to authenticate
     * @param token with temporary access
     * @return  true if authentication OK
     */
    public boolean authenticateWithChangePasswordToken(String username, String token) {
        String salt = ldapHelper.getSalt(username);

        byte[] saltAsBytes;
        try {
            saltAsBytes = salt.getBytes(SALT_ENCODING);
        } catch (UnsupportedEncodingException e1) {
            throw new RuntimeException("Error with salt for username=" + username, e1);
        }
        ChangePasswordToken changePasswordToken = ChangePasswordToken.fromTokenString(token, saltAsBytes);
        boolean ok = primaryLdapAuthenticator.authenticateWithTemporaryPassword(username, changePasswordToken.getPassword());
        log.info("authenticateWithChangePasswordToken was ok={} for username={}", username, ok);
        return ok;
    }


    public void changePassword(String username, String userUid, String newPassword) {
        ldapHelper.changePassword(username, newPassword);
        audit(ActionPerformed.MODIFIED, "password", userUid);
    }

    public UserIdentity addUserIdentityWithGeneratedPassword(UserIdentityRepresentation dto) {
        String username = dto.getUsername();
        try {
            if (ldapHelper.usernameExist(username)) {
                //return Response.status(Response.Status.NOT_ACCEPTABLE).build();
                throw new ConflictException("User already exists, could not create user " + username);
            }
        } catch (NamingException e) {
            throw new RuntimeException("usernameExist failed for username=" + username, e);
        }

        String email = null;

            if (dto.getEmail() != null && dto.getEmail().contains("+")) {
                email = replacePlusWithEmpty(dto.getEmail());
            } else {
                email = dto.getEmail();
            }
        if (email != null) {
            InternetAddress internetAddress = new InternetAddress();
            internetAddress.setAddress(email);
            try {
                internetAddress.validate();
            } catch (AddressException e) {
                //log.error(String.format("E-mail: %s is of wrong format.", email));
                //return Response.status(Response.Status.BAD_REQUEST).build();
                //TODO use BadRequestException from jersey 2.7
                throw new IllegalArgumentException(String.format("E-mail: %s is of wrong format.", email));
            }

            List<UserIdentityRepresentation> usersWithSameEmail = searcher.search(email);
            if (!usersWithSameEmail.isEmpty()) {
                throw new ConflictException("E-mail " + email + " is already in use, could not create user " + username);
            }
        }

        String uid = UUID.randomUUID().toString();
        UserIdentity userIdentity = new UserIdentity(uid, dto.getUsername(), dto.getFirstName(), dto.getLastName(),
                dto.getPersonRef(), email, dto.getCellPhone(), passwordGenerator.generate());
        try {
            ldapHelper.addUserIdentity(userIdentity);
            indexer.addToIndex(userIdentity);
        } catch (NamingException e) {
            throw new RuntimeException("addUserIdentity failed for " + userIdentity.toString(), e);
        }
        log.info("Added new user to LDAP: username={}, uid={}", username, uid);
        return userIdentity;
    }

    private static String replacePlusWithEmpty(String email){
        String[] words = email.split("[+]");
        if (words.length == 1) {
            return email;
        }
        email  = "";
        for (String word : words) {
            email += word;
        }
        return email;
    }


    @Deprecated
    public void addUserIdentity(UserIdentity userIdentity) {
        String username = userIdentity.getUsername();
        try {
            if (ldapHelper.usernameExist(username)) {
                throw new ConflictException("User already exists, could not create user " + username);
            }
        } catch (NamingException e) {
            throw new RuntimeException("usernameExist failed for username=" + username, e);
        }

        if (userIdentity.getEmail() != null) {
            String email = userIdentity.getEmail();
            List<UserIdentityRepresentation> usersWithSameEmail = searcher.search(email);
            if (!usersWithSameEmail.isEmpty()) {
                throw new ConflictException("E-mail " + email + " is already in use, could not create user " + username);
            }
        }

        userIdentity.setPassword(passwordGenerator.generate());
        userIdentity.setUid(UUID.randomUUID().toString());

        try {
            ldapHelper.addUserIdentity(userIdentity);
        } catch (NamingException e) {
            throw new RuntimeException("addUserIdentity failed for " + userIdentity.toString(), e);
        }
        log.info("Added new user to LDAP: {}", username);
    }

    public UserIdentity getUserIndentityForUid(String uid) throws NamingException {
        return ldapHelper.getUserIndentityForUid(uid);
    }

    public void updateUserIdentityForUid(String uid, UserIdentity newuser) {
        ldapHelper.updateUserIdentityForUid(uid, newuser);
    }


    public UserIdentity getUserIndentity(String username) throws NamingException {
        return ldapHelper.getUserIndentity(username);
    }
    public void updateUserIdentity(String username, UserIdentity newuser) {
        ldapHelper.updateUserIdentityForUsername(username, newuser);
    }

    public void deleteUserIdentity(String username) {
        ldapHelper.deleteUserIdentity(username);
    }

    private void audit(String action, String what, String value) {
        String now = sdf.format(new Date());
        ActionPerformed actionPerformed = new ActionPerformed(value, now, action, what, value);
        auditLogRepository.store(actionPerformed);
    }

    //FIXME baardl: implement verification that admin is allowed to update this password.
    //Find the admin user token, based on tokenid
    public boolean allowedToUpdate(String applicationtokenid, String adminUserTokenId) {

        return true;
    }

    //FIXME baardl: implement verification that admin is allowed to update this password.
    //Find the admin user token, based on tokenid
    public String findUserByTokenId(String adminUserTokenId) {
        return "not-found-not-implemented";
    }
}
