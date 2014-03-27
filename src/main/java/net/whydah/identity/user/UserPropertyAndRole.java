package net.whydah.identity.user;

/**
 * A "holder"-class for a users various properties and roles. Backed by RDBMS scheme
 * User: totto
 * Date: 1/11/11
 * Time: 8:21 AM
 */
public class UserPropertyAndRole {
    private String uid;
    private String appId;
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

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
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

        if (appId != null ? !appId.equals(that.appId) : that.appId != null) {
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
        result = 31 * result + (appId != null ? appId.hashCode() : 0);
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
                ", appId='" + appId + '\'' +
                ", applicationName='" + applicationName + '\'' +
                ", orgId='" + orgId + '\'' +
                ", organizationName='" + organizationName + '\'' +
                ", roleName='" + roleName + '\'' +
                ", roleValue='" + roleValue + '\'' +
                '}';
    }
}
