package net.whydah.identity.repository;

import com.google.inject.Inject;
import net.whydah.identity.audit.ActionPerformed;
import net.whydah.identity.exceptions.DatastoreException;
import org.apache.commons.dbutils.QueryRunner;

import java.sql.SQLException;

public class AuditLogRepository {
    private static final String INSERT_ROW = "INSERT INTO AUDITLOG (userid, timestamp, action, field, value) values (?,?,?,?,?)";

    private QueryRunner queryRunner;

    @Inject
    public AuditLogRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    public void store(ActionPerformed actionPerformed) {
        try {
            queryRunner.update(INSERT_ROW,
                    actionPerformed.getUserId(),
                    actionPerformed.getTimestamp(),
                    actionPerformed.getAction(),
                    actionPerformed.getField(),
                    actionPerformed.getValue()
            );
        } catch (SQLException e) {
            throw new DatastoreException(e.getLocalizedMessage(), e);
        }
    }
}
