package net.whydah.identity.config;

import com.google.inject.AbstractModule;
import net.whydah.identity.exceptions.ConfigurationException;
import net.whydah.identity.ldap.LDAPHelper;
import net.whydah.identity.search.Indexer;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class ImportModule extends AbstractModule {
	 private static final Logger logger = LoggerFactory.getLogger(ImportModule.class);
    @Override
    protected void configure() {
    	logger.info("configure");
        //LDAP
        String externalLdapUrl = AppConfig.appConfig.getProperty("ldap.external.url");
        String externalAdmPrincipal = AppConfig.appConfig.getProperty("ldap.external.principal");
        String externalAdmCredentials = AppConfig.appConfig.getProperty("ldap.external.credentials");
        String externalUsernameAttribute = AppConfig.appConfig.getProperty("ldap.external.usernameattribute");
        bind(LDAPHelper.class)
                .toInstance(new LDAPHelper(externalLdapUrl, externalAdmPrincipal, externalAdmCredentials, externalUsernameAttribute));

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

        //Lucene
        try {
            String luceneDir = AppConfig.appConfig.getProperty("lucene.directory");
            Directory index = new NIOFSDirectory(new File(luceneDir));
            bind(Directory.class).toInstance(index);
            bind(Indexer.class).toInstance(new Indexer(index));
        } catch (IOException e) {
            throw new ConfigurationException(e.getLocalizedMessage(), e);
           
        }
    }

}
