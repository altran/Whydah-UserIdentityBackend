package net.whydah.identity.application;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.whydah.identity.audit.ActionPerformed;
import net.whydah.identity.audit.AuditLogDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by baardl on 29.03.14.
 */
@Singleton
public class ApplicationService {
    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");

    private final ApplicationDao applicationDao;
    private final AuditLogDao auditLogDao;

    @Inject
    public ApplicationService(ApplicationDao applicationDao, AuditLogDao auditLogDao) {
        this.applicationDao = applicationDao;
        this.auditLogDao = auditLogDao;
    }

    public Application createApplication(String applicationJson) throws java.lang.IllegalArgumentException {
        Application application=null;
        try {
             application = Application.fromJson(applicationJson);
            applicationDao.create(application);
            audit(ActionPerformed.ADDED, "application", application.toString());
        } catch (Exception e){
            throw new java.lang.IllegalArgumentException("Illegal application arguments:"+applicationJson);
        }
        return application;
    }

    private void audit(String action, String what, String value) {
        String now = sdf.format(new Date());
        ActionPerformed actionPerformed = new ActionPerformed(value, now, action, what, value);
        auditLogDao.store(actionPerformed);
    }


    public Application getApplication(String applicationId) {
        return applicationDao.getApplication(applicationId);
    }

    public List<Application> getApplications() {
        log.trace("Found applications:"+ applicationDao.getApplications().size());
        return applicationDao.getApplications();
    }

}
