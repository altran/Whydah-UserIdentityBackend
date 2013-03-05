package no.freecode.iam.service.repository;

import com.google.inject.Inject;
import no.freecode.iam.service.domain.Application;
import no.freecode.iam.service.exceptions.DatastoreException;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: asbkar
 * Date: 3/10/11
 * Time: 10:26 AM
 */
public class BackendConfigDataRepository {
    private static final String APPLICATIONS_SQL = "SELECT Id, Name, DefaultRole, DefaultOrgid from Applications";
    private static final String APPLICATION_SQL = APPLICATIONS_SQL + " WHERE id=?";
    private static final String ORG_SQL = "SELECT Name from Organization WHERE id=?";

    @Inject
    private QueryRunner queryRunner;

    public Application getApplication(String appid) {
        try {
            return queryRunner.query(APPLICATION_SQL, new ApplicationResultSetHandler(), appid);
        } catch (SQLException e) {
            throw new DatastoreException(e.getLocalizedMessage(), e);
        }
    }

    public List<Application> getApplications() {
        try {
            return queryRunner.query(APPLICATIONS_SQL, new ApplicationsResultSetHandler());
        } catch (SQLException e) {
            throw new DatastoreException(e.getLocalizedMessage(), e);
        }
    }

    public String getOrgname(String orgid) {
        try {
        return queryRunner.query(ORG_SQL, new OrgnameResultSetHandler(), orgid);
        } catch (SQLException e) {
            throw new DatastoreException(e.getLocalizedMessage(), e);
        }

    }
    public void setQueryRunner(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    private static class OrgnameResultSetHandler implements ResultSetHandler<String> {
        @Override
        public String handle(ResultSet rs) throws SQLException {
            if(rs.next()) {
                return rs.getString(1);
            } else {
                return null;
            }
        }
    }

    private static class ApplicationsResultSetHandler implements ResultSetHandler<List<Application>> {
        public List<Application> handle(ResultSet rs) throws SQLException {
            ArrayList<Application> apps = new ArrayList<Application>();
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
