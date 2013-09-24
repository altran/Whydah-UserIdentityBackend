package net.whydah.iam.service.config;

import java.io.File;
import java.io.IOException;

import net.whydah.iam.service.exceptions.ConfigurationException;
import net.whydah.iam.service.ldap.LDAPHelper;
import net.whydah.iam.service.ldap.LdapAuthenticatorImpl;
import net.whydah.iam.service.search.Indexer;
import net.whydah.iam.service.search.Search;
import net.whydah.iam.service.security.SecurityTokenHelper;
import net.whydah.module.service.config.WhydahConfig;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

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

        String internalLdapUrl;
        String admPrincipal;
		String admCredentials;
		String usernameAttribute;
        
        WhydahConfig fCconfig = new WhydahConfig();
        //For Customer   
        if (fCconfig.getProptype().equals("DEV")){
        	internalLdapUrl = "internalLdapUrl FOR CUSTOMER";
            admPrincipal = "admPrincipal FOR CUSTOMER";
     		admCredentials = "admCredentials FOR CUSTOMER";
     		usernameAttribute="usernameAttribute FOR CUSTOMER";
        } else {
        	//For Whydah DEV or TEST
        	internalLdapUrl = fCconfig.getfCinternalLdapUrl(); 
            admPrincipal = fCconfig.getfCadmPrincipal();
    		admCredentials = fCconfig.getfCadmCredentials();
    		usernameAttribute = fCconfig.getfCusernameAttribute();
        }

//        int LDAP_PORT = new Integer(AppConfig.appConfig.getProperty("ldap.embedded.port"));
//        internalLdapUrl = "ldap://localhost:" + LDAP_PORT + "/dc=external,dc=WHYDAH,dc=no";


        bind(LdapAuthenticatorImpl.class).annotatedWith(Names.named("external")).toInstance(new LdapAuthenticatorImpl(externalLdapUrl, externalAdmPrincipal, externalAdmCredentials, externalUsernameAttribute));
        bind(LDAPHelper.class).toInstance(new LDAPHelper(externalLdapUrl, externalAdmPrincipal, externalAdmCredentials, externalUsernameAttribute));
        bind(LdapAuthenticatorImpl.class).annotatedWith(Names.named("internal")).toInstance(new LdapAuthenticatorImpl(internalLdapUrl, admPrincipal, admCredentials, usernameAttribute));
//        bind(LdapAuthenticatorImpl.class).annotatedWith(Names.named("internal")).toInstance(new LdapAuthenticatorImpl(externalLdapUrl, externalAdmPrincipal, externalAdmCredentials, externalUsernameAttribute));
    }
}
