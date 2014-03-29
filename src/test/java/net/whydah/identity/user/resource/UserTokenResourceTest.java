package net.whydah.identity.user.resource;

import com.sun.jersey.api.view.Viewable;
import net.whydah.identity.application.role.ApplicationRepository;
import net.whydah.identity.audit.AuditLogRepository;
import net.whydah.identity.config.AppConfig;
import net.whydah.identity.dataimport.DatabaseHelper;
import net.whydah.identity.user.WhydahUser;
import net.whydah.identity.user.identity.*;
import net.whydah.identity.user.role.UserPropertyAndRole;
import net.whydah.identity.user.role.UserPropertyAndRoleRepository;
import net.whydah.identity.user.search.Indexer;
import net.whydah.identity.usertoken.UserTokenResource;
import net.whydah.identity.util.FileUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.naming.NamingException;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author <a href="mailto:erik.drolshammer@altran.com">Erik Drolshammer</a>
 * @since 10/18/12
 */
public class UserTokenResourceTest {
    private final static String basepath = "target/UserTokenResourceTest/";
    private final static String ldappath = basepath + "hsqldb/ldap/";
    private final static String dbpath = basepath + "hsqldb/roles";
//    private final static int LDAP_PORT = 10937;
    private static String LDAP_URL; // = "ldap://localhost:" + LDAP_PORT + "/dc=external,dc=WHYDAH,dc=no";

    private static EmbeddedADS ads;
    private static UserPropertyAndRoleRepository roleRepository;
    private static UserAdminHelper userAdminHelper;
    private static QueryRunner queryRunner;

    private static UserAuthenticationService userAuthenticationService;

    @BeforeClass
    public static void setUp() throws Exception {
    	System.setProperty(AppConfig.IAM_MODE_KEY, AppConfig.IAM_MODE_DEV);
    	
        int LDAP_PORT = new Integer(AppConfig.appConfig.getProperty("ldap.embedded.port"));
        LDAP_URL = "ldap://localhost:" + LDAP_PORT + "/dc=external,dc=WHYDAH,dc=no";
        
    	FileUtils.deleteDirectory(new File(basepath));

        File ldapdir = new File(ldappath);
        ldapdir.mkdirs();
        ads = new EmbeddedADS(ldappath);
        try {
            ads.startServer(LDAP_PORT);
        } catch (Exception e){

        }
        LDAPHelper ldapHelper = new LDAPHelper(LDAP_URL, "uid=admin,ou=system", "secret", "initials");
        LdapAuthenticatorImpl ldapAuthenticator = new LdapAuthenticatorImpl(LDAP_URL, "uid=admin,ou=system", "secret", "initials");
        userAuthenticationService = new UserAuthenticationService(ldapAuthenticator, ldapHelper);


        roleRepository = new UserPropertyAndRoleRepository();
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setUrl("jdbc:hsqldb:file:" + dbpath);
        queryRunner = new QueryRunner(dataSource);

        DatabaseHelper databaseHelper = new DatabaseHelper(queryRunner);
        databaseHelper.initDB();

        roleRepository.setQueryRunner(queryRunner);
        ApplicationRepository configDataRepository = new ApplicationRepository();
        configDataRepository.setQueryRunner(queryRunner);
        roleRepository.setApplicationRepository(configDataRepository);
        AuditLogRepository auditLogRepository = new AuditLogRepository(queryRunner);
        Directory index = new NIOFSDirectory(new File(basepath + "lucene"));
        userAdminHelper = new UserAdminHelper(ldapHelper, new Indexer(index), auditLogRepository, roleRepository);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (ads != null) {
            ads.stopServer();
        }
    }


