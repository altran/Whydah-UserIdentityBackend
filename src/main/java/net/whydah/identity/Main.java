package net.whydah.identity;

import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import net.whydah.identity.dataimport.IamDataImporter;
import net.whydah.identity.ldapserver.EmbeddedADS;
import net.whydah.identity.user.authentication.SecurityTokenServiceClient;
import net.whydah.identity.util.FileUtils;
import net.whydah.sso.util.SSLTool;
import org.apache.commons.dbcp.BasicDataSource;
import org.constretto.ConstrettoBuilder;
import org.constretto.ConstrettoConfiguration;
import org.constretto.model.Resource;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;

public class Main {
    public static final String CONTEXT_PATH = "/uib";
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private EmbeddedADS ads;
    //private HttpServer httpServer;
    private int webappPort;
    private Server server;
    private String resourceBase;

    int maxThreads = 100;
    int minThreads = 10;
    int idleTimeout = 120;


    public Main(Integer webappPort) {
        this.webappPort = webappPort;
        //log.info("Starting Jetty on port {}", webappPort);
        QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads, idleTimeout);

        this.server = new Server(threadPool);
        ServerConnector connector = new ServerConnector(this.server);
        connector.setPort(webappPort);
        this.server.setConnectors(new Connector[]{connector});

