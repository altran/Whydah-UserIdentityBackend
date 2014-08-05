package net.whydah.identity.dataimport;

import com.google.inject.Inject;
import net.whydah.identity.config.AppConfig;

public class IamDataImporter {
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
        String jdbcDriverString = AppConfig.appConfig.getProperty("roledb.jdbc.driver");

        if (jdbcDriverString.contains("hsqldb")) {
            databaseHelper.initDB(DatabaseHelper.DB_DIALECT.HSSQL);
        } else if(jdbcDriverString.contains("mysql")) {
            databaseHelper.initDB(DatabaseHelper.DB_DIALECT.MYSQL);
        } else {
            throw new RuntimeException("Unknown database driver found in configuration - " + jdbcDriverString);
        }

        applicationImporter.importApplications(applicationsImportSource);
        organizationImporter.importOrganizations(organizationsImportSource);
        userImporter.importUsers(userImportSource);
        roleMappingImporter.importRoleMapping(roleMappingImportSource);
	}
}
