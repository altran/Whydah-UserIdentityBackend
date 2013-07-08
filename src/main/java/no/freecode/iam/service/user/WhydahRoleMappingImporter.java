package no.freecode.iam.service.user;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import no.freecode.iam.service.domain.UserPropertyAndRole;
import no.freecode.iam.service.repository.UserPropertyAndRoleRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class WhydahRoleMappingImporter {

	private static final Logger logger = LoggerFactory.getLogger(WhydahRoleMappingImporter.class);
	
	private static final int REQUIRED_NUMBER_OF_FIELDS = 7;

	private static final int USERID = 0;
	private static final int APPLICATIONID = 1;
	private static final int APPLICATIONNAME = 2;
	private static final int ORGANIZATIONID = 3;
	private static final int ORGANIZATIONNAME = 4;
	private static final int ROLENAME = 5;
	private static final int ROLEVALUE = 6;
	
    private UserPropertyAndRoleRepository roleMappingRepository;
    
    @Inject
	public WhydahRoleMappingImporter(UserPropertyAndRoleRepository roleMappingRepository) {
		this.roleMappingRepository = roleMappingRepository;
	}

    public List<UserPropertyAndRole> importRoleMapping(String roleMappingSource) {
    	List<UserPropertyAndRole> roles = parseRoleMapping(roleMappingSource);
    	saveRoleMapping(roles);
    	return roles;
    }
    
	private void saveRoleMapping(List<UserPropertyAndRole> roles) {
		for(UserPropertyAndRole userPropertyAndRole : roles) {
			roleMappingRepository.addUserPropertyAndRole(userPropertyAndRole);	
		}
	}

	protected static List<UserPropertyAndRole> parseRoleMapping(String roleMappingSource) {

		BufferedReader reader = null;
		try {
			List<UserPropertyAndRole> roleMappings = new ArrayList<>();
			logger.info("Importing data from {}", roleMappingSource);
	        InputStream classpathStream = WhydahRoleMappingImporter.class.getClassLoader().getResourceAsStream(roleMappingSource);
	        reader = new BufferedReader(new InputStreamReader(classpathStream, "ISO-8859-1"));
	        String line = null; 
	        while (null != (line = reader.readLine())) {
	        	boolean isComment = line.startsWith("#");
				if (isComment) {
	        		continue;
	        	}
				
	        	String[] lineArray = line.split(",");
	        	validateLine(line, lineArray);
	        	
	            UserPropertyAndRole userPropertyAndRole = new UserPropertyAndRole();

	            userPropertyAndRole.setUid(cleanString(lineArray[USERID]));
	            userPropertyAndRole.setAppId(cleanString(lineArray[APPLICATIONID]));
	            userPropertyAndRole.setApplicationName(cleanString(lineArray[APPLICATIONNAME]));
	        	
	            userPropertyAndRole.setOrgId(cleanString(lineArray[ORGANIZATIONID]));
	            userPropertyAndRole.setOrganizationName(cleanString(lineArray[ORGANIZATIONNAME]));
	            userPropertyAndRole.setRoleName(cleanString(lineArray[ROLENAME]));
	            userPropertyAndRole.setRoleValue(cleanString(lineArray[ROLEVALUE]));
	            
	            roleMappings.add(userPropertyAndRole);
	        }
			return roleMappings;
		
		} catch (IOException ioe) {
			logger.error("Unable to read file {}", roleMappingSource);
			throw new RuntimeException("Unable to import Role Mappings from file: " + roleMappingSource);
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
			throw new RuntimeException("Role Mapping parsing error. Incorrect format of Line. It does not contain all required fields. Line: " + line);
		}
	}
}
