package net.whydah.module.service.config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FCconfig {

    private static final Logger logger = LoggerFactory.getLogger(FCconfig.class);
    private String proptype;
    private String fCinternalLdapUrl;
    private String fCadmPrincipal;
    private String fCadmCredentials;
    private String fCusernameAttribute;
    private String fCdc;
    private String propName ="";


    public String getProptype(){
        String propNameRes = getPropName();
        //logger.info("PT"+propNameRes);
        if(propNameRes.equals("DEV")){
            return proptype ="DEV";
        }else if (propNameRes.equals("FCDEV")){
            return proptype ="FCDEV";
        }else if(propNameRes.equals("TEST")){
            return proptype ="TEST";
        }else{
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
        if(getProptype().equals("FCDEV")){
            return fCinternalLdapUrl = "ldap://FOR FREECODE DEV";
        }else if (getProptype().equals("TEST")){
            return fCinternalLdapUrl="ldap://FOR FREECODE TEST";
        }else{
            return fCinternalLdapUrl="";
        }

    }


    public String getfCadmPrincipal(){
        if(getProptype().equals("FCDEV")){
            return fCadmPrincipal = "admPrincipal FREECODE DEV";
        }else if (getProptype().equals("TEST")){
            return fCadmPrincipal = "admPrincipal FREECODE TEST";

        }else{
            return fCadmPrincipal = "";

        }

    }

    public String getfCadmCredentials(){
        if(getProptype().equals("FCDEV")){
            return fCadmCredentials = "admCredentials FREECODE DEV";
        }else if(getProptype().equals("TEST")){
            return fCadmCredentials = "admCredentials TEST";
        }else{
            return fCadmCredentials="";
        }

    }

    public String getfCusernameAttribute(){
        if(getProptype().equals("FCDEV")){
            return fCusernameAttribute = "usernameAttribute FREECODE DEV";
        }else if(getProptype().equals("TEST")){
            return fCusernameAttribute = "usernameAttribute TEST";
        }else{
            return fCusernameAttribute="";
        }

    }

    public String getFCDc(){
        return fCdc="Freecode"; //TODO Finally this value may be little bit different
    }


    public void setfCinternalLdapUrl(String fCinternalLdapUrl){
        this.fCinternalLdapUrl = fCinternalLdapUrl;
    }

    public void setfCadmPrincipal(String fCadmPrincipal){
        this.fCadmPrincipal = fCadmPrincipal;
    }

    public void setfCadmCredentials(String fCadmCredentials){
        this.fCadmCredentials = fCadmCredentials;
    }

    public void setfCusernameAttribute(String fCusernameAttribute){
        this.fCusernameAttribute=fCusernameAttribute;
    }

    public void setFCDc(String fCdc){
        this.fCdc = fCdc;
    }





}
