package net.whydah.identity.dataimport;

import com.google.inject.Inject;
import org.apache.commons.dbutils.QueryRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ApplicationImporter {
	private static final Logger log = LoggerFactory.getLogger(ApplicationImporter.class);
	
	private static final int REQUIRED_NUMBER_OF_FIELDS = 4;
	private static final int APPLICATIONID = 0;
	private static final int APPLICATIONNAME = 1;
	private static final int DEFAULTROLE = 2;
	private static final int DEFAULTORGANIZATIONID = 3;
	
	private QueryRunner queryRunner;
	
	@Inject
	public ApplicationImporter(QueryRunner queryRunner) {
		this.queryRunner = queryRunner;
	}
	
	public void importApplications(String applicationsSource) {
        if (applicationsSource == null || applicationsSource.isEmpty()) {
            log.info("applicationsSource was empty, skipping applications import.");
            return;
        }

        log.info("importApplications from applicationsSource={}", applicationsSource);
		List<Application> applications = parseApplications(applicationsSource);
		saveApplications(applications);
        log.info("{} applications imported.", applications.size());
	}

	private void saveApplications(List<Application> applications) {
		try {
			for (Application application: applications) {
				queryRunner.update("INSERT INTO Applications values (?, ?, ?, ?)", 
									application.getId(), application.getName(), application.getDefaultRoleName(), application.getDefaultOrganizationId());	
			}
		} catch(Exception e) {
			log.error("Unable to persist applications.", e);
			throw new RuntimeException("Unable to persist applications.", e);
		}
	}
	
	protected static List<Application> parseApplications(String applicationsSource) {
		BufferedReader reader = null;
		try {
			List<Application> applications = new ArrayList<>();
	        InputStream classpathStream = ApplicationImporter.class.getClassLoader().getResourceAsStream(applicationsSource);
	        reader = new BufferedReader(new InputStreamReader(classpathStream, "ISO-8859-1"));
	        String line = null; 
	        while (null != (line = reader.readLine())) {
	        	boolean isComment = line.startsWith("#");
				if (isComment) {
	        		continue;
	        	}
				
	        	String[] lineArray = line.split(",");
	        	validateLine(line, lineArray);
	        	
	        	String applicatinId = cleanString(lineArray[APPLICATIONID]);
	        	String applicationName = cleanString(lineArray[APPLICATIONNAME]);
	        	String defaultRoleName = cleanString(lineArray[DEFAULTROLE]);
	        	String defaultOrganizationId = cleanString(lineArray[DEFAULTORGANIZATIONID]);
	        	
	        	Application application = new Application(applicatinId, applicationName, defaultRoleName, defaultOrganizationId);
	            applications.add(application);
	        }
			return applications;
		
		} catch (IOException ioe) {
			log.error("Unable to read file {}", applicationsSource);
			throw new RuntimeException("Unable to import Application from file: " + applicationsSource);
		} finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.warn("Error closing stream", e);
                }
            }
        }
	}
	private static String cleanString(String string) {
		return string==null ? string : string.trim();
	}

	private static void validateLine(String line, String[] lineArray) {
		if (lineArray.length < REQUIRED_NUMBER_OF_FIELDS) {
			throw new RuntimeException("Applications parsing error. Incorrect format of Line. It does not contain all required fields. Line: " + line);
		}
	}
}
