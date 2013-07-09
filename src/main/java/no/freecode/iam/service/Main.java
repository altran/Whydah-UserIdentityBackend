package no.freecode.iam.service;


import java.io.File;
import java.util.HashMap;

import no.freecode.iam.service.config.AppConfig;
import no.freecode.iam.service.config.ImportModule;
import no.freecode.iam.service.config.UserIdentityBackendModule;
import no.freecode.iam.service.dataimport.IamDataImporter;
import no.freecode.iam.service.helper.FileUtils;
import no.freecode.iam.service.ldap.EmbeddedADS;
import no.freecode.iam.service.security.SecurityFilter;
import no.freecode.iam.service.security.SecurityTokenHelper;

import org.apache.commons.lang.StringUtils;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.servlet.ServletHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private EmbeddedADS ads;
    private HttpServer httpServer;
    private int webappPort;
    private final Injector injector;

    public Main() {
        injector = Guice.createInjector(new UserIdentityBackendModule());
    }


    public static void main(String[] args) {
        //ED: Not in use as far as I can see.
        /*
        String propName = AppConfig.appConfig.getProperty("prop.type");
        logger.info("Prop name is " + propName);
        FCconfig fCconfig = new FCconfig();
        fCconfig.setPropName(propName);
        */


        final Main main = new Main();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                main.stop();
            }
        });

        boolean importUsers = shouldImportUsers();


        // Start ldap embedded server
        String startEmbeddedDS = AppConfig.appConfig.getProperty("ldap.embedded");
        boolean embeddedDSEnabled = "enabled".equals(startEmbeddedDS);
        if (embeddedDSEnabled) {
            if (importUsers) {
                FileUtils.deleteDirectory(new File(AppConfig.appConfig.getProperty("ldap.embedded.directory")));
            }
            try {
                main.startEmbeddedDS();
            } catch (Exception e) {
                logger.error("Could not start embedded ApacheDS. Shutting down UserIdentityBackend.", e);
                System.exit(1);
            }
        }


        // Populate ldap, database and lucene index
        if (importUsers) {
            FileUtils.deleteDirectory(new File(AppConfig.appConfig.getProperty("roledb.directory")));
            FileUtils.deleteDirectory(new File(AppConfig.appConfig.getProperty("lucene.directory")));
            main.importUsersAndRoles();
        }

        try {
            main.startHttpServer();
        } catch (Exception e) {
            logger.error("Could not start HTTP Server. Shutting down UserIdentityBackend.", e);
            System.exit(2);
        }

        if (!embeddedDSEnabled) {
            try {
                // wait forever...
                Thread.currentThread().join();
            } catch (InterruptedException ie) {
                logger.warn("Thread was interrupted.", ie);
            }
            main.stop();
        }
    }

    public void importUsersAndRoles() {
        
        Injector injector = Guice.createInjector(new ImportModule());
        
        IamDataImporter iamDataImporter = injector.getInstance(IamDataImporter.class);
        iamDataImporter.importIamData();
        
//        databaseHelper.initDB();
//        
//        ApplicationImporter applicationImporter = injector.getInstance(ApplicationImporter.class);
//        applicationImporter.importApplications(applicationsImportSource);
//        
//        OrganizationImporter organizationImporter = injector.getInstance(OrganizationImporter.class);
//        organizationImporter.importOrganizations(organizationsImportSource);
//        
//        WhydahUserIdentityImporter userImporter = injector.getInstance(WhydahUserIdentityImporter.class);
//        userImporter.importUsers(userImportSource);
//        
//        RoleMappingImporter roleMappingImporter = injector.getInstance(RoleMappingImporter.class);
//        roleMappingImporter.importRoleMapping(roleMappingImportSource);
    }

    public static boolean shouldImportUsers() {
        String dburl = AppConfig.appConfig.getProperty("roledb.jdbc.url");
        String dbpath = dburl.substring(dburl.lastIndexOf(':') + 1) + ".script";
        File dbfile = new File(dbpath);
        boolean shouldImport = !dbfile.exists();    //TODO - When we have prod. and dev enviroment should be dbfile.exists()

        logger.debug("dbpath=" + dbfile.getAbsolutePath() + ", exists=" + dbfile.exists() + ", shouldImport is set to " + shouldImport);
        return shouldImport;
    }


    public Injector getInjector() {
        return injector;
    }

    public void startHttpServer() throws Exception {
        logger.trace("Starting UserIdentityBackend");

        ServletHandler servletHandler = new ServletHandler();
        servletHandler.setContextPath("/uib");
        servletHandler.addInitParameter("com.sun.jersey.config.property.packages", "no.freecode.iam.service.resource,no.freecode.iam.service.view");
        servletHandler.addInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
        servletHandler.setProperty(ServletHandler.LOAD_ON_STARTUP, "1");

        GuiceFilter filter = new GuiceFilter();
        servletHandler.addFilter(filter, "guiceFilter", null);

        addSecurityFilterForUserAdmin(servletHandler);


        GuiceContainer container = new GuiceContainer(injector);
        servletHandler.setServletInstance(container);

        /*
        webappPort = Integer.valueOf(AppConfig.appConfig.getProperty("service.port"));
        //URI baseUri = UriBuilder.fromUri("http://localhost").port(webappPort).build();
        URI baseUri =  new URI(AppConfig.appConfig.getProperty("myuri"));
        httpServer = GrizzlyServerFactory.createHttpServer(baseUri, servletHandler);
        logger.info("UserIdentityBackend started with baseUri=", baseUri);
        */
        webappPort = Integer.valueOf(AppConfig.appConfig.getProperty("service.port"));
        httpServer = new HttpServer();
        ServerConfiguration serverconfig = httpServer.getServerConfiguration();
        serverconfig.addHttpHandler(servletHandler, "/");
        NetworkListener listener = new NetworkListener("grizzly", NetworkListener.DEFAULT_NETWORK_HOST, webappPort);
        httpServer.addListener(listener);
        httpServer.start();
        logger.info("UserIdentityBackend started on port {}", webappPort);
    }


	private void addSecurityFilterForUserAdmin(ServletHandler servletHandler) {
		String requiredRoleName = AppConfig.appConfig.getProperty("requiredrolename");
		if (StringUtils.isEmpty(requiredRoleName)) {
			logger.warn("Required Role Name is empty! Verify the requiredrolename-attribute in the configuration.");
		}
		SecurityFilter securityFilter = new SecurityFilter(injector.getInstance(SecurityTokenHelper.class));
        HashMap<String, String> initParams = new HashMap<>(1);
        initParams.put(SecurityFilter.SECURED_PATHS_PARAM, "/useradmin, /createandlogon");   //TODO verify
        initParams.put(SecurityFilter.REQUIRED_ROLE_PARAM, requiredRoleName);
        servletHandler.addFilter(securityFilter, "SecurityFilter", initParams);
	}

    public int getPort() {
        return webappPort;
    }

    public void startEmbeddedDS() throws Exception {
        logger.info("Starting embedded ApacheDS");
        String ldappath = AppConfig.appConfig.getProperty("ldap.embedded.directory");
        ads = new EmbeddedADS(ldappath);

        int ldapport = Integer.valueOf(AppConfig.appConfig.getProperty("ldap.embedded.port"));
        ads.startServer(ldapport);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            logger.error("Thread interrupted.", e);
        }
    }

    public void stop() {
        logger.info("Stopping http server and embedded Apache DS.");    //TODO ED: What about hsqldb?
        if (httpServer != null) {
            httpServer.stop();
        }
        if (ads != null) {
            ads.stopServer();
        }
    }

}
