package net.whydah.identity.audit;

import com.google.inject.Inject;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

public class AuditLogDao {
    private JdbcTemplate jdbcTemplate;

    @Inject
    public AuditLogDao(BasicDataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void store(ActionPerformed actionPerformed) {
        String sql = "INSERT INTO AUDITLOG (userid, timestamp, action, field, value) values (?,?,?,?,?)";

        jdbcTemplate.update(sql,
                actionPerformed.getUserId(),
                actionPerformed.getTimestamp(),
                actionPerformed.getAction(),
                actionPerformed.getField(),
                actionPerformed.getValue()
        );
    }
}
