package net.whydah.identity.dataimport;

import net.whydah.identity.application.ApplicationService;
import net.whydah.identity.application.search.LuceneApplicationIndexer;
import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;
import org.apache.lucene.store.NIOFSDirectory;
import org.constretto.ConstrettoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;



public class ApplicationJsonImporter {
    private static final Logger log = LoggerFactory.getLogger(ApplicationJsonImporter.class);


    private ApplicationService applicationService;
    private final String luceneApplicationDir;



    public ApplicationJsonImporter(ApplicationService applicationService, ConstrettoConfiguration configuration) throws IOException {
        this.applicationService = applicationService;
        this.luceneApplicationDir = configuration.evaluateToString("lucene.applicationsdirectory");
        NIOFSDirectory index = createDirectory(luceneApplicationDir);
        LuceneApplicationIndexer luceneApplicationIndexer = new LuceneApplicationIndexer(index);

    }

    public void importApplications(InputStream applicationsSource) {
        log.info("Importing basis applications from file: {}", applicationsSource);
        BufferedReader reader = null;
        String applicationsJson = "";
        try {
            reader = new BufferedReader(new InputStreamReader(applicationsSource, IamDataImporter.CHARSET_NAME));
            String line=null;
            while (null != (line = reader.readLine())) {
                applicationsJson = applicationsJson + line;
            }
        } catch (Exception ioe) {
            log.error("Unable to read file {}", applicationsSource);
            throw new RuntimeException("Unable to import Application from file: " + applicationsSource);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.warn("Error closing stream", e);
                }
            }
        }
        log.info("Importing applications: {}", applicationsJson);
        List<Application> applications = ApplicationMapper.fromJsonList(applicationsJson);
        if (applications.size()>0){
            saveApplications(applications);
        } else {
            log.warn("Empty applications list detected, no import.");
        }
        log.info("{} applications imported.", applications.size());
    }


    private void saveApplications(List<Application> applications) {
        for (Application application: applications) {
            try {
                applicationService.create(application.getId(), application);
                //luceneApplicationIndexer.addToIndex(application);
                log.info("Imported Application. Id {}, Name {}", application.getId(), application.getName());
            } catch(Exception e) {
                log.error("Unable to persist application: {}", application.toString(), e);
                throw new RuntimeException("Unable to persist application: " + application.toString(), e);
            }
        }
    }
    private NIOFSDirectory createDirectory(String luceneDir) {
        try {
            File luceneDirectory = new File(luceneDir);
            if (!luceneDirectory.exists()) {
                boolean dirsCreated = luceneDirectory.mkdirs();
                if (!dirsCreated) {
                    log.debug("{} was not successfully created.", luceneDirectory.getAbsolutePath());
                }
            }
            return new NIOFSDirectory(luceneDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
