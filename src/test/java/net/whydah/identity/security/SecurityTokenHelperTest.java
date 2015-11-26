package net.whydah.identity.security;

import net.whydah.identity.application.ApplicationService;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationCredential;
import org.junit.Test;

public class SecurityTokenHelperTest {

    String applicationToken=" <?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n" +
            "  <applicationtoken>\n" +
            "     <params>\n" +
            "         <applicationtokenID>1cca06386f52f91d9610aa1dbd95b9a9</applicationtokenID>\n" +
            "         <applicationid>2210</applicationid>\n" +
            "         <applicationname>Whydah-UserIdentityBackend</applicationname>\n" +
            "         <expires>1448277365860</expires>\n" +
            "     </params> \n" +
            "     <Url type=\"application/xml\" method=\"POST\"                 template=\"https://whydahdev.cantara.no/tokenservice/user/1cca06386f52f91d9610aa1dbd95b9a9/get_usertoken_by_usertokenid\"/> \n" +
            " </applicationtoken>\n";

    @Test
    public void testParsingOfApplicationToken(){
        ApplicationService applicationService = ApplicationService.getApplicationService();
        Application app = applicationService.getApplication(applicationToken);
        String appid = app.getId();
        String appname=app.getName();
        String secret=app.getSecurity().getSecret();
         new ApplicationCredential(appid,appname,secret);
    }
}
