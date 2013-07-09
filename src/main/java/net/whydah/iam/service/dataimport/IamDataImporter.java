package net.whydah.iam.service.dataimport;

import net.whydah.iam.service.config.AppConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class IamDataImporter {

	private static final Logger logger = LoggerFactory.getLogger(IamDataImporter.class);
	
	private DatabaseHelper databaseHelper;
	private ApplicationImporter applicationImporter;
	private OrganizationImporter organizationImporter;
	private WhydahUserIdentityImporter userImporter;
	private RoleMappingImporter roleMappingImporter;
	
	@Inject
	public IamDataImporter(DatabaseHelper databaseHelper, ApplicationImporter applicationImporter, 
						   OrganizationImporter organizationImporter, WhydahUserIdentityImporter userImporter, 
						   RoleMappingImporter roleMappingImporter)  {
		
		this.databaseHelper = databaseHelper;
		this.applicationImporter = applicationImporter;
		this.organizationImporter = organizationImporter;
		this.userImporter = userImporter;
		this.roleMappingImporter = roleMappingImporter;
	}
	
	public void importIamData() {
		
        String userImportSource = AppConfig.appConfig.getProperty("import.usersource");
        String roleMappingImportSource = AppConfig.appConfig.getProperty("import.rolemappingsource");
        String organizationsImportSource = AppConfig.appConfig.getProperty("import.organizationssource");
        String applicationsImportSource= AppConfig.appConfig.getProperty("import.applicationssource");
        
        logger.info("Initializing database.");
        databaseHelper.initDB();
        
        logger.info("Initializing Applications.");
        applicationImporter.importApplications(applicationsImportSource);
        
        logger.info("Initializing Organizations.");
        organizationImporter.importOrganizations(organizationsImportSource);
        
        logger.info("Initializing Users.");
        userImporter.importUsers(userImportSource);
        
        logger.info("Initializing Role mapping.");
        roleMappingImporter.importRoleMapping(roleMappingImportSource);
	}
}