    @Test
    public void testAuthenticateUsingFacebookCredentials() throws NamingException {
        WhydahUserIdentity newIdentity = new WhydahUserIdentity();
        String username = "facebookUsername";
        newIdentity.setUsername(username);
        String facebookId = "1234";
        newIdentity.setPassword(facebookId + facebookId);
        newIdentity.setFirstName("firstName");
        newIdentity.setLastName("lastName");
        String email = "e@mail.com";
        newIdentity.setEmail(email);

        UserTokenResource resource = new UserTokenResource(roleRepository, userAdminHelper, userAuthenticationService);


        String roleValue = "roleValue";
        Response response = resource.createAndAuthenticateUser(newIdentity, roleValue,false);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Viewable entity = (Viewable) response.getEntity();
        WhydahUser model = (WhydahUser) entity.getModel();

        WhydahUserIdentity identity = model.getIdentity();
        assertEquals(username, identity.getUsername());
        assertNull(identity.getPersonRef());
        assertEquals(email, identity.getEmail());
        assertNotNull(identity.getUid());


        List<UserPropertyAndRole> propsAndRoles = model.getPropsAndRoles();
        
        String applicationId = AppConfig.appConfig.getProperty("adduser.defaultapplication.id");
        String applicationName = AppConfig.appConfig.getProperty("adduser.defaultapplication.name");
        String organizationId = AppConfig.appConfig.getProperty("adduser.defaultorganization.id");
        String organizationName = AppConfig.appConfig.getProperty("adduser.defaultorganization.name");
        String roleName = AppConfig.appConfig.getProperty("adduser.defaultrole.name");
        
        String facebookRoleName = AppConfig.appConfig.getProperty("adduser.defaultrole.facebook.name");
        
        for (UserPropertyAndRole role : propsAndRoles) {
            assertEquals(applicationId, role.getAppId());
//            assertEquals(applicationName, role.getApplicationName());
//            assertEquals(organizationId, role.getOrgId());
//            assertEquals(organizationName, role.getOrganizationName()); //TODO figure out why orgName is not set.
        }

        assertEquals(2, propsAndRoles.size());

        UserPropertyAndRole role1 = propsAndRoles.get(0);
        assertEquals(roleName, role1.getRoleName());

        UserPropertyAndRole role2 = propsAndRoles.get(1);
        assertEquals(facebookRoleName, role2.getRoleName());
    }


    @Test
    public void testGetFacebookDataAsString() {
        StringBuilder strb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n ");
        strb.append("<user>");
        strb.append("<params>");
        strb.append("<userId>").append("745925666").append("</userId>");
        strb.append("<firstName>").append("Erik").append("</firstName>");
        strb.append("<lastName>").append("Drolshammer").append("</lastName>");
        strb.append("<username>").append("erik.drolshammer").append("</username>");
        strb.append("<email>").append("erik.drolshammer@someprovider.com").append("</email>");
        strb.append("<birthday>").append("08/05/1982").append("</birthday>");
        strb.append("<hometown>").append("Moss, Norway").append("</hometown>");
        strb.append("<location>").append("Oslo, Norway").append("</location>");
        strb.append("</params>");
        strb.append("</user>");

        InputStream input = new ByteArrayInputStream(strb.toString().getBytes());
        String facebookDataAsString = UserTokenResource.getFacebookDataAsString(input);
        assertNotNull(facebookDataAsString);
    }

    @Test
    public void testGetFacebookDataAsStringFromDomDocument() throws Exception {
        StringBuilder strb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n ");
        strb.append("<user>");
        strb.append("<params>");
        String expectedFbUserId = "745925666";
        strb.append("<userId>").append(expectedFbUserId).append("</userId>");
        strb.append("<firstName>").append("Erik").append("</firstName>");
        strb.append("<lastName>").append("Drolshammer").append("</lastName>");
        strb.append("<username>").append("erik.drolshammer").append("</username>");
        strb.append("<email>").append("erik.drolshammer@someprovider.com").append("</email>");
        strb.append("<birthday>").append("08/05/1982").append("</birthday>");
        String expectedHomeTown = "Moss, Norway";
        strb.append("<hometown>").append(expectedHomeTown).append("</hometown>");
        strb.append("<location>").append("Oslo, Norway").append("</location>");
        strb.append("</params>");
        strb.append("</user>");

        InputStream input = new ByteArrayInputStream(strb.toString().getBytes());
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        Document fbUserDoc = builder.parse(input);

        String fbDataValueWithCdata = UserTokenResource.getFacebookDataAsXmlString(fbUserDoc);
        assertNotNull(fbDataValueWithCdata);

        //Strip cdata wrapper
        String fbDataValue = fbDataValueWithCdata.replace("<![CDATA[", "").replace("]]>", "");

        InputStream fbDataInput = new ByteArrayInputStream(fbDataValue.getBytes());
        Document fbDataDoc = builder.parse(fbDataInput);

        XPath xPath = XPathFactory.newInstance().newXPath();
        String fbUserId = (String) xPath.evaluate("//userId[1]", fbDataDoc, XPathConstants.STRING);
        assertEquals(expectedFbUserId, fbUserId);
        String hometown = (String) xPath.evaluate("//hometown[1]", fbDataDoc, XPathConstants.STRING);
        assertEquals(expectedHomeTown, hometown);
    }
}
