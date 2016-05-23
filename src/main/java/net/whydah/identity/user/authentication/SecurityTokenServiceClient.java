package net.whydah.identity.user.authentication;

import net.whydah.identity.application.ApplicationService;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.session.baseclasses.BaseWhydahServiceClient;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserToken;
import org.constretto.annotation.Configuration;
import org.constretto.annotation.Configure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SecurityTokenServiceClient {
    private static final Logger log = LoggerFactory.getLogger(SecurityTokenServiceClient.class);

    private static String MY_APPLICATION_ID = "2210";
    private static String securitytokenserviceurl;
    private static BaseWhydahServiceClient bas = null;
    private String sts;

    @Autowired
    @Configure
    public SecurityTokenServiceClient(@Configuration("securitytokenservice") String securitytokenserviceurl, @Configuration("my_applicationid") String MY_APPLICATION_ID) {
        this.MY_APPLICATION_ID = MY_APPLICATION_ID;
        this.securitytokenserviceurl = securitytokenserviceurl;
    }

    public String getActiveUibApplicationTokenId(){
        if (bas == null) {
            try {
                ApplicationCredential myApplicationCredential = getAppCredentialForApplicationId(this.MY_APPLICATION_ID);
                bas = new BaseWhydahServiceClient(securitytokenserviceurl,
                        "",  // No UAS
                        myApplicationCredential.getApplicationID(),
                        myApplicationCredential.getApplicationName(),
                        myApplicationCredential.getApplicationSecret());
            } catch (Exception e) {
                log.warn("Unable to create WhydahSession");
            }

        }
        return bas.getMyAppTokenID();
    }

    public UserToken getUserToken(String usertokenid){
        if (bas == null) {
            try {
                ApplicationCredential myApplicationCredential = getAppCredentialForApplicationId(this.MY_APPLICATION_ID);
                bas = new BaseWhydahServiceClient(securitytokenserviceurl,
                        "",  // No UAS
                        myApplicationCredential.getApplicationID(),
                        myApplicationCredential.getApplicationName(),
                        myApplicationCredential.getApplicationSecret());
            } catch (Exception e) {
                log.warn("Unable to create WhydahSession");
            }

        }
        return UserTokenMapper.fromUserTokenXml(bas.getUserTokenByUserTokenID(usertokenid));
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
