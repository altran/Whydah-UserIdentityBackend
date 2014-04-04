package net.whydah.identity.application;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;

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
    private String id;
    private String name;
    private String defaultrole;
    private String defaultOrgid;
    private List<String> availableOrgIds;

    public Application(String id, String name) {
        this(id, name, null, null);
    }
    public Application(String id, String name, String defaultrole, String defaultOrgid) {
        this.id = id;
        this.name = name;
        this.defaultrole = defaultrole;
        this.defaultOrgid = defaultOrgid;
    }

    public Application(String id, String name, String defaultrole, String defaultOrgid, List<String> availableOrgIds) {
        this.id = id;
        this.name = name;
        this.defaultrole = defaultrole;
        this.defaultOrgid = defaultOrgid;
        this.availableOrgIds = availableOrgIds;
    }

    public String toXML() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n " +
                " <application>\n" +
                "   <applicationid>" + id + "</applicationid>\n" +
                "   <applicationname>" + name + "</applicationname>\n" +
                "   <defaultrole>" + defaultrole + "</defaultrole>\n" +
                "   <defaultorgid>" + defaultOrgid + "</defaultorgid>\n" +
                "  " + buildAvailableOrgIsXml() + "\n" +
                " </application>\n";
    }

    private String buildAvailableOrgIsXml() {
        StringBuilder availableXml = new StringBuilder("<availableOrgIs>\n");
        for (String availableOrgId : availableOrgIds) {
            availableXml.append("<orgId>" + availableOrgId + "</orgId>\n");
        }
        availableXml.append("<availableOrgIs>");
        return availableXml.toString();
    }


    public String getId() {
        return id;
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

    public List<String> getAvailableOrgIds() {
        return availableOrgIds;
    }

    public void setAvailableOrgIds(List<String> availableOrgIds) {
        if (availableOrgIds != null) {
            this.availableOrgIds = availableOrgIds;
        }
    }

    public void addAvailableOrgId(String availableOrgId) {
        if (availableOrgIds == null) {
            availableOrgIds = new ArrayList();
        }
        if (availableOrgId != null) {
            this.availableOrgIds.add(availableOrgId);
        }
    }

    public void removeAvailableOrgId(String availableOrgId) {
        if (availableOrgIds != null && availableOrgId != null) {
            availableOrgIds.remove(availableOrgId);
        }
    }

    public void setDefaultOrgid(String defaultOrgid) {
        this.defaultOrgid = defaultOrgid;
    }

    public void setDefaultrole(String defaultrole) {
        this.defaultrole = defaultrole;
    }

    @Override
    public String toString() {
        String availableIds = Joiner.on(", ").join(this.availableOrgIds);
        return "Application{" +
                "appId='" + id + '\'' +
                ", name='" + name + '\'' +
                ", defaultrole='" + defaultrole + '\'' +
                ", defaultOrgid='" + defaultOrgid + '\'' +
                ", availableOrgIds = '" + availableIds  + '\'' +
                '}';
    }
}
