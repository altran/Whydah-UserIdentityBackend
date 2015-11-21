package net.whydah.identity.dataimport;

import net.whydah.identity.application.ApplicationService;
import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;



public class ApplicationJsonImporter {
    private static final Logger log = LoggerFactory.getLogger(ApplicationJsonImporter.class);


    private ApplicationService applicationService;


    public ApplicationJsonImporter(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public void importApplications(InputStream applicationsSource) {
        log.info("Importing basis applications from file: {}", applicationsSource);
        BufferedReader reader = null;
        String applicationsJson = null;
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
        List<Application> applications = ApplicationMapper.fromJsonList(applicationsJson);
        saveApplications(applications);
        log.info("{} applications imported.", applications.size());
    }


    private void saveApplications(List<Application> applications) {
        for (Application application: applications) {
            try {
                applicationService.create(application.getId(), application);
                log.info("Imported Application. Id {}, Name {}", application.getId(), application.getName());
            } catch(Exception e) {
                log.error("Unable to persist application: {}", application.toString(), e);
                throw new RuntimeException("Unable to persist application: " + application.toString(), e);
            }
        }
    }
}
