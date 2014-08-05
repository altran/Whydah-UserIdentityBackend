package net.whydah.identity.config;

import com.google.inject.AbstractModule;
import net.whydah.identity.user.identity.LdapUserIdentityDao;
import net.whydah.identity.user.search.Indexer;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class ImportModule extends AbstractModule {
    private static final Logger log = LoggerFactory.getLogger(ImportModule.class);

    @Override
    protected void configure() {
        log.info("Configure ImportModule (primaryLDAP, roledb (sql), queryRunner and Lucene).");

        //Primary LDAP
        String primaryLdapUrl = AppConfig.appConfig.getProperty("ldap.primary.url");
        String primaryAdmPrincipal = AppConfig.appConfig.getProperty("ldap.primary.admin.principal");
        String primaryAdmCredentials = AppConfig.appConfig.getProperty("ldap.primary.admin.credentials");
        String primaryUsernameAttribute = AppConfig.appConfig.getProperty("ldap.primary.usernameattribute");
        bind(LdapUserIdentityDao.class).toInstance(new LdapUserIdentityDao(primaryLdapUrl, primaryAdmPrincipal, primaryAdmCredentials, primaryUsernameAttribute));


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
