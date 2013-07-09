package net.whydah.iam.service.domain;

/**
 * Application.
 */
public class Application {
    private String appId;
    private String name;
    private String defaultrole;
    private String defaultOrgid;

    public Application(String appId, String name) {
        this(appId, name, null, null);
    }
    public Application(String appId, String name, String defaultrole, String defaultOrgid) {
        this.appId = appId;
        this.name = name;
        this.defaultrole = defaultrole;
        this.defaultOrgid = defaultOrgid;
    }

    public String getAppId() {
        return appId;
    }

    public String getName() {
        return name;
    }

    public String getDefaultrole() {
        return defaultrole;
    }

    public String getDefaultOrgid() {
        return defaultOrgid;
    }

    @Override
    public String toString() {
        return "Application{" +
                "appId='" + appId + '\'' +
                ", name='" + name + '\'' +
                ", defaultrole='" + defaultrole + '\'' +
                ", defaultOrgid='" + defaultOrgid + '\'' +
                '}';
    }
}
