package net.whydah.identity.application;

import net.whydah.identity.audit.ActionPerformed;
import net.whydah.identity.audit.AuditLogDao;
import net.whydah.sso.application.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by baardl on 29.03.14.
 */
@Service
public class ApplicationService {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");

    private final ApplicationDao applicationDao;
    private final AuditLogDao auditLogDao;

    @Autowired
    public ApplicationService(ApplicationDao applicationDao, AuditLogDao auditLogDao) {
        this.applicationDao = applicationDao;
        this.auditLogDao = auditLogDao;
    }

    public Application create(Application application) {
        return create(UUID.randomUUID().toString(), application);
    }
    //used by ApplicationImporter, should be remove later
    public Application create(String applicationId, Application application) {
        application.setId(applicationId);
        Application persisted = applicationDao.create(application);
        audit(ActionPerformed.ADDED, application.getId() + ", " + application.getName());
        return persisted;
    }

    public Application getApplication(String applicationId) {
        return applicationDao.getApplication(applicationId);
    }

    public List<Application> getApplications() {
        List<Application> applications = applicationDao.getApplications();
        return applications;
    }

    public void update(Application application) {
        applicationDao.update(application);
        audit(ActionPerformed.MODIFIED, application.getId() + ", " + application.getName());
    }

    public void delete(String applicationId) {
        applicationDao.delete(applicationId);
        audit(ActionPerformed.DELETED, applicationId);
    }

    private void audit(String action, String value) {
        String now = sdf.format(new Date());
        ActionPerformed actionPerformed = new ActionPerformed(value, now, action, "application", value);
        auditLogDao.store(actionPerformed);
    }
}
