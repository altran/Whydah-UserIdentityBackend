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

public class OrganizationImporter {

	private static final Logger logger = LoggerFactory.getLogger(OrganizationImporter.class);
	
	private static final int REQUIRED_NUMBER_OF_FIELDS = 2;
	
	private static final int ORGANIZATIONID = 0;
	private static final int ORGANIZATIONNAME = 1;
	
	private QueryRunner queryRunner;
	
	@Inject
	public OrganizationImporter(QueryRunner queryRunner) {
		this.queryRunner = queryRunner;
	}

	public void importOrganizations(String organizationsSource) {
		List<Organization> organizations = parseOrganizations(organizationsSource);
		saveOrganizations(organizations);
	}
	
	private void saveOrganizations(List<Organization> organizations) {
		try {
			for (Organization organization: organizations) {
				queryRunner.update("INSERT INTO Organization values (?, ?)", organization.getId(), organization.getName());
			}
		} catch(Exception e) {
			logger.error("Unable to persist organizations.", e);
			throw new RuntimeException("Unable to persist organizations.", e);
		}
	}
	
	protected static List<Organization> parseOrganizations(String organizationsSource) {

		BufferedReader reader = null;
		try {
			List<Organization> organizations = new ArrayList<>();
			logger.info("Importing data from {}", organizationsSource);
	        InputStream classpathStream = RoleMappingImporter.class.getClassLoader().getResourceAsStream(organizationsSource);
	        reader = new BufferedReader(new InputStreamReader(classpathStream, "ISO-8859-1"));
	        String line = null; 
	        while (null != (line = reader.readLine())) {
	        	boolean isComment = line.startsWith("#");
				if (isComment) {
	        		continue;
	        	}
				
	        	String[] lineArray = line.split(",");
	        	validateLine(line, lineArray);
	        	
	        	String organizationId = cleanString(lineArray[ORGANIZATIONID]);
	        	String organizationName = cleanString(lineArray[ORGANIZATIONNAME]);
	        	
	        	Organization organization = new Organization(organizationId, organizationName);
	            organizations.add(organization);
	        }
			return organizations;
		
		} catch (IOException ioe) {
			logger.error("Unable to read file {}", organizationsSource);
			throw new RuntimeException("Unable to import Organizations from file: " + organizationsSource);
		} finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.warn("Error closing stream", e);
                }
            }
        }
	}
	private static String cleanString(String string) {
		return string==null ? string : string.trim();
	}

	private static void validateLine(String line, String[] lineArray) {
		if (lineArray.length < REQUIRED_NUMBER_OF_FIELDS) {
			throw new RuntimeException("Organizations parsing error. Incorrect format of Line. It does not contain all required fields. Line: " + line);
		}
	}
}

