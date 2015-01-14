package net.whydah.identity.dataimport;

import com.google.inject.Inject;
import com.googlecode.flyway.core.Flyway;
import com.googlecode.flyway.core.api.FlywayException;
import net.whydah.identity.config.AppConfig;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ensure database has the necessary DDL and all migrations have been applied.
 *
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a>
 */
public class DatabaseMigrationHelper {
    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationHelper.class);

    private Flyway flyway;
    private String dbInfo;

    @Inject
    public DatabaseMigrationHelper(BasicDataSource dataSource) {
        this.dbInfo = dataSource.getUrl();
        flyway = new Flyway();
        flyway.setDataSource(dataSource);
        setMigrationScriptLocation();
    }

    private void setMigrationScriptLocation() {
        String jdbcDriverString = AppConfig.appConfig.getProperty("roledb.jdbc.driver");
        String jdbcUrlString = AppConfig.appConfig.getProperty("roledb.jdbc.url");

        if (jdbcDriverString.contains("hsqldb")) {
            flyway.setLocations("db/migration/hssql");
        } else if(jdbcDriverString.contains("mysql")) {
            flyway.setLocations("db/migration/mysql");
        } else if (jdbcUrlString.contains("sqlserver")) {
            log.info("Expecting the MS SQL database to be pre-initialized with the latest schema. Automatic database migration is not supported.");
        } else {
            throw new RuntimeException("Unsupported database driver found in configuration - " + jdbcDriverString);
        }
    }

    public void upgradeDatabase() {
        try {
            flyway.migrate();
        } catch (FlywayException e) {
            throw new RuntimeException("Database upgrade failed using " + dbInfo, e);
        }
    }


    /*
    public void cleanDatabase() {
        try {
            flyway.clean();
        } catch (FlywayException e) {
            throw new RuntimeException("Database cleaning failed.", e);
        }
    }
    */
}
