package net.whydah.iam.service.resource;

import com.google.inject.Inject;

import net.whydah.iam.service.audit.ActionPerformed;
import net.whydah.iam.service.config.AppConfig;
import net.whydah.iam.service.domain.UserPropertyAndRole;
import net.whydah.iam.service.domain.WhydahUserIdentity;
import net.whydah.iam.service.ldap.LDAPHelper;
import net.whydah.iam.service.repository.AuditLogRepository;
import net.whydah.iam.service.repository.UserPropertyAndRoleRepository;
import net.whydah.iam.service.search.Indexer;
import net.whydah.iam.service.security.Authentication;
import net.whydah.iam.service.security.UserToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.ws.rs.core.Response;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Helper class to avoid code duplication between UserAdminResource and WhydahUserResource.
 *
 * @author <a href="mailto:erik.drolshammer@altran.com">Erik Drolshammer</a>
 * @since 10/4/12
 */
public class UserAdminHelper {

    private static final Logger logger = LoggerFactory.getLogger(UserAdminHelper.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");

    private final LDAPHelper ldapHelper;
    private final Indexer indexer;
    private final AuditLogRepository auditLogRepository;
    private final UserPropertyAndRoleRepository roleRepository;

    @Inject
    public UserAdminHelper(LDAPHelper ldapHelper, Indexer indexer, AuditLogRepository auditLogRepository, UserPropertyAndRoleRepository roleRepository) {
        this.ldapHelper = ldapHelper;
        this.indexer = indexer;
        this.auditLogRepository = auditLogRepository;
        this.roleRepository = roleRepository;
    }

    public Response addUser(WhydahUserIdentity newIdentity) {
        String username = newIdentity.getUsername();
        logger.trace("Adding new user: {}", username);

        try {
            if (ldapHelper.usernameExist(username)) {
                logger.info("User already exists, could not create user " + username);
                return Response.status(Response.Status.NOT_ACCEPTABLE).build();
            }

            newIdentity.setUid(UUID.randomUUID().toString());
            ldapHelper.addWhydahUserIdentity(newIdentity);
            logger.info("Added new user: {}", username);
        } catch (Exception e) {
            logger.error("Could not create user " + username, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        addDefaultWhydahUserRole(newIdentity);

        try {
            indexer.addToIndex(newIdentity);
            audit(ActionPerformed.ADDED, "user", newIdentity.toString());
        } catch (Exception e) {
            logger.error("Error with lucene indexing or audit loggin for " + username, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok().build();
    }

    public static WhydahUserIdentity createWhydahUserIdentity(Document fbUserDoc) {
        XPath xPath = XPathFactory.newInstance().newXPath();

        try {
            String fbUserId = (String) xPath.evaluate("//userId", fbUserDoc, XPathConstants.STRING);
            String firstName = (String) xPath.evaluate("//firstName", fbUserDoc, XPathConstants.STRING);
            String lastName = (String) xPath.evaluate("//lastName", fbUserDoc, XPathConstants.STRING);
            String username = (String) xPath.evaluate("//username", fbUserDoc, XPathConstants.STRING);
            String email = (String) xPath.evaluate("//email", fbUserDoc, XPathConstants.STRING);
            logger.debug("From fbuserXml, fbUserId=" + fbUserId + ", firstName=" + firstName + ", lastName=" + lastName);

            WhydahUserIdentity whydahUserIdentity = new WhydahUserIdentity();
            whydahUserIdentity.setUsername(username);
            whydahUserIdentity.setFirstName(firstName);
            whydahUserIdentity.setLastName(lastName);
            whydahUserIdentity.setEmail(email);

            String password = calculateFacebookPassword(fbUserId);
            whydahUserIdentity.setPassword(password);
            return whydahUserIdentity;
        } catch (XPathExpressionException e) {
            logger.error("", e);
            return null;
        }
    }

    static String calculateFacebookPassword(String fbId) {
        return fbId + fbId;
    }

    public void addDefaultWhydahUserRole(WhydahUserIdentity userIdentity) {
        UserPropertyAndRole role = new UserPropertyAndRole();
        
        String applicationId = AppConfig.appConfig.getProperty("adduser.defaultapplication.id");
        String applicationName = AppConfig.appConfig.getProperty("adduser.defaultapplication.name");
        String organizationId = AppConfig.appConfig.getProperty("adduser.defaultorganization.id");
        String organizationName = AppConfig.appConfig.getProperty("adduser.defaultorganization.name");
        String roleName = AppConfig.appConfig.getProperty("adduser.defaultrole.name");
        String roleValue = AppConfig.appConfig.getProperty("adduser.defaultrole.value");
        
        role.setUid(userIdentity.getUid());
		role.setAppId(applicationId);
		role.setApplicationName(applicationName);
		role.setOrgId(organizationId);
		role.setOrganizationName(organizationName);
		role.setRoleName(roleName);
		role.setRoleValue(roleValue);
        logger.debug("Adding Role: {}", role);

        if (roleRepository.hasRole(userIdentity.getUid(), role)) {
            logger.warn("Role already exist. " + role.toString());
            return;
        }

        roleRepository.addUserPropertyAndRole(role);
        String value = "uid=" + userIdentity + ", username=" + userIdentity.getUsername() + ", appid=" + role.getAppId() + ", role=" + role.getRoleName();
        audit(ActionPerformed.ADDED, "role", value);
    }

    public void addFacebookDataRole(WhydahUserIdentity userIdentity, String roleValue){
        boolean facebook=true;
        boolean netiq=false;
        if (roleValue.indexOf("netIQAccessToken")>0){
            facebook=false;
            netiq=true;
        }
        UserPropertyAndRole role = new UserPropertyAndRole();
        
        String applicationId = AppConfig.appConfig.getProperty("adduser.defaultapplication.id");
        String applicationName = AppConfig.appConfig.getProperty("adduser.defaultapplication.name");
        String organizationId = AppConfig.appConfig.getProperty("adduser.defaultorganization.id");
        String organizationName = AppConfig.appConfig.getProperty("adduser.defaultorganization.name");
        String facebookRoleName = AppConfig.appConfig.getProperty("adduser.defaultrole.facebook.name");
        String netIQRoleName = AppConfig.appConfig.getProperty("adduser.defaultrole.netiq.name");

        role.setUid(userIdentity.getUid());
        role.setAppId(applicationId);
        role.setApplicationName(applicationName);
        role.setOrgId(organizationId);
        role.setOrganizationName(organizationName);
        if (facebook){
            role.setRoleName(facebookRoleName);
        role.setRoleValue(roleValue);
        } else if (netiq){
            role.setRoleName(netIQRoleName);
            role.setRoleValue(userIdentity.getEmail());  // Provide NetIQ identity as rolevalue
        }
        logger.debug("Adding Role: {}", role);

        if (roleRepository.hasRole(userIdentity.getUid(), role)) {
            logger.warn("Role already exist. " + role.toString());
            return;
        }

        roleRepository.addUserPropertyAndRole(role);
        String value = "uid=" + userIdentity + ", username=" + userIdentity.getUsername() + ", appid=" + role.getAppId() + ", role=" + role.getRoleName();
        audit(ActionPerformed.ADDED, "role", value);
    }


    private void audit(String action, String what, String value) {
        UserToken authenticatedUser = Authentication.getAuthenticatedUser();
        if (authenticatedUser == null) {
            logger.error("authenticatedUser is not set. Auditing failed for action=" + action + ", what=" + what + ", value=" + value);
            return;
        }
        String userId = authenticatedUser.getName();
        String now = sdf.format(new Date());
        ActionPerformed actionPerformed = new ActionPerformed(userId, now, action, what, value);
        auditLogRepository.store(actionPerformed);
    }


}
