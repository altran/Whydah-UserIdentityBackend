package net.whydah.identity.user.identity;

import net.whydah.identity.config.WhydahConfig;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.io.File;
import java.io.IOException;
import java.util.UUID;


public class EmbeddedADS {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedADS.class);
    private static final int DEFAULT_SERVER_PORT = 10389;
    private static final String INSTANCE_NAME = "Whydah";
    private static final String BASE_DN = "o=TEST";
    /**
     * The directory identity
     */
    private DirectoryService service;

    /**
     * The LDAP server
     */
    private LdapServer server;
    private String dc = "";
    // private FCconfig fCconfig;


    /**
     * initialize the schema manager and add the schema partition to diectory identity
     *
     * @throws Exception if the schema LDIF files are not found on the classpath
     */
    /**
     * private void initSchemaPartition() throws Exception {
     * SchemaPartition schemaPartition = service.getSchemaPartition();
     * <p/>
     * // Init the LdifPartition
     * LdifPartition ldifPartition = new LdifPartition();
     * String workingDirectory = service.getWorkingDirectory().getPath();
     * ldifPartition.setWorkingDirectory(workingDirectory + "/schema");
     * <p/>
     * // Extract the schema on disk (a brand new one) and load the registries
     * File schemaRepository = new File(workingDirectory, "schema");
     * SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor(new File(workingDirectory));
     * extractor.extractOrCopy(true);
     * <p/>
     * logger.trace("Extacted Schema");
     * schemaPartition.setWrappedPartition(ldifPartition);
     * <p/>
     * SchemaLoader loader = new LdifSchemaLoader(schemaRepository);
     * SchemaManager schemaManager = new DefaultSchemaManager(loader);
     * service.setSchemaManager(schemaManager);
     * <p/>
     * // We have to load the schema now, otherwise we won't be able
     * // to initialize the Partitions, as we won't be able to parse
     * // and normalize their suffix DN
     * schemaManager.loadAllEnabled();
     * <p/>
     * schemaPartition.setSchemaManager(schemaManager);
     * <p/>
     * List<Throwable> errors = schemaManager.getErrors();
     * <p/>
     * if (!errors.isEmpty()) {
     * throw new Exception("Schema load failed : " + errors);
     * }
     * }
     */

    private void init(String INSTANCE_PATH) throws Exception, IOException, LdapException,
            NamingException {

        DefaultDirectoryServiceFactory factory = new DefaultDirectoryServiceFactory();
        factory.init(INSTANCE_NAME);

        service = factory.getDirectoryService();
        service.getChangeLog().setEnabled(false);
        service.setShutdownHookEnabled(true);

        InstanceLayout il = new InstanceLayout(INSTANCE_PATH);
        service.setInstanceLayout(il);


        AvlPartition partition = new AvlPartition(
                service.getSchemaManager());
        partition.setId("Test");
        partition.setSuffixDn(new Dn(service.getSchemaManager(),
                BASE_DN));
        logger.trace("Initializing partition {} instance {}", BASE_DN, "Test");
        partition.initialize();
        service.addPartition(partition);

        AvlPartition mypartition = new AvlPartition(
                service.getSchemaManager());
        mypartition.setId(INSTANCE_NAME);
        mypartition.setSuffixDn(new Dn(service.getSchemaManager(),
                "dc=external,dc=" + dc + ",dc=no"));
        logger.trace("Initializing LDAP partition {} instance {}", "dc=external,dc=" + dc + ",dc=no", INSTANCE_NAME);
        mypartition.initialize();
        service.addPartition(mypartition);

        //         Partition apachePartition = addPartition(dc, "dc=external,dc="+dc+",dc=no");

        Dn dnApache = new Dn("dc=external,dc=" + dc + ",dc=no");
        Entry entryApache = service.newEntry(dnApache);
        entryApache.add("objectClass", "top", "domain", "extensibleObject");
        entryApache.add("dc", dc);
        service.getAdminSession().add(entryApache);
        Entry entryApache2 = service.newEntry(new Dn("ou=users,dc=external,dc=" + dc + ",dc=no"));
        entryApache2.add("objectClass", "top", "organizationalUnit");
        service.getAdminSession().add(entryApache2);

        //server = new LdapServer();
        //server.setTransports(new TcpTransport("localhost", DEFAULT_SERVER_PORT));
        //server.setDirectoryService(service);
    }

    /**
     * Initialize the server. It creates the partition, adds the index, and
     * injects the context entries for the created partitions.
     *
     * @param workDir the directory to be used for storing the data
     * @throws Exception if there were some problems while initializing the system
     */
    private void initDirectoryService(File workDir) throws Exception {
        if (!workDir.exists()) {
            boolean dirsCreated = workDir.mkdirs();
            if (!dirsCreated) {
                logger.debug("Not all directories could be created. " + workDir.getAbsolutePath());
            }
        }


        WhydahConfig myConfig = new WhydahConfig();
        if (myConfig.getProptype().equals("DEV")) {
            dc = "WHYDAH";
        } else {
            dc = "WHYDAH";
        }

        init(workDir.getPath());


        // And start the identity
        // service.startup();

    }


    /**
     * Creates a new instance of EmbeddedADS. It initializes the directory identity.
     *
     * @throws Exception If something went wrong
     */
    public EmbeddedADS(File workDir) throws Exception {
        initDirectoryService(workDir);
    }

    /**
     * Creates a new instance of EmbeddedADS. It initializes the directory identity.
     *
     * @throws Exception If something went wrong
     */
    public EmbeddedADS(String workDir) throws Exception {
        this(new File(workDir));
    }


    public void startServer() throws Exception {
        startServer(DEFAULT_SERVER_PORT);
    }

    /**
     * starts the LdapServer
     *
     * @throws Exception
     */
    public void startServer(int serverPort) throws Exception {
        server = new LdapServer();
        server.setTransports(new TcpTransport(serverPort));
        server.setDirectoryService(service);
        server.start();
        logger.info("Apache DS Started, port: {}", serverPort);
    }

    public void stopServer() {
        server.stop();
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {

        }
    }


    /**
     * Main class for standalone test and configuration of ApacheDS embedded.
     *
     * @param args first arg is working directory. Default is used if not specified.
     */
    public static void main(String[] args) {
        try {
            String ldappath;
            if (args.length == 0) {
                ldappath = System.getProperty("user.home") + "/bootstrapdata/ldaptest" + UUID.randomUUID().toString();
            } else {
                ldappath = args[0];
            }

            logger.info("LDAP working directory={}", ldappath);
            File workDir = new File(ldappath);

            boolean isDirCreated = workDir.mkdirs();
            if (isDirCreated) {
                // Create the server
                EmbeddedADS ads = new EmbeddedADS(workDir);
                ads.init(workDir.getPath());
                // optionally we can start a server too
                ads.startServer(DEFAULT_SERVER_PORT);


                // Read an entry
                Entry result = ads.service.getAdminSession().lookup(new Dn("dc=external,dc=WHYDAH,dc=no"));

                // And print it if available
                System.out.println("Found entry : " + result);
            }


        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        }
    }
}
