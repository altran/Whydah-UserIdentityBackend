package net.whydah.identity.application;

import net.whydah.identity.audit.ActionPerformed;
import net.whydah.identity.audit.AuditLogDao;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class ApplicationService {
    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");

    private final ApplicationDao applicationDao;
    public static ApplicationService staticApplicationService;
    private final AuditLogDao auditLogDao;
//    private final LuceneApplicationIndexer luceneApplicationIndexer;


    @Autowired
    public ApplicationService(ApplicationDao applicationDao, AuditLogDao auditLogDao) {
        this.applicationDao = applicationDao;
        staticApplicationService = this;
        this.auditLogDao = auditLogDao;
        //this.luceneApplicationIndexer=luceneApplicationIndexer;
    }

    public static ApplicationService getApplicationService(){
        return staticApplicationService;
    }

    ////// CRUD

    public Application create(Application application) {
    	if(application.getId()==null||application.getId().isEmpty()){
    		return create(UUID.randomUUID().toString(), application);
    	} else {
    		if(getApplication(application.getId())!=null){
    			return null; //The id is already existing
    		} else {
    			return create(application.getId(), application);		
    		}
    	}
    }
    //used by ApplicationImporter, should be remove later
    public Application create(String applicationId, Application application) {
        application.setId(applicationId);
        int numRowsAffected = applicationDao.create(application);
 //       luceneApplicationIndexer.addToIndex(application);
        audit(ActionPerformed.ADDED, application.getId() + ", " + application.getName());
        return application;
    }

    public Application getApplication(String applicationId) {
        return applicationDao.getApplication(applicationId);
    }

    public List<Application> getApplications() {
        return applicationDao.getApplications();
    }

    public int update(Application application) {
        int numRowsAffected = applicationDao.update(application);
   //     luceneApplicationIndexer.update(application);
        audit(ActionPerformed.MODIFIED, application.getId() + ", " + application.getName());
        return numRowsAffected;
    }

    public int delete(String applicationId) {
        int numRowsAffected = applicationDao.delete(applicationId);
  //      luceneApplicationIndexer.removeFromIndex(applicationId);
        audit(ActionPerformed.DELETED, applicationId);
        return numRowsAffected;
    }

    private void audit(String action, String value) {
        String now = sdf.format(new Date());
        ActionPerformed actionPerformed = new ActionPerformed(value, now, action, "application", value);
        auditLogDao.store(actionPerformed);
    }


    ////// Authentication

    /**
     * @param credential    the application credential to verify
     * @return  application if successful authentication, else null
     */
    public Application authenticate(ApplicationCredential credential) {
        if (credential == null) {
            log.warn("authenticate - Missing ApplicationCredential ");
            return null;
        }
        //List<Application> applications = applicationDao.getApplications();
        Application application = applicationDao.getApplication(credential.getApplicationID().trim());
        if (application == null) {
            log.warn("authenticate - ApplicationID:{} - Not Found", credential.getApplicationID().trim());
            return null;
        }
        String applicationSecret = application.getSecurity().getSecret().trim();
        if (applicationSecret == null || applicationSecret.isEmpty()) {
            log.warn("secret is not set for applicationId=, name={}, all attempts at authentication will fail.", application.getId(), application.getName());
            return null;
        }
        if (applicationSecret.equals(credential.getApplicationSecret().trim())) {
            return application;
        }
        log.warn("authenticate - Incorrect applicationSectret ");
        return null;
    }
}
