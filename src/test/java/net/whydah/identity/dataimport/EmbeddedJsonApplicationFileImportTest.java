package net.whydah.identity.dataimport;

import com.jayway.restassured.RestAssured;
import net.whydah.identity.Main;
import net.whydah.identity.application.ApplicationDao;
import net.whydah.identity.application.ApplicationService;
import net.whydah.identity.audit.AuditLogDao;
import net.whydah.identity.config.ApplicationMode;
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


public class EmbeddedJsonApplicationFileImportTest {
    private static final Logger log = LoggerFactory.getLogger(EmbeddedJsonApplicationFileImportTest.class);

    private BasicDataSource dataSource;
    private Main main;
    private String applicationsImportSource;
   
    
    @BeforeClass
    public void startServer() {
        ApplicationMode.setCIMode();
        final ConstrettoConfiguration configuration = new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
                .addResource(Resource.create("classpath:useridentitybackend-test.properties"))
                .done()
                .getConfiguration();


        String roleDBDirectory = configuration.evaluateToString("roledb.directory");
        applicationsImportSource = configuration.evaluateToString("import.applicationssource");
        FileUtils.deleteDirectory(roleDBDirectory);
        dataSource = initBasicDataSource(configuration);
        DatabaseMigrationHelper dbHelper = new DatabaseMigrationHelper(dataSource);
        dbHelper.cleanDatabase();
        dbHelper.upgradeDatabase();

        main = new Main(6655);
        main.startEmbeddedDS(configuration.asMap());

        main.startJetty();
        RestAssured.port = main.getPort();
        RestAssured.basePath = Main.CONTEXT_PATH;
    }

    private BasicDataSource initBasicDataSource(ConstrettoConfiguration configuration) {
        String jdbcdriver = configuration.evaluateToString("roledb.jdbc.driver");
        String jdbcurl = configuration.evaluateToString("roledb.jdbc.url");
        String roledbuser = configuration.evaluateToString("roledb.jdbc.user");
        String roledbpasswd = configuration.evaluateToString("roledb.jdbc.password");

        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(jdbcdriver);
        dataSource.setUrl(jdbcurl);
        dataSource.setUsername(roledbuser);
        dataSource.setPassword(roledbpasswd);
        return dataSource;
    }

    @AfterClass
    public void stop() {
        if (main != null) {
            main.stop();
        }
        
        try {
        	if(!dataSource.isClosed()) {
        		dataSource.close();
        	}
		} catch (SQLException e) {
			log.error("", e);
		}
    }

    @Test
    public void testEmbeddedJsonApplicationsFile(ConstrettoConfiguration configuration) throws IOException {
        InputStream ais = null;
        try {
            ais = openInputStream("Applications", applicationsImportSource);
            log.debug("Testimporting:"+applicationsImportSource);

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

    InputStream openInputStream(String tableName, String importSource) {
        InputStream is;
        if (FileUtils.localFileExist(importSource)) {
            is = FileUtils.openLocalFile(importSource);
        } else {
            is = FileUtils.openFileOnClasspath(importSource);
        }
        return is;
    }

}
