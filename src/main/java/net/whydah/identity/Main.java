package net.whydah.identity;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import net.whydah.identity.application.authentication.ApplicationTokenService;
import net.whydah.identity.config.AppConfig;
import net.whydah.identity.config.ImportModule;
import net.whydah.identity.config.SSLTool;
import net.whydah.identity.config.UserIdentityBackendModule;
import net.whydah.identity.dataimport.IamDataImporter;
import net.whydah.identity.security.SecurityFilter;
import net.whydah.identity.user.authentication.SecurityTokenServiceHelper;
import net.whydah.identity.user.identity.EmbeddedADS;
import net.whydah.identity.util.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.servlet.ServletHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private EmbeddedADS ads;
    private HttpServer httpServer;
    private int webappPort;
    private final Injector injector;
    private String contextpath = "/uib";
    public static String version;

    public Main() {
        version = this.getClass().getPackage().getImplementationVersion();
        injector = Guice.createInjector(new UserIdentityBackendModule());
    }


    public static void main(String[] args) {
        final Main main = new Main();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                log.debug("ShutdownHook triggered. Exiting application");
                main.stop();
            }
        });

        //TODO Ask Stig Lau. about this functionality. It is not finished/ doesn't work as expected.
        /*
        //TODO During startup, the app should check if it can do "count * from users" and get a number larger than 0, and locate more than one user from LDAP to ensure that both servers are up and schemas are working. If not, assume that the DB's are empty and need bootstrapping.
        boolean canAccessDBWithUserRoles = main.canAccessDBWithUserRoles();
        boolean canContactLDAP = true;

        //TODO remove the "PROD" hack when the previous TODO is fixed!
        boolean importUsers = !"PROD".equals(System.getProperty(AppConfig.IAM_MODE_KEY).toUpperCase()) && shouldImportUsers();
        */

        boolean importEnabled = Boolean.parseBoolean(AppConfig.appConfig.getProperty("import.enabled"));

        // Start ldap embedded server
        boolean embeddedDSEnabled = Boolean.parseBoolean(AppConfig.appConfig.getProperty("ldap.embedded"));
        if (embeddedDSEnabled) {
            String ldapPath = AppConfig.appConfig.getProperty("ldap.embedded.directory");
            if (importEnabled) {
                FileUtils.deleteDirectory(new File(ldapPath));
            }
            try {
                Integer ldapPort = Integer.valueOf(AppConfig.appConfig.getProperty("ldap.embedded.port"));
                main.startEmbeddedDS(ldapPath, ldapPort);
            } catch (Exception e) {
                log.error("Could not start embedded ApacheDS. Shutting down UserIdentityBackend.", e);
                System.exit(1);
            }
        }

        // Populate ldap, database and lucene index
        //if (!canAccessDBWithUserRoles || importTestData) {
        if (importEnabled) {
            try {
                main.deleteDirectoryByProperty("roledb.directory");
                main.deleteDirectoryByProperty("lucene.directory");
                main.importUsersAndRoles();
            } catch (RuntimeException e) {
                log.error("Import failed. Shutting down UserIdentityBackend.", e);
                System.exit(2);
            }
        }

        try {
            main.startHttpServer();
        } catch (Exception e) {
            log.error("Could not start HTTP Server. Shutting down UserIdentityBackend.", e);
            System.exit(3);
        }

        if (!embeddedDSEnabled) {
            try {
                // wait forever...
                Thread.currentThread().join();
            } catch (InterruptedException ie) {
                log.warn("Thread was interrupted.", ie);
            }
            log.debug("Finished waiting for Thread.currentThread().join()");
            main.stop();
        }
    }

    void deleteDirectoryByProperty(String key) {
        String dirPath = AppConfig.appConfig.getProperty(key);
        if (dirPath != null) {
            FileUtils.deleteDirectory(new File(dirPath));
        }
    }

    public void importUsersAndRoles() {
        Injector injector = Guice.createInjector(new ImportModule());

        IamDataImporter iamDataImporter = injector.getInstance(IamDataImporter.class);
        iamDataImporter.importIamData();

    }

    /*
    public boolean canAccessDBWithUserRoles() {
        try {
            return Guice.createInjector(new ImportModule()).getInstance(UserPropertyAndRoleRepository.class)
                    .countUserRolesInDB() > 0;
        }catch(Exception e) {
            log.error("Sanity check of DB failed. Possibly because DB was not set up or schema was erronous!", e);
            return false;
        }
    }

    public static boolean shouldImportUsers() {
        String dburl = AppConfig.appConfig.getProperty("roledb.jdbc.url");
        String dbpath = dburl.substring(dburl.lastIndexOf(':') + 1) + ".script";
        File dbfile = new File(dbpath);
        boolean shouldImport = !dbfile.exists();    //TODO - When we have prod. and dev enviroment should be dbfile.exists()

        log.debug("dbpath=" + dbfile.getAbsolutePath() + ", exists=" + dbfile.exists() + ", shouldImport is set to " + shouldImport);
        return shouldImport;
    }
    */


    public Injector getInjector() {
        return injector;
    }

    public void startHttpServer() throws Exception {
        log.trace("Starting UserIdentityBackend");


        // Property-overwrite of SSL verification to support weak ssl certificates
        if ("disabled".equalsIgnoreCase(AppConfig.appConfig.getProperty("sslverification"))) {
            SSLTool.disableCertificateValidation();

        }

        ServletHandler servletHandler = new ServletHandler();
        servletHandler.setContextPath(contextpath);
        servletHandler.addInitParameter("com.sun.jersey.config.property.packages",
                "net.whydah.identity.user.resource, net.whydah.identity.user.authentication, net.whydah.identity.application, net.whydah.identity.application.authentication, net.whydah.identity.health");
        servletHandler.addInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
        servletHandler.setProperty(ServletHandler.LOAD_ON_STARTUP, "1");

        GuiceFilter filter = new GuiceFilter();
        servletHandler.addFilter(filter, "guiceFilter", null);

        addSecurityFilterForUserAdmin(servletHandler);


        GuiceContainer container = new GuiceContainer(injector);
        servletHandler.setServletInstance(container);


        // TODO: use AppConfig.appConfig.getProperty("myuri") instead of hardcoded values
        webappPort = Integer.valueOf(AppConfig.appConfig.getProperty("service.port"));
        httpServer = new HttpServer();
        ServerConfiguration serverconfig = httpServer.getServerConfiguration();
        serverconfig.addHttpHandler(servletHandler, "/");
        NetworkListener listener = new NetworkListener("grizzly", NetworkListener.DEFAULT_NETWORK_HOST, webappPort);
        httpServer.addListener(listener);
        httpServer.start();
        log.info("UserIdentityBackend - import.enabled=" + Boolean.parseBoolean(AppConfig.appConfig.getProperty("import.enabled")));
        log.info("UserIdentityBackend - embeddedDSEnabled=" + Boolean.parseBoolean(AppConfig.appConfig.getProperty("ldap.embedded")));
        log.info("UserIdentityBackend version:{} started on port {}", version, webappPort + " context-path:" + contextpath);
    }


    private void addSecurityFilterForUserAdmin(ServletHandler servletHandler) {
        String requiredRoleName = AppConfig.appConfig.getProperty("useradmin.requiredrolename");
        if (StringUtils.isEmpty(requiredRoleName)) {
            log.warn("Required Role Name is empty! Verify the useradmin.requiredrolename-attribute in the configuration.");
        }
        SecurityFilter securityFilter = new SecurityFilter(injector.getInstance(SecurityTokenServiceHelper.class), injector.getInstance(ApplicationTokenService.class));
        HashMap<String, String> initParams = new HashMap<>(1);
        servletHandler.addFilter(securityFilter, "SecurityFilter", initParams);
        log.info("SecurityFilter initialized with params:", initParams);
    }

    public int getPort() {
        return webappPort;
    }

    public void startEmbeddedDS(String ldapPath, int ldapPort) throws Exception {
        log.info("Starting embedded ApacheDS");
        ads = new EmbeddedADS(ldapPath);

        ads.startServer(ldapPort);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            log.error("Thread interrupted.", e);
        }
    }

    public void stop() {
        log.info("Stopping http server");    //TODO ED: What about hsqldb?  It dies with the process..
        if (httpServer != null) {
            httpServer.stop();
        }
        if (ads != null) {
            log.info("Stopping embedded Apache DS.");
            ads.stopServer();
        }
    }

}
