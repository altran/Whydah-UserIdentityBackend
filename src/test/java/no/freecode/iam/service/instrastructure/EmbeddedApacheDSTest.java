package no.freecode.iam.service.instrastructure;


import junit.framework.TestCase;
import no.freecode.iam.service.ldap.EmbeddedADS;

import java.io.File;

public class EmbeddedApacheDSTest extends TestCase {
    private EmbeddedADS ads;
    public void setUp() throws Exception {
        File workDir = new File(System.getProperty("user.home") + "/bootstrapdata/ldap");
        workDir.mkdirs();
        // Create the server
        ads = new EmbeddedADS(workDir);

        // optionally we can start a server too
        ads.startServer(10368);
        super.setUp();
        Thread.sleep(2000);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        ads.stopServer();
    }

    public void testProcess() throws Exception {
/**        LDAPHelper ldap = new LDAPHelper();
        //System.in.read();
        try {
            ldap.addUser("ostroy", "Roy", "Ost", "myPW");
        } catch (NamingException e) {

        }*/
        Thread.sleep(30);
    }
}
