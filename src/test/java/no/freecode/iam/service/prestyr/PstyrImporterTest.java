package no.freecode.iam.service.prestyr;

import no.freecode.iam.service.dataimport.DatabaseHelper;
import no.freecode.iam.service.dataimport.PstyrImporter;
import no.freecode.iam.service.domain.UserPropertyAndRole;
import no.freecode.iam.service.domain.WhydahUserIdentity;
import no.freecode.iam.service.ldap.EmbeddedADS;
import no.freecode.iam.service.ldap.LDAPHelper;
import no.freecode.iam.service.repository.BackendConfigDataRepository;
import no.freecode.iam.service.repository.UserPropertyAndRoleRepository;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PstyrImporterTest {
    private final static String basepath = "/tmp/pstyrimp/";
    private final static String dbpath = basepath + "hsqldb/roles";
    private final static String ldappath = basepath + "hsqldb/ldap/";
    private final static int LDAP_PORT = 10935;
    private UserPropertyAndRoleRepository userPropertyAndRoleRepository;
    private PstyrImporter pstyrImporter;
    private EmbeddedADS ads;
    private Directory index;
    private LDAPHelper ldapHelper;

    @Before
    public void setUp() throws Exception {
        deleteDirectory(new File(basepath));
        index = new NIOFSDirectory(new File(basepath + "lucene"));

        File ldapdir = new File(ldappath);
        ldapdir.mkdirs();
        ads = new EmbeddedADS(ldappath);
        ads.startServer(LDAP_PORT);
        pstyrImporter = new PstyrImporter();
        ldapHelper = new LDAPHelper("ldap://localhost:10935/dc=external,dc=OBOS,dc=no", "uid=admin,ou=system", "secret", "initials");
        pstyrImporter.setLdapHelper(ldapHelper);
        userPropertyAndRoleRepository = new UserPropertyAndRoleRepository();
        pstyrImporter.setRoleRepository(userPropertyAndRoleRepository);

        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setUrl("jdbc:hsqldb:file:" + dbpath);
        QueryRunner queryRunner = new QueryRunner(dataSource);
        pstyrImporter.setQueryRunner(queryRunner);
        userPropertyAndRoleRepository.setQueryRunner(queryRunner);
        BackendConfigDataRepository bcdr = new BackendConfigDataRepository();
        bcdr.setQueryRunner(queryRunner);
        userPropertyAndRoleRepository.setBackendConfigDataRepository(bcdr);

        pstyrImporter.setDatabaseHelper(new DatabaseHelper(queryRunner));
    }

    @After
    public void tearDown() throws Exception {
        ads.stopServer();
    }

    static public boolean deleteDirectory(File path) {
        if( path.exists() ) {
            File[] files = path.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        return( path.delete() );
    }

    @Test
    public void testImport() throws Exception {
        //pstyrImporter.importUsers("testxstyr.csv", index);
        pstyrImporter.setIndex(index);
        pstyrImporter.importUsers("testxstyrFreecode.csv");
        WhydahUserIdentity whydahUserIdentity = ldapHelper.getUserinfo("rafal.laczek@freecode.no");
        assertNotNull(whydahUserIdentity);
        assertEquals("rafal.laczek@freecode.no", whydahUserIdentity.getUsername());
        assertEquals("rafal.laczek@freecode.no", whydahUserIdentity.getEmail());
        assertNotNull(whydahUserIdentity.getPersonRef());
        assertNotNull(whydahUserIdentity.getUid());
        assertNotNull(whydahUserIdentity.getFirstName());
        assertNotNull(whydahUserIdentity.getLastName());
        List<UserPropertyAndRole> roles = userPropertyAndRoleRepository.getUserPropertyAndRoles(whydahUserIdentity.getUid());
        assertNotNull(roles);
        assertEquals(1, roles.size());
        UserPropertyAndRole role = roles.get(0);
        assertEquals(whydahUserIdentity.getUid(), role.getUid());
        assertEquals("0001", role.getOrgId());
        assertEquals("DEV", role.getRoleName());
    }
}
