package net.whydah.identity.user.role;

/**
 * A Java representation for a user's properties and roles as it is stored in a RDBMS.
 *
 * A user (uid)
 * has the role (roleName, roleValue)
 * in this application (applicationId, applicationName)
 * from the relation (orgId, organizationName).
 *
 * An example to illustrate why the context/relation/organization concept is needed:
 *
 * As an employee (relation: orgId, organizationName),
 * a user (uid)
 * has the "reader" role in
 * application CompanyCMS.
 *
 * As technical writer in the Company (relation: orgId, organizationName),
 * a user (uid)
 * has the "writer" role in
 * application CompanyCMS.
 *
 * @author totto
 * @since 1/11/11
 */
public class UserPropertyAndRole {
    private String uid;

    private String applicationId;
    private transient String applicationName;

    private String orgId;
    private transient String organizationName;

    private String roleName;
    private String roleValue;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicationName() {
        return (applicationName == null ? "" : applicationName);
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getOrgId() {
        return (orgId == null ? "" : orgId);
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getOrganizationName() {
        return (organizationName == null ? "" : organizationName);
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleValue() {
        return roleValue;
    }

    public void setRoleValue(String roleValue) {
        this.roleValue = roleValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UserPropertyAndRole that = (UserPropertyAndRole) o;

        if (applicationId != null ? !applicationId.equals(that.applicationId) : that.applicationId != null) {
            return false;
        }
        if (applicationName != null ? !applicationName.equals(that.applicationName) : that.applicationName != null) {
            return false;
        }
        if (orgId != null ? !orgId.equals(that.orgId) : that.orgId != null) {
            return false;
        }
        if (organizationName != null ? !organizationName.equals(that.organizationName) : that.organizationName != null) {
            return false;
        }
        if (roleName != null ? !roleName.equals(that.roleName) : that.roleName != null) {
            return false;
        }
        if (roleValue != null ? !roleValue.equals(that.roleValue) : that.roleValue != null) {
            return false;
        }
        if (uid != null ? !uid.equals(that.uid) : that.uid != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = uid != null ? uid.hashCode() : 0;
        result = 31 * result + (applicationId != null ? applicationId.hashCode() : 0);
        result = 31 * result + (applicationName != null ? applicationName.hashCode() : 0);
        result = 31 * result + (orgId != null ? orgId.hashCode() : 0);
        result = 31 * result + (organizationName != null ? organizationName.hashCode() : 0);
        result = 31 * result + (roleName != null ? roleName.hashCode() : 0);
        result = 31 * result + (roleValue != null ? roleValue.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UserPropertyAndRole{" +
                "uid='" + uid + '\'' +
                ", applicationId='" + applicationId + '\'' +
                ", applicationName='" + applicationName + '\'' +
                ", orgId='" + orgId + '\'' +
                ", organizationName='" + organizationName + '\'' +
                ", roleName='" + roleName + '\'' +
                ", roleValue='" + roleValue + '\'' +
                '}';
    }
}
