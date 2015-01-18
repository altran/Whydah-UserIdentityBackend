package net.whydah.identity.application;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

//TODO Decide strategy to handle different SQL queries for different databases.
@Singleton
public class ApplicationDao {
    private static final Logger log = LoggerFactory.getLogger(ApplicationDao.class);

    private static String APPLICATIONS_SQL = "SELECT Id, Name, DefaultRoleName, DefaultOrgName from Applications";
    private static String APPLICATION_SQL = APPLICATIONS_SQL + " WHERE id=?";

    private JdbcTemplate jdbcTemplate;

    @Inject
    public ApplicationDao(BasicDataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);

        String jdbcDriverString = dataSource.getDriverClassName();
        if (jdbcDriverString.contains("mysql")) {
            APPLICATION_SQL = "SELECT Id, Name, DefaultRoleName, DefaultOrgName from Applications WHERE id=? GROUP BY ID";
            APPLICATIONS_SQL = "SELECT Id, Name, DefaultRoleName, DefaultOrgName from Applications GROUP BY ID ORDER BY Name ASC";
        }
    }


    public Application getApplication(String appid) {
        List<Application> applications = jdbcTemplate.query(APPLICATION_SQL, new String[]{appid}, new ApplicationMapper());
        if (applications.isEmpty()) {
            return null;
        }
        return applications.get(0);
    }

    public List<Application> getApplications() {
        return this.jdbcTemplate.query(APPLICATION_SQL, new ApplicationMapper());
    }


    /**
     * @param application
     * @return application, with new Id inserted.
     */
    public Application create(Application application) {
        Application applicationStored = null;
        //TODO Store availableOrdIds
        String sql = "INSERT INTO Applications (Id, Name, DefaultRoleName, DefaultOrgName) VALUES (?,?,?,?)";
        int numRowsUpdated = jdbcTemplate.update(sql, application.getId(), application.getName(), application.getDefaultRoleName(), application.getDefaultOrgName());
        if (numRowsUpdated > 0) {
            applicationStored = getApplication(application.getId());
            log.trace("Created application {}, numRowsUpdated {}", applicationStored.toString(), numRowsUpdated);
        }
        return applicationStored;
    }


    public int countApplications() {
        String sql = "SELECT count(*) FROM Applications";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        log.debug("applicationCount={}", count);
        return count;
    }

    private static final class ApplicationMapper implements RowMapper<Application> {
        public Application mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Application(rs.getString("Id"), rs.getString("Name"), rs.getString("DefaultRoleName"), rs.getString("DefaultOrgName"));
        }
    }
}
