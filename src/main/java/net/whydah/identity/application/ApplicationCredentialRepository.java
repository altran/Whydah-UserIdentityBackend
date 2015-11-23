package net.whydah.identity.application;

import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationCredential;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by baardl on 23.11.15.
 */
@Repository
public class ApplicationCredentialRepository {
    private static final Logger log = getLogger(ApplicationCredentialRepository.class);

    private final ApplicationDao applicationDao;
    private ApplicationCredential uibAppCred = null;
    private ApplicationCredential uasAppCred = null;
    private ApplicationCredential stsAppCred = null;

    private final String uibAppId;
    private final String stsAppId;
    private final String uasAppId;


    @Autowired
    public ApplicationCredentialRepository(ApplicationDao applicationDao, @Value("my_applicationid") String uibAppId,
                                           @Value("securitytokenservice_applicationid") String stsAppId,
                                           @Value("uas_applicationid") String uasAppId) {
        this.applicationDao = applicationDao;
        this.uibAppId = uibAppId;
        this.stsAppId = stsAppId;
        this.uasAppId = uasAppId;
    }

    public ApplicationCredential getUibAppCred() {
        if (uibAppCred == null) {
            uibAppCred = getAppCredentialForApplicationId(uibAppId);
        }
        return uibAppCred;
    }

    public ApplicationCredential getUasAppCred() {
        if (uasAppCred == null) {
            uasAppCred = getAppCredentialForApplicationId(uasAppId);
        }
        return uasAppCred;
    }

    public ApplicationCredential getStsAppCred() {
        if (stsAppCred == null) {
            stsAppCred = getAppCredentialForApplicationId(stsAppId);
        }
        return stsAppCred;
    }

    protected ApplicationCredential getAppCredentialForApplicationId(String applicationId){
        Application app = applicationDao.getApplication(applicationId);
        String appid = app.getId();
        String appname=app.getName();
        String secret=app.getSecurity().getSecret();
        return new ApplicationCredential(appid,appname,secret);
    }
}
