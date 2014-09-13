package net.whydah.identity.user.identity;

import net.whydah.identity.config.WhydahConfig;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.core.schema.SchemaPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.registries.SchemaLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashSet;
import java.util.List;


/**
 * A simple example exposing how to embed Apache Directory Server version 1.5.7
 * into an application.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class EmbeddedADS {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedADS.class);
    private static final int DEFAULT_SERVER_PORT = 10389;

    /**
     * The directory identity
     */
    private DirectoryService service;

    /**
     * The LDAP server
     */
    private LdapServer server;
    private String dc="";
    // private FCconfig fCconfig;


    /**
     * Add a new partition to the server
     *
     * @param partitionId The partition Id
     * @param partitionDn The partition DN
     * @return The newly added partition
     * @throws Exception If the partition can't be added
     */
    private Partition addPartition(String partitionId, String partitionDn) throws Exception {
        // Create a new partition named 'foo'.
        JdbmPartition partition = new JdbmPartition();
        partition.setId(partitionId);
        partition.setPartitionDir(new File(service.getWorkingDirectory(), partitionId));
        partition.setSuffix(partitionDn);
        service.addPartition(partition);

        return partition;
    }


    /**
     * Add a new set of index on the given attributes
     *
     * @param partition The partition on which we want to add index
     * @param attrs     The list of attributes to index
     */
    private void addIndex(Partition partition, String... attrs) {
        // Index some attributes on the apache partition
        HashSet<Index<?, ServerEntry, Long>> indexedAttributes = new HashSet<Index<?, ServerEntry, Long>>();

        for (String attribute : attrs) {
            indexedAttributes.add(new JdbmIndex<String, ServerEntry>(attribute));
        }

        ((JdbmPartition) partition).setIndexedAttributes(indexedAttributes);
    }


    /**
     * initialize the schema manager and add the schema partition to diectory identity
     *
     * @throws Exception if the schema LDIF files are not found on the classpath
     */
    private void initSchemaPartition() throws Exception {
        SchemaPartition schemaPartition = service.getSchemaService().getSchemaPartition();

        // Init the LdifPartition
        LdifPartition ldifPartition = new LdifPartition();
        String workingDirectory = service.getWorkingDirectory().getPath();
        ldifPartition.setWorkingDirectory(workingDirectory + "/schema");

        // Extract the schema on disk (a brand new one) and load the registries
        File schemaRepository = new File(workingDirectory, "schema");
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor(new File(workingDirectory));
        extractor.extractOrCopy(true);

        logger.trace("Extacted Schema");
        schemaPartition.setWrappedPartition(ldifPartition);

        SchemaLoader loader = new LdifSchemaLoader(schemaRepository);
        SchemaManager schemaManager = new DefaultSchemaManager(loader);
        service.setSchemaManager(schemaManager);

        // We have to load the schema now, otherwise we won't be able
        // to initialize the Partitions, as we won't be able to parse
        // and normalize their suffix DN
        schemaManager.loadAllEnabled();

        schemaPartition.setSchemaManager(schemaManager);

        List<Throwable> errors = schemaManager.getErrors();

        if (!errors.isEmpty()) {
            throw new Exception("Schema load failed : " + errors);
        }
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

        // Initialize the LDAP identity
        service = new DefaultDirectoryService();
        service.setWorkingDirectory(workDir);

        WhydahConfig fCconfig = new WhydahConfig();
        if (fCconfig.getProptype().equals("FCDEV")) {
            //dc = fCconfig.getFCDc(); //TODO commented for a while. Work with it when Prod and dev. enviroment are ready
            dc="WHYDAH";
        } else if(fCconfig.getProptype().equals("DEV")) {
            //dc = "CUSTOMER"; //TODO commented for a while. Work with it when Prod and dev. enviroment are ready
            dc="WHYDAH";
        } else{
            dc="WHYDAH"; //Test
        }


        // first load the schema
        initSchemaPartition();

        // then the system partition
        // this is a MANDATORY partition
        Partition systemPartition = addPartition("system", ServerDNConstants.SYSTEM_DN);
        service.setSystemPartition(systemPartition);

        // Disable the ChangeLog system
        service.getChangeLog().setEnabled(false);
        service.setDenormalizeOpAttrsEnabled(true);

        // Now we can create as many partitions as we need
        // Create some new partitions named 'foo', 'bar' and 'apache'.

        Partition apachePartition = addPartition(dc, "dc=external,dc="+dc+",dc=no");
        // Index some attributes on the apache partition
        addIndex(apachePartition, "objectClass", "ou", "uid");

        // And start the identity
        service.startup();

        // Inject the apache root entry
        if (service.getAdminSession().exists(apachePartition.getSuffixDn())) {
            return;
        }

        DN dnApache = new DN("dc=external,dc="+dc+",dc=no");
        ServerEntry entryApache = service.newEntry(dnApache);
        entryApache.add("objectClass", "top", "domain", "extensibleObject");
        entryApache.add("dc", dc); 
        service.getAdminSession().add(entryApache);
        ServerEntry entryApache2 = service.newEntry(new DN("ou=users,dc=external,dc="+dc+",dc=no")); 
        entryApache2.add("objectClass", "top", "organizationalUnit");
        service.getAdminSession().add(entryApache2);
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
    }


    /**
     * Main class.
     *
     * @param args first arg is working directory. Default is used if not specified.
     */
    public static void main(String[] args) {
        try {
            String ldappath;
            if (args.length == 0) {
                ldappath = System.getProperty("user.home") + "/bootstrapdata/ldap";
            } else {
                ldappath = args[0];
            }

            logger.info("LDAP working directory={}", ldappath);
            File workDir = new File(ldappath);

            boolean isDirCreated =  workDir.mkdirs();
            if(isDirCreated){
                // Create the server
                EmbeddedADS ads = new EmbeddedADS(workDir);
                // optionally we can start a server too
                ads.startServer(DEFAULT_SERVER_PORT);
            }


            // Read an entry
//            Entry result = ads.identity.getAdminSession().lookup(new DN("dc=external,dc=WHYDAH,dc=no"));

            // And print it if available
//            System.out.println("Found entry : " + result);


        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        }
    }
}
