package net.whydah.identity.application;

import net.whydah.identity.application.search.LuceneApplicationIndexer;
import net.whydah.identity.application.search.LuceneApplicationSearch;
import net.whydah.identity.audit.ActionPerformed;
import net.whydah.identity.audit.AuditLogDao;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.util.Lock;

import org.apache.commons.collections.map.HashedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
public class ApplicationService {
    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");

    private final ApplicationDao applicationDao;
    private final AuditLogDao auditLogDao;
    private final LuceneApplicationIndexer luceneApplicationIndexer;
    private final LuceneApplicationSearch luceneApplicationSearch;
    public static boolean skipImportFromLuceneIndex = false;

    @Autowired
    public ApplicationService(ApplicationDao applicationDao, AuditLogDao auditLogDao, @Qualifier("luceneApplicationsDirectory") LuceneApplicationIndexer luceneApplicationIndexer, LuceneApplicationSearch luceneApplicationSearch) {
        this.applicationDao = applicationDao;
        this.auditLogDao = auditLogDao;
        this.luceneApplicationIndexer = luceneApplicationIndexer;
        this.luceneApplicationSearch = luceneApplicationSearch;
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
        if(numRowsAffected>0) {
        	luceneApplicationIndexer.addToIndex(application);
        }
        audit(ActionPerformed.ADDED, application.getId() + ", " + application.getName());
        return application;
    }

    public List<Application> search(String applicationQuery) {
        return luceneApplicationSearch.search(applicationQuery);
    }

    public Application getApplication(String applicationId) {
        return applicationDao.getApplication(applicationId);
    }
    
    Lock lock = new Lock();
    public List<Application> getApplications() {
        List<Application> applicationDBList = applicationDao.getApplications();
        Map<String, Application> dbMap = new HashMap<>();
        for(Application a : applicationDBList) {
        	dbMap.put(a.getId(), a);
        }
        int indexSize = luceneApplicationSearch.getApplicationIndexSize();
        if(!lock.isLocked()){
        	try {
				lock.lock();
				
				if (applicationDBList.size() > indexSize) {
					
	        		for (Application application : applicationDBList) {
	        			
	        			if(!luceneApplicationSearch.isApplicationExists(application.getId())) {
	        				if(luceneApplicationIndexer.addToIndex(application)) {
		        				log.info("new application: " + application.getName() + " is added to the Lucene index");
		        			} else {
		        				luceneApplicationIndexer.addActionQueue.clear(); //no need to queue, try again later
		        				log.error("failed to add the new application: " + application.getName() + " to the Lucene index");
		        			}
	        			}	        			
	        		}	        		
	        	}
				 //this is an unexpected condition, avoid data loss if something went wrong 
				else if (applicationDBList.size() < indexSize)  {
					log.warn("This is an unexpected condition, lucene data list has more items than the database list");
					if(!skipImportFromLuceneIndex) {
						log.info("New items in lucene indexer is being imported into DB");					
						//merge the applications from lucene to DB
						List<Application> applicationLuceneList = luceneApplicationSearch.search("*");
						for(Application app : applicationLuceneList) {
							if(!dbMap.containsKey(app.getId())) {
								log.info("application id={}, appName={} is not existing in the DB. Try to create and import", app.getId(), app.getName());
								applicationDao.create(app);
								applicationDBList.add(app);
							}
						}
					} else {
						log.warn("Skipped the import process");
					}
	        	}
				
			} catch (InterruptedException e) {
				
			} finally{
				lock.unlock();
			}
        }
        return applicationDBList;
    }

    public int update(Application application) {
        int numRowsAffected = applicationDao.update(application);
        luceneApplicationIndexer.updateIndex(application);
        audit(ActionPerformed.MODIFIED, application.getId() + ", " + application.getName());
        return numRowsAffected;
    }

    public int delete(String applicationId) {
        int numRowsAffected = applicationDao.delete(applicationId);
        luceneApplicationIndexer.removeFromIndex(applicationId);
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
            log.warn("authenticate - ApplicationID:{}- name={} - Not Found", credential.getApplicationID(), application.getName());
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
