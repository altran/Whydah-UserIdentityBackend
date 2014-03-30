package net.whydah.identity.config;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import net.whydah.identity.user.authentication.SecurityTokenHelper;
import net.whydah.identity.user.identity.LDAPHelper;
import net.whydah.identity.user.identity.LdapAuthenticatorImpl;
import net.whydah.identity.user.search.Indexer;
import net.whydah.identity.user.search.Search;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;

import java.io.File;
import java.io.IOException;

public class UserIdentityBackendModule extends AbstractModule {
    @Override
    protected void configure() {
        BasicDataSource dataSource = getDataSource();
        QueryRunner queryRunner = new QueryRunner(dataSource);
        bind(QueryRunner.class).toInstance(queryRunner);

        bindLuceneServices();

        String tokenserviceUri = AppConfig.appConfig.getProperty("securitytokenservice");
        bind(SecurityTokenHelper.class).toInstance(new SecurityTokenHelper(tokenserviceUri));

        bindLdapServices();
    }


    private BasicDataSource getDataSource() {
        String jdbcdriver = AppConfig.appConfig.getProperty("roledb.jdbc.driver");
        String jdbcurl = AppConfig.appConfig.getProperty("roledb.jdbc.url");
        String roledbuser = AppConfig.appConfig.getProperty("roledb.jdbc.user");
        String roledbpasswd = AppConfig.appConfig.getProperty("roledb.jdbc.password");

        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(jdbcdriver);
        dataSource.setUrl(jdbcurl);//"jdbc:hsqldb:file:" + basepath + "hsqldb");
        dataSource.setUsername(roledbuser);
        dataSource.setPassword(roledbpasswd);
        return dataSource;
    }

    private void bindLuceneServices() {
        try {
            String luceneDir = AppConfig.appConfig.getProperty("lucene.directory");
            Directory index = new NIOFSDirectory(new File(luceneDir));
            bind(Directory.class).toInstance(index);
            bind(Indexer.class).toInstance(new Indexer(index));
            bind(Search.class).toInstance(new Search(index));
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }

    private void bindLdapServices() {
        String externalLdapUrl =  AppConfig.appConfig.getProperty("ldap.external.url");
        String externalAdmPrincipal =  AppConfig.appConfig.getProperty("ldap.external.principal");
        String externalAdmCredentials =  AppConfig.appConfig.getProperty("ldap.external.credentials");
        String externalUsernameAttribute =  AppConfig.appConfig.getProperty("ldap.external.usernameattribute");
        LdapAuthenticatorImpl externalLdapAuthenticator = new LdapAuthenticatorImpl(externalLdapUrl, externalAdmPrincipal, externalAdmCredentials, externalUsernameAttribute);
        bind(LdapAuthenticatorImpl.class).annotatedWith(Names.named("external")).toInstance(externalLdapAuthenticator);

        bind(LDAPHelper.class).toInstance(new LDAPHelper(externalLdapUrl, externalAdmPrincipal, externalAdmCredentials, externalUsernameAttribute));

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
        //int LDAP_PORT = new Integer(AppConfig.appConfig.getProperty("ldap.embedded.port"));
        // internalLdapUrl = "ldap://localhost:" + LDAP_PORT + "/dc=external,dc=WHYDAH,dc=no";
        LdapAuthenticatorImpl internalLdapAuthenticator = new LdapAuthenticatorImpl(internalLdapUrl, admPrincipal, admCredentials, usernameAttribute);
        bind(LdapAuthenticatorImpl.class).annotatedWith(Names.named("internal")).toInstance(internalLdapAuthenticator);
    }
}
