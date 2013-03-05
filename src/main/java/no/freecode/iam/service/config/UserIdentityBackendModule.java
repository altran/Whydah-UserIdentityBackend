package no.freecode.iam.service.config;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import no.freecode.iam.service.exceptions.ConfigurationException;
import no.freecode.iam.service.ldap.LDAPHelper;
import no.freecode.iam.service.ldap.LdapAuthenticatorImpl;
import no.freecode.iam.service.search.Indexer;
import no.freecode.iam.service.search.Search;
import no.freecode.iam.service.security.SecurityTokenHelper;
import no.freecode.module.service.config.FCconfig;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;

import java.io.File;
import java.io.IOException;

/**
 * User: asbkar
 */
public class UserIdentityBackendModule extends AbstractModule {
	

	
    @Override
    protected void configure() {
        //datasource
        String jdbcdriver = AppConfig.appConfig.getProperty("roledb.jdbc.driver");
        String jdbcurl = AppConfig.appConfig.getProperty("roledb.jdbc.url");
        String roledbuser = AppConfig.appConfig.getProperty("roledb.jdbc.user");
        String roledbpasswd = AppConfig.appConfig.getProperty("roledb.jdbc.password");

        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(jdbcdriver);
        dataSource.setUrl(jdbcurl);//"jdbc:hsqldb:file:" + basepath + "hsqldb");
        dataSource.setUsername(roledbuser);
        dataSource.setPassword(roledbpasswd);
        QueryRunner queryRunner = new QueryRunner(dataSource);
        bind(QueryRunner.class).toInstance(queryRunner);
        try {
            String luceneDir = AppConfig.appConfig.getProperty("lucene.directory");
            Directory index = new NIOFSDirectory(new File(luceneDir));
            bind(Directory.class).toInstance(index);
            bind(Indexer.class).toInstance(new Indexer(index));
            bind(Search.class).toInstance(new Search(index));
        } catch (IOException e) {
            throw new ConfigurationException(e.getLocalizedMessage(), e);
        }

        String tokenserviceUri = AppConfig.appConfig.getProperty("securitytokenservice");
        bind(SecurityTokenHelper.class).toInstance(new SecurityTokenHelper(tokenserviceUri));

        String externalLdapUrl =  AppConfig.appConfig.getProperty("ldap.external.url");
        String externalAdmPrincipal =  AppConfig.appConfig.getProperty("ldap.external.principal");
        String externalAdmCredentials =  AppConfig.appConfig.getProperty("ldap.external.credentials");
        String externalUsernameAttribute =  AppConfig.appConfig.getProperty("ldap.external.usernameattribute");

      //String internalLdapUrl =  "ldap://hkdc03.obos.no:389/DC=obos,DC=no"; //TODO old
        
        String internalLdapUrl;
        String admPrincipal;
		String admCredentials;
		String usernameAttribute;
        
        FCconfig fCconfig = new FCconfig();
        //For Customer   
        if(fCconfig.getProptype().equals("DEV")){
        	internalLdapUrl = "internalLdapUrl FOR CUSTOMER";
            admPrincipal = "admPrincipal FOR CUSTOMER";
     		admCredentials = "admCredentials FOR CUSTOMER";
     		usernameAttribute="usernameAttribute FOR CUSTOMER";
        }else{
        	//For Freecode DEV or TEST
        	internalLdapUrl = fCconfig.getfCinternalLdapUrl(); 
            admPrincipal = fCconfig.getfCadmPrincipal();
    		admCredentials = fCconfig.getfCadmCredentials();
    		usernameAttribute = fCconfig.getfCusernameAttribute();
        }
        
        
        bind(LdapAuthenticatorImpl.class).annotatedWith(Names.named("external")).toInstance(new LdapAuthenticatorImpl(externalLdapUrl, externalAdmPrincipal, externalAdmCredentials, externalUsernameAttribute));
        bind(LDAPHelper.class).toInstance(new LDAPHelper(externalLdapUrl, externalAdmPrincipal, externalAdmCredentials, externalUsernameAttribute));
		//bind(LdapAuthenticatorImpl.class).annotatedWith(Names.named("internal")).toInstance(new LdapAuthenticatorImpl(internalLdapUrl, "ldap@obos.no", "NeSe1542", "sAMAccountName")); //TODO old
        bind(LdapAuthenticatorImpl.class).annotatedWith(Names.named("internal")).toInstance(new LdapAuthenticatorImpl(internalLdapUrl, admPrincipal, admCredentials, usernameAttribute)); //TODO new
        
   
    }
}
