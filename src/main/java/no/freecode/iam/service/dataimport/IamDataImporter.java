package no.freecode.iam.service.dataimport;

import no.freecode.iam.service.config.AppConfig;
import no.freecode.iam.service.user.ApplicationImporter;
import no.freecode.iam.service.user.OrganizationImporter;
import no.freecode.iam.service.user.RoleMappingImporter;
import no.freecode.iam.service.user.WhydahUserIdentityImporter;

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
		
        String userImportSource = AppConfig.appConfig.getProperty("userimportsource");
        String roleMappingImportSource = AppConfig.appConfig.getProperty("rolemappingimportsource");
        String organizationsImportSource = AppConfig.appConfig.getProperty("organizationsimportsource");
        String applicationsImportSource= AppConfig.appConfig.getProperty("applicationsimportsource");
        
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
