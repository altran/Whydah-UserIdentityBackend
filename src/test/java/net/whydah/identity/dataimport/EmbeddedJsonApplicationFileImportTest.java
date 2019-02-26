package net.whydah.identity.dataimport;

import net.whydah.identity.Main;
import net.whydah.identity.application.ApplicationDao;
import net.whydah.identity.application.ApplicationService;
import net.whydah.identity.audit.AuditLogDao;
import net.whydah.identity.config.ApplicationMode;
import net.whydah.identity.ldapserver.EmbeddedADS;
import net.whydah.identity.util.FileUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.constretto.ConstrettoBuilder;
import org.constretto.ConstrettoConfiguration;
import org.constretto.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Map;


public class EmbeddedJsonApplicationFileImportTest {
    private static final Logger log = LoggerFactory.getLogger(EmbeddedJsonApplicationFileImportTest.class);
    private static final String ldapPath = "target/EmbeddedJsonApplicationFileImportTest/ldap";

    private BasicDataSource dataSource;
    private Main main;
    private String applicationsImportSource;
   
    
    @BeforeClass
    public void startServer() {
        ApplicationMode.setCIMode();
        final ConstrettoConfiguration config = new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
                .addResource(Resource.create("classpath:useridentitybackend-test.properties"))
                .done()
                .getConfiguration();


        String roleDBDirectory = config.evaluateToString("roledb.directory");
        FileUtils.deleteDirectory(roleDBDirectory);

        applicationsImportSource = config.evaluateToString("import.applicationssource");
        dataSource = Main.initBasicDataSource(config);

        DatabaseMigrationHelper dbHelper = new DatabaseMigrationHelper(dataSource);
        dbHelper.cleanDatabase();
        dbHelper.upgradeDatabase();


        Map<String, String> ldapProperties = Main.ldapProperties(config);
        ldapProperties.put("ldap.embedded.directory", ldapPath);
        ldapProperties.put(EmbeddedADS.PROPERTY_BIND_PORT, "10689");
        ldapProperties.put("ldap.primary.url", "ldap://localhost:10689/dc=people,dc=whydah,dc=no");
        FileUtils.deleteDirectories(ldapPath);

        main = new Main(6648);
        main.startEmbeddedDS(ldapProperties);
    }



    @AfterClass
    public void stop() {
        if (main != null) {
            main.stopEmbeddedDS();
        }
        
        try {
        	if(!dataSource.isClosed()) {
        		dataSource.close();
        	}
		} catch (SQLException e) {
			log.error("", e);
		}

        FileUtils.deleteDirectories(ldapPath);
    }

    @Test
    public void testEmbeddedJsonApplicationsFile(ConstrettoConfiguration configuration) throws IOException {
        InputStream ais = null;
        try {
            ais = openInputStream("Applications", applicationsImportSource);
            log.debug("Testimporting:" + applicationsImportSource);

            ApplicationService applicationService = new ApplicationService(new ApplicationDao(dataSource), new AuditLogDao(dataSource), null, null);

            if (applicationsImportSource.endsWith(".csv")) {
                new ApplicationImporter(applicationService).importApplications(ais);
            } else {
                new ApplicationJsonImporter(applicationService,configuration).importApplications(ais);
            }
        } finally {
            FileUtils.close(ais);

        }
    }

    private InputStream openInputStream(String tableName, String importSource) {
        InputStream is;
        if (FileUtils.localFileExist(importSource)) {
            is = FileUtils.openLocalFile(importSource);
        } else {
            is = FileUtils.openFileOnClasspath(importSource);
        }
        return is;
    }
}