        URL url = ClassLoader.getSystemResource("WEB-INF/web.xml");
        this.resourceBase = url.toExternalForm().replace("WEB-INF/web.xml", "");
    }


    // 1a. Default:        External ldap and database
    // or
    // 1b. Test scenario:  startJetty embedded Ldap and database

    // 2. run db migrations (should not share any objects with the web application)

    // 3. possibly import (should not share any objects with the web application)

    // 4. startJetty webserver
    public static void main(String[] args) {
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        LogManager.getLogManager().getLogger("").setLevel(Level.INFO);

        final ConstrettoConfiguration configuration = new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
                .addResource(Resource.create("file:./useridentitybackend_override.properties"))
                .done()
                .getConfiguration();

        printConfiguration(configuration);

        String version = Main.class.getPackage().getImplementationVersion();

        boolean importEnabled = configuration.evaluateToBoolean("import.enabled");
        boolean embeddedDSEnabled = configuration.evaluateToBoolean("ldap.embedded");
        log.info("Starting UserIdentityBackend version={}, import.enabled={}, embeddedDSEnabled={}", version, importEnabled, embeddedDSEnabled);
        try {
            Integer webappPort = configuration.evaluateToInt("service.port");
            final Main main = new Main(webappPort);
            startWhydahClient();


            String ldapEmbeddedpath = configuration.evaluateToString("ldap.embedded.directory");
            String roleDBDirectory = configuration.evaluateToString("roledb.directory");
            String luceneUsersDirectory = configuration.evaluateToString("lucene.usersdirectory");
            String luceneApplicationDirectory = configuration.evaluateToString("lucene.applicationsdirectory");

            if (importEnabled) {
                FileUtils.deleteDirectories(roleDBDirectory, luceneUsersDirectory, luceneApplicationDirectory);
            }
            FileUtils.createDirectory(luceneUsersDirectory);
            FileUtils.createDirectory(luceneApplicationDirectory);

            try {
                if (embeddedDSEnabled) {
                    if (importEnabled) {
                        FileUtils.deleteDirectories(ldapEmbeddedpath);
                    }
                    main.startEmbeddedDS(configuration.asMap());
                }
            } catch (Exception e) {
                log.error("Unable to startJetty Embedded LDAP chema", e);
            }

            try {
                BasicDataSource dataSource = initBasicDataSource(configuration);
                new DatabaseMigrationHelper(dataSource).upgradeDatabase();

                if (importEnabled) {
                    // Populate ldap, database and lucene index
                    new IamDataImporter(dataSource, configuration).importIamData();
                }
            } catch (Exception e) {
                log.error("Unable to upgrade DB chema", e);
            }


            // Property-overwrite of SSL verification to support weak ssl certificates
            String sslVerification = configuration.evaluateToString("sslverification");
            if ("disabled".equalsIgnoreCase(sslVerification)) {
                SSLTool.disableCertificateValidation();
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

            //main.startHttpServer(requiredRoleName);
            main.startJetty();
            main.joinJetty();
            log.info("UserIdentityBackend version:{} started on port {}. ", version, webappPort + " context-path:" + CONTEXT_PATH);
            log.info("Health: http://localhost:{}/{}/{}/", webappPort, CONTEXT_PATH, "health");

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    log.debug("ShutdownHook triggered. Exiting application");
                    main.stop();
                }
            });

        } catch (RuntimeException e) {
            log.error("Error during startup. Shutting down UserIdentityBackend.", e);
            System.exit(1);
        }
    }

    public static Map<Object, Object> subProperties(ConstrettoConfiguration configuration, String prefix) {
        final Map<Object, Object> ldapProperties = new HashMap<>();
        configuration.forEach(property -> {
            if (property.getKey().startsWith(prefix)) {
                ldapProperties.put(property.getKey(), property.getValue());
            }
        });
        return ldapProperties;
    }

    private static BasicDataSource initBasicDataSource(ConstrettoConfiguration configuration) {
        String jdbcdriver = configuration.evaluateToString("roledb.jdbc.driver");
        String jdbcurl = configuration.evaluateToString("roledb.jdbc.url");
        String roledbuser = configuration.evaluateToString("roledb.jdbc.user");
        String roledbpasswd = configuration.evaluateToString("roledb.jdbc.password");

        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(jdbcdriver);
        dataSource.setUrl(jdbcurl);
        dataSource.setUsername(roledbuser);
        dataSource.setPassword(roledbpasswd);
        return dataSource;
    }

    public void startJetty() {

        WebAppContext webAppContext = new WebAppContext();
        log.debug("Start Jetty using resourcebase={}", resourceBase);
        webAppContext.setDescriptor(resourceBase + "/WEB-INF/web.xml");
        webAppContext.setResourceBase(resourceBase);
        webAppContext.setContextPath(CONTEXT_PATH);
        webAppContext.setParentLoaderPriority(true);

        HandlerList handlers = new HandlerList();
        Handler[] handlerList = {webAppContext, new DefaultHandler()};
        handlers.setHandlers(handlerList);
        server.setHandler(handlers);


        try {
            server.start();
        } catch (Exception e) {
            log.error("Error during Jetty startup. Exiting", e);
            System.exit(2);
        }
        int localPort = getPort();
        log.info("Jetty server started on http://localhost:{}{}", localPort, CONTEXT_PATH);
    }


    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            log.warn("Error when stopping Jetty server", e);
        }

        if (ads != null) {
            log.info("Stopping embedded Apache DS.");
            try {
                ads.stop();
            } catch (Exception e) {
                runtimeException(e);
            }
        }
    }

    public void joinJetty() {
        try {
            server.join();
        } catch (InterruptedException e) {
            log.error("Jetty server thread when joinJetty. Pretend everything is OK.", e);
        }
    }

    public static void startWhydahClient() {
        SecurityTokenServiceClient.getWAS();
    }

    public void startEmbeddedDS(Map<String, String> properties) {
        ads = new EmbeddedADS(properties);
        try {
            ads.init();
            ads.start();
        } catch (Exception e) {
            runtimeException(e);
        }
    }

    private void runtimeException(Exception e) {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        throw new RuntimeException(e);
    }


    public int getPort() {
        return webappPort;
        //        return ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    }

    private static void printConfiguration(ConstrettoConfiguration configuration) {
        Map<String, String> properties = configuration.asMap();
        for (String key : properties.keySet()) {
            log.info("Using Property: {}, value: {}", key, properties.get(key));
        }
    }
}