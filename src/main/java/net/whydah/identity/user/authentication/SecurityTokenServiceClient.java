package net.whydah.identity.user.authentication;

import net.whydah.identity.application.ApplicationService;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.commands.userauth.CommandGetUsertokenByUsertokenId;
import net.whydah.sso.session.WhydahApplicationSession;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserToken;
import org.constretto.annotation.Configuration;
import org.constretto.annotation.Configure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class SecurityTokenServiceClient {
    private static final Logger log = LoggerFactory.getLogger(SecurityTokenServiceClient.class);

    private static String MY_APPLICATION_ID = "2210";
    private static String securitytokenserviceurl;
    private static WhydahApplicationSession was = null;
    private static SecurityTokenServiceClient securityTokenServiceClient;
    private String sts;

    @Autowired
    @Configure
    public SecurityTokenServiceClient(@Configuration("securitytokenservice") String securitytokenserviceurl, @Configuration("my_applicationid") String MY_APPLICATION_ID) {
        this.MY_APPLICATION_ID = MY_APPLICATION_ID;
        this.securitytokenserviceurl = securitytokenserviceurl;
        securityTokenServiceClient = this;
    }


    public static SecurityTokenServiceClient getSecurityTokenServiceClient() {
        return securityTokenServiceClient;
    }

    public String getActiveUibApplicationTokenId(){
        if (was == null) {
            try {
                ApplicationCredential myApplicationCredential = getAppCredentialForApplicationId(this.MY_APPLICATION_ID);
                was = WhydahApplicationSession.getInstance(securitytokenserviceurl,
                        null,  // No UAS
                        myApplicationCredential.getApplicationID(),
                        myApplicationCredential.getApplicationName(),
                        myApplicationCredential.getApplicationSecret());
            } catch (Exception e) {
                log.warn("Unable to create WhydahSession", e);
            }

        }
        if (was != null) {
            return was.getActiveApplicationTokenId();
        }
        return null;
    }

    public UserToken getUserToken(String usertokenid){
        if (was == null) {
            try {
                ApplicationCredential myApplicationCredential = getAppCredentialForApplicationId(this.MY_APPLICATION_ID);
                was = WhydahApplicationSession.getInstance(securitytokenserviceurl,
                        null,  // No UAS
                        myApplicationCredential.getApplicationID(),
                        myApplicationCredential.getApplicationName(),
                        myApplicationCredential.getApplicationSecret());
            } catch (Exception e) {
                log.warn("Unable to create WhydahSession", e);
            }

        }
        if (was != null) {
            String userTokenXML = new CommandGetUsertokenByUsertokenId(URI.create(was.getSTS()), was.getActiveApplicationTokenId(), was.getActiveApplicationTokenXML(), usertokenid).execute();
            if (userTokenXML != null && userTokenXML.length() > 10) {
                return UserTokenMapper.fromUserTokenXml(userTokenXML);
            } else {
                // Attempt to auto-recover if sts-state has been lost
                was.renewWhydahApplicationSession();
            }
        }
        return null;
    }

    public WhydahApplicationSession getWAS() {
        if (was == null) {
            try {
                ApplicationCredential myApplicationCredential = getAppCredentialForApplicationId(this.MY_APPLICATION_ID);
                was = WhydahApplicationSession.getInstance(securitytokenserviceurl,
                        null,  // No UAS
                        myApplicationCredential.getApplicationID(),
                        myApplicationCredential.getApplicationName(),
                        myApplicationCredential.getApplicationSecret());
            } catch (Exception e) {
                log.warn("Unable to create WhydahSession", e);
            }

        }
        return was;
    }


    private ApplicationCredential getAppCredentialForApplicationId(String appNo){
        ApplicationService applicationService = ApplicationService.getApplicationService();
        Application app = applicationService.getApplication(appNo);
        String appid = app.getId();
        String appname=app.getName();
        String secret=app.getSecurity().getSecret();
        return new ApplicationCredential(appid,appname,secret);
    }
}
