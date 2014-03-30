package net.whydah.identity.application.role;

/**
 * Application.
 *
 * Erik's notes:
 *
 * applications
 *      application, applicationID, name
 *          relations   (organisation)
 *              relation1, id, name
 *                  properties / hashmap (mappet tidligere til roller)
 *              relation2, id, name
 *                  properties / hashmap
 *
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

    public String toXML() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n " +
                " <application>\n" +
                "   <applicationid>" + appId + "</applicationid>\n" +
                "   <applicationname>" + name + "</applicationname>\n" +
                "   <defaultrole>" + defaultrole + "</defaultrole>\n" +
                "   <defaultorgid>" + defaultOrgid + "</defaultorgid>\n" +
                " </application>\n";
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
