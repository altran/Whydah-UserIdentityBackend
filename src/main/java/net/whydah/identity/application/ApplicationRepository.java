package net.whydah.identity.application;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.whydah.identity.config.AppConfig;
import net.whydah.identity.dataimport.DatabaseHelper;
import net.whydah.identity.user.role.DatastoreException;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author asbkar
 */
@Singleton
public class ApplicationRepository {
    private static final Logger log = LoggerFactory.getLogger(ApplicationRepository.class);

    private static String APPLICATIONS_SQL = "SELECT Id, Name, DefaultRoleName, DefaultOrgName GROUP BY ID from Applications";
    private static String APPLICATION_SQL = APPLICATIONS_SQL + " WHERE id=?";

    private QueryRunner queryRunner;

    @Inject
    public ApplicationRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
        String jdbcDriverString = AppConfig.appConfig.getProperty("roledb.jdbc.driver");
        if (jdbcDriverString.contains("hsqldb")) {
            APPLICATIONS_SQL = "SELECT Id, Name, DefaultRoleName, DefaultOrgName from Applications";
        } else if(jdbcDriverString.contains("mysql")) {
            APPLICATION_SQL = APPLICATIONS_SQL + " WHERE id=?  GROUP BY ID";
            APPLICATIONS_SQL = "SELECT Id, Name, DefaultRoleName, DefaultOrgName ID from Applications GROUP BY ID";
        }
        }

    public Application getApplication(String appid) {
        try {
            return queryRunner.query(APPLICATION_SQL, new ApplicationResultSetHandler(), appid);
        } catch (SQLException e) {
            throw new DatastoreException(e);
        }
    }

    public List<Application> getApplications() {
        try {
            return queryRunner.query(APPLICATIONS_SQL, new ApplicationsResultSetHandler());
        } catch (SQLException e) {
            throw new DatastoreException(e);
        }
    }


    public void setQueryRunner(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    /**
     *
     * @param application
     * @return application, with new Id inserted.
     */
    public Application create(Application application) {
        Application applicationStored = null;
        int numRowsUpdated = -1;
        //TODO Store availableOrdIds
        try {
            numRowsUpdated = queryRunner.update("INSERT INTO Applications (Id, Name, DefaultRoleName, DefaultOrgName) VALUES (?,?,?,?)",
                    application.getId(), application.getName(), application.getDefaultRoleName(), application.getDefaultOrgName());
            if (numRowsUpdated > 0) {
                applicationStored = getApplication(application.getId());
                log.trace("Created application {}, numRowsUpdated {}", applicationStored.toString(), numRowsUpdated);
            }
        } catch (SQLException e) {
            throw new DatastoreException(e);
        }
        return applicationStored;
    }


    private static class ApplicationsResultSetHandler implements ResultSetHandler<List<Application>> {
        public List<Application> handle(ResultSet rs) throws SQLException {
            ArrayList<Application> apps = new ArrayList<>();
            while(rs.next()) {
                apps.add(new Application(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
            }
            return apps;
        }
    }

    private static class ApplicationResultSetHandler implements ResultSetHandler<Application> {
        @Override
        public Application handle(ResultSet rs) throws SQLException {
            if(rs.next()) {
                return new Application(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4));
            } else {
                return null;
            }
        }
    }
}
