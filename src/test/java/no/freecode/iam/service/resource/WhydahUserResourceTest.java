package no.freecode.iam.service.resource;

import com.sun.jersey.api.view.Viewable;
import no.freecode.iam.service.dataimport.DatabaseHelper;
import no.freecode.iam.service.domain.UserPropertyAndRole;
import no.freecode.iam.service.domain.WhydahUser;
import no.freecode.iam.service.domain.WhydahUserIdentity;
import no.freecode.iam.service.ldap.EmbeddedADS;
import no.freecode.iam.service.ldap.LDAPHelper;
import no.freecode.iam.service.ldap.LdapAuthenticatorImpl;
import no.freecode.iam.service.prestyr.PstyrImporterTest;
import no.freecode.iam.service.repository.AuditLogRepository;
import no.freecode.iam.service.repository.BackendConfigDataRepository;
import no.freecode.iam.service.repository.UserPropertyAndRoleRepository;
import no.freecode.iam.service.search.Indexer;
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
 * @author <a href="mailto:erik@freecode.no">Erik Drolshammer</a>
 * @since 10/18/12
 */
public class WhydahUserResourceTest {
    private final static String basepath = "target/WhydahUserResourceTest/";
    private final static String ldappath = basepath + "hsqldb/ldap/";
    private final static String dbpath = basepath + "hsqldb/roles";
    private final static int LDAP_PORT = 10937;
    private final static String LDAP_URL = "ldap://localhost:" + LDAP_PORT + "/dc=external,dc=OBOS,dc=no";

    private static EmbeddedADS ads;
    private static LDAPHelper ldapHelper;
    private static LdapAuthenticatorImpl ldapAuthenticator;
    private static UserPropertyAndRoleRepository roleRepository;
    private static UserAdminHelper userAdminHelper;
    private static QueryRunner queryRunner;

    @BeforeClass
    public static void setUp() throws Exception {
        PstyrImporterTest.deleteDirectory(new File(basepath));

        File ldapdir = new File(ldappath);
        ldapdir.mkdirs();
        EmbeddedADS ads = new EmbeddedADS(ldappath);
        ads.startServer(LDAP_PORT);
        ldapHelper = new LDAPHelper(LDAP_URL, "uid=admin,ou=system", "secret", "initials");
        ldapAuthenticator = new LdapAuthenticatorImpl(LDAP_URL, "uid=admin,ou=system", "secret", "initials");


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
        BackendConfigDataRepository configDataRepository = new BackendConfigDataRepository();
        configDataRepository.setQueryRunner(queryRunner);
        roleRepository.setBackendConfigDataRepository(configDataRepository);
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

        WhydahUserResource resource = new WhydahUserResource(ldapAuthenticator, roleRepository, userAdminHelper);


        String roleValue = "roleValue";
        Response response = resource.createAndAuthenticateUser(newIdentity, roleValue);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Viewable entity = (Viewable) response.getEntity();
        WhydahUser model = (WhydahUser) entity.getModel();

        WhydahUserIdentity identity = model.getIdentity();
        assertEquals(username, identity.getUsername());
        assertNull(identity.getPersonRef());
        assertEquals(email, identity.getEmail());
        assertNotNull(identity.getUid());


        List<UserPropertyAndRole> propsAndRoles = model.getPropsAndRoles();

        for (UserPropertyAndRole role : propsAndRoles) {
            assertEquals(UserAdminHelper.APP_ID_GIFTIT, role.getAppId());
            assertEquals(UserAdminHelper.APP_NAME_GIFTIT, role.getApplicationName());
            assertEquals(UserAdminHelper.ORG_ID_YENKA, role.getOrgId());
            //assertEquals(UserAdminHelper.ORG_NAME_YENKA, role.getOrganizationName()); //TODO figure out why orgName is not set.
        }

        assertEquals(2, propsAndRoles.size());

        UserPropertyAndRole role1 = propsAndRoles.get(0);
        assertEquals(UserAdminHelper.ROLE_NAME_GIFTIT_USER, role1.getRoleName());

        UserPropertyAndRole role2 = propsAndRoles.get(1);
        assertEquals(UserAdminHelper.ROLE_NAME_FACEBOOK_DATA, role2.getRoleName());
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
        String facebookDataAsString = WhydahUserResource.getFacebookDataAsString(input);
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

        String fbDataValueWithCdata = WhydahUserResource.getFacebookDataAsXmlString(fbUserDoc);
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
