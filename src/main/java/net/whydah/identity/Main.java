package net.whydah.identity;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import net.whydah.identity.applicationtoken.ApplicationTokenService;
import net.whydah.identity.config.AppConfig;
import net.whydah.identity.config.ImportModule;
import net.whydah.identity.config.UserIdentityBackendModule;
import net.whydah.identity.dataimport.IamDataImporter;
import net.whydah.identity.security.SecurityFilter;
import net.whydah.identity.user.identity.EmbeddedADS;
import net.whydah.identity.usertoken.SecurityTokenHelper;
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

    public Main() {
        injector = Guice.createInjector(new UserIdentityBackendModule());
    }


    public static void main(String[] args) {
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
                log.error("Could not start embedded ApacheDS. Shutting down UserIdentityBackend.", e);
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
            log.error("Could not start HTTP Server. Shutting down UserIdentityBackend.", e);
            System.exit(2);
        }

        if (!embeddedDSEnabled) {
            try {
                // wait forever...
                Thread.currentThread().join();
            } catch (InterruptedException ie) {
                log.warn("Thread was interrupted.", ie);
            }
            main.stop();
        }
    }

    public void importUsersAndRoles() {
        Injector injector = Guice.createInjector(new ImportModule());
        
        IamDataImporter iamDataImporter = injector.getInstance(IamDataImporter.class);
        iamDataImporter.importIamData();
        
    }

    public static boolean shouldImportUsers() {
        String dburl = AppConfig.appConfig.getProperty("roledb.jdbc.url");
        String dbpath = dburl.substring(dburl.lastIndexOf(':') + 1) + ".script";
        File dbfile = new File(dbpath);
        boolean shouldImport = !dbfile.exists();    //TODO - When we have prod. and dev enviroment should be dbfile.exists()

        log.debug("dbpath=" + dbfile.getAbsolutePath() + ", exists=" + dbfile.exists() + ", shouldImport is set to " + shouldImport);
        return shouldImport;
    }


    public Injector getInjector() {
        return injector;
    }

    public void startHttpServer() throws Exception {
        log.trace("Starting UserIdentityBackend");

        ServletHandler servletHandler = new ServletHandler();
        servletHandler.setContextPath("/uib");
        servletHandler.addInitParameter("com.sun.jersey.config.property.packages",
                "net.whydah.identity.user.resource, net.whydah.identity.usertoken, net.whydah.identity.application.resource");
        servletHandler.addInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
        servletHandler.setProperty(ServletHandler.LOAD_ON_STARTUP, "1");

        GuiceFilter filter = new GuiceFilter();
        servletHandler.addFilter(filter, "guiceFilter", null);

        addSecurityFilterForUserAdmin(servletHandler);


        GuiceContainer container = new GuiceContainer(injector);
        servletHandler.setServletInstance(container);

        /*
        webappPort = Integer.valueOf(AppConfig.appConfig.getProperty("identity.port"));
        //URI baseUri = UriBuilder.fromUri("http://localhost").port(webappPort).build();
        URI baseUri =  new URI(AppConfig.appConfig.getProperty("myuri"));
        httpServer = GrizzlyServerFactory.createHttpServer(baseUri, servletHandler);
        log.info("UserIdentityBackend started with baseUri=", baseUri);
        */
        webappPort = Integer.valueOf(AppConfig.appConfig.getProperty("service.port"));
        httpServer = new HttpServer();
        ServerConfiguration serverconfig = httpServer.getServerConfiguration();
        serverconfig.addHttpHandler(servletHandler, "/");
        NetworkListener listener = new NetworkListener("grizzly", NetworkListener.DEFAULT_NETWORK_HOST, webappPort);
        httpServer.addListener(listener);
        httpServer.start();
        log.info("UserIdentityBackend started on port {}", webappPort);
    }


	private void addSecurityFilterForUserAdmin(ServletHandler servletHandler) {
		String requiredRoleName = AppConfig.appConfig.getProperty("useradmin.requiredrolename");
		if (StringUtils.isEmpty(requiredRoleName)) {
			log.warn("Required Role Name is empty! Verify the useradmin.requiredrolename-attribute in the configuration.");
		}
		SecurityFilter securityFilter = new SecurityFilter(injector.getInstance(SecurityTokenHelper.class), injector.getInstance(ApplicationTokenService.class));
        HashMap<String, String> initParams = new HashMap<>(1);
        initParams.put(SecurityFilter.SECURED_PATHS_PARAM, "/useradmin, /createandlogon");   //TODO verify
        initParams.put(SecurityFilter.REQUIRED_ROLE_PARAM, requiredRoleName);
        servletHandler.addFilter(securityFilter, "SecurityFilter", initParams);
	}

    public int getPort() {
        return webappPort;
    }

    public void startEmbeddedDS() throws Exception {
        log.info("Starting embedded ApacheDS");
        String ldappath = AppConfig.appConfig.getProperty("ldap.embedded.directory");
        ads = new EmbeddedADS(ldappath);

        int ldapport = Integer.valueOf(AppConfig.appConfig.getProperty("ldap.embedded.port"));
        ads.startServer(ldapport);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            log.error("Thread interrupted.", e);
        }
    }

    public void stop() {
        log.info("Stopping http server and embedded Apache DS.");    //TODO ED: What about hsqldb?
        if (httpServer != null) {
            httpServer.stop();
        }
        if (ads != null) {
            ads.stopServer();
        }
    }

}
