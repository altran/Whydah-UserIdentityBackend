package net.whydah.identity.config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhydahConfig {

    private static final Logger logger = LoggerFactory.getLogger(WhydahConfig.class);
    private String proptype;
    private String propName ="";


    public String getProptype(){
        String propNameRes = getPropName();
        //logger.info("PT"+propNameRes);
        if(propNameRes.equals("DEV")){
            return proptype ="DEV";
        } else if (propNameRes.equals("TEST_LOCALHOST")){
            return proptype ="TEST_LOCALHOST";
        } else if (propNameRes.equals("TEST")){
            return proptype ="TEST";
        } else {
            return proptype ="TEST";
        }
    }


    public String getPropName(){
        return propName;
    }

    public void setPropName(String propName){
        this.propName = propName;
    }


    public String getfCinternalLdapUrl(){
    	if (getProptype().equals("TEST")){
            return "ldap://FOR whyDah TEST";
        } else if (getProptype().equals("TEST_LOCALHOST")){
    		return "ldap://FOR whyDah TEST LOCALHOST";
        } else {
            return "";
        }

    }


    public String getfCadmPrincipal(){
    	if (getProptype().equals("TEST")){
            return "admPrincipal whyDah TEST";
        } else if (getProptype().equals("TEST_LOCALHOST")){
    		return "admPrincipal whyDah TEST LOCALHOST";
        } else {
            return "";

        }

    }

    public String getfCadmCredentials(){
        
    	if (getProptype().equals("TEST")){
            return "admCredentials TEST";
    	} else if (getProptype().equals("TEST_LOCALHOST")){
    		return "admCredentials TEST LOCALHOST";
        } else{
            return "";
        }

    }

    public String getfCusernameAttribute(){
    	if (getProptype().equals("TEST")){
            return "usernameAttribute TEST";
    	} else if (getProptype().equals("TEST_LOCALHOST")){
    		return "usernameAttribute TEST LOCALHOST";
        } else {
            return "";
        }

    }

    public String getFCDc(){
        
    	if (getProptype().equals("TEST")){
    		return "whyDah TEST"; 
    	} else if (getProptype().equals("TEST_LOCALHOST")){
    		return "whyDah TEST LOCALHOST"; 
        } else {
        	return "whyDah"; 
        }
    }
//
//    public void setfCinternalLdapUrl(String fCinternalLdapUrl){
//        this.whyDahInternalLdapUrl = fCinternalLdapUrl;
//    }
//
//    public void setfCadmPrincipal(String fCadmPrincipal){
//        this.whyDahAdmPrincipal = fCadmPrincipal;
//    }
//
//    public void setfCadmCredentials(String fCadmCredentials){
//        this.whyDahAdmCredentials = fCadmCredentials;
//    }
//
//    public void setfCusernameAttribute(String fCusernameAttribute){
//        this.whyDahUsernameAttribute=fCusernameAttribute;
//    }
//
//    public void setFCDc(String fCdc){
//        this.whyDahDc = fCdc;
//    }
}
