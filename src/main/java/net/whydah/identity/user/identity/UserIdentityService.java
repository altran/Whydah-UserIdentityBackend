package net.whydah.identity.user.identity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.ConflictException;
import net.whydah.identity.audit.ActionPerformed;
import net.whydah.identity.audit.AuditLogRepository;
import net.whydah.identity.user.ChangePasswordToken;
import net.whydah.identity.user.email.PasswordSender;
import net.whydah.identity.user.search.LuceneIndexer;
import net.whydah.identity.user.search.LuceneSearch;
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
    private final LdapAuthenticator primaryLdapAuthenticator;
    private final LdapUserIdentityDao ldapUserIdentityDao;
    private final AuditLogRepository auditLogRepository;

    private final PasswordGenerator passwordGenerator;
    private final PasswordSender passwordSender;

    private final LuceneIndexer luceneIndexer;
    private final LuceneSearch searcher;


    @Inject
    public UserIdentityService(@Named("primaryLdap") LdapAuthenticator primaryLdapAuthenticator,
                               LdapUserIdentityDao ldapUserIdentityDao, AuditLogRepository auditLogRepository, PasswordGenerator passwordGenerator,
                               PasswordSender passwordSender, LuceneIndexer luceneIndexer, LuceneSearch searcher) {
        this.primaryLdapAuthenticator = primaryLdapAuthenticator;
        this.ldapUserIdentityDao = ldapUserIdentityDao;
        this.auditLogRepository = auditLogRepository;
        this.passwordGenerator = passwordGenerator;
        this.passwordSender = passwordSender;
        this.luceneIndexer = luceneIndexer;
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
        ldapUserIdentityDao.setTempPassword(username, newPassword, salt);
        audit(uid,ActionPerformed.MODIFIED, "resetpassword", uid);

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
        String salt = ldapUserIdentityDao.getSalt(username);

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
        ldapUserIdentityDao.changePassword(username, newPassword);
        audit(userUid,ActionPerformed.MODIFIED, "password", userUid);
    }

    public UserIdentity addUserIdentityWithGeneratedPassword(UserIdentityRepresentation dto) {
        String username = dto.getUsername();
        if (username == null){
            throw new ConflictException("Can not create a user without username!");
        }
        try {
            if (ldapUserIdentityDao.usernameExist(username)) {
                //return Response.status(Response.Status.NOT_ACCEPTABLE).build();
                String msg = "User already exists in LDAP, could not create user with username=" + dto.getUsername();
                log.info(msg);
                throw new ConflictException(msg);
            }
        } catch (NamingException e) {
            throw new RuntimeException("usernameExist failed for username=" + dto.getUsername(), e);
        }

        String email;
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
                String msg = "E-mail " + email + " is already in use (in lucene index), could not create user with username=" + username;
                log.info(msg);
                throw new ConflictException(msg);
            }
        }

        String uid = UUID.randomUUID().toString();
        UserIdentity userIdentity = new UserIdentity(uid, dto.getUsername(), dto.getFirstName(), dto.getLastName(),
                dto.getPersonRef(), email, dto.getCellPhone(), passwordGenerator.generate());
        try {
            ldapUserIdentityDao.addUserIdentity(userIdentity);
            luceneIndexer.addToIndex(userIdentity);
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


    public UserIdentity getUserIdentityForUid(String uid) throws NamingException {
        if (ldapUserIdentityDao.getUserIndentityForUid(uid) == null) {
            log.warn("Trying to access non-existing UID, removing form index: " + uid);
            luceneIndexer.removeFromIndex(uid);
        }
        return ldapUserIdentityDao.getUserIndentityForUid(uid);
    }

    public void updateUserIdentityForUid(String uid, UserIdentity newUserIdentity) {
        ldapUserIdentityDao.updateUserIdentityForUid(uid, newUserIdentity);
        luceneIndexer.update(newUserIdentity);
        audit(uid,ActionPerformed.MODIFIED, "user", newUserIdentity.toString());
    }


    public UserIdentity getUserIdentity(String username) throws NamingException {
        return ldapUserIdentityDao.getUserIndentity(username);
    }
    public void updateUserIdentity(String username, UserIdentity newuser) {
        ldapUserIdentityDao.updateUserIdentityForUsername(username, newuser);
        luceneIndexer.update(newuser);
    }

    public void deleteUserIdentity(String username) throws NamingException {
        luceneIndexer.removeFromIndex(getUserIdentity(username).getUid());
        ldapUserIdentityDao.deleteUserIdentity(username);
    }

    private void audit(String uid,String action, String what, String value) {
        String now = sdf.format(new Date());
        ActionPerformed actionPerformed = new ActionPerformed(uid, now, action, what, value);
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
