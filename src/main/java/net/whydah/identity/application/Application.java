package net.whydah.identity.application;

import com.google.common.base.Joiner;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    private String id;
    private String name;
    private String defaultRoleName;
    private String defaultOrgName;


    private List<String> availableOrgNames;
    private List<String> availableRoleNames;

    private Application() {
    }

    public Application(String id, String name) {
        this(id, name, null, null);
    }
    public Application(String id, String name, String defaultRoleName, String defaultOrgName) {
        this.id = id;
        this.name = name;
        this.defaultRoleName = defaultRoleName;
        this.defaultOrgName = defaultOrgName;
    }


    public Application(String id, String name, String defaultRoleName, String defaultOrgName, List<String> availableOrgNames) {
        this.id = id;
        this.name = name;
        this.defaultRoleName = defaultRoleName;
        this.defaultOrgName = defaultOrgName;
        this.availableOrgNames = availableOrgNames;
    }

    /**
     * {
     "id": "id1",
     "name": "test",
     "defaultRoleName": "default1role",
     "defaultOrgName": "defaultorgid",
     "availableOrgNames": [
     "developer@customer",
     "consultant@customer"
     ]
     }
     * @param applicationJson
     * @return
     */
    public static Application fromJson(String applicationJson) throws  IllegalArgumentException {
            try {
                Application application;

                ObjectMapper mapper = new ObjectMapper();
                application = mapper.readValue(applicationJson, Application.class);
                return application;
            } catch (JsonMappingException e) {
                throw new IllegalArgumentException("Error mapping json for " + applicationJson, e);
            } catch (JsonParseException e) {
                throw new IllegalArgumentException("Error parsing json for " + applicationJson, e);
            } catch (IOException e) {
                throw new IllegalArgumentException("Error reading json for " + applicationJson, e);
            }
    }

    public String toJson() {
        String applicationJson = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            applicationJson =  mapper.writeValueAsString(this);
        } catch (IOException e) {
            log.info("Could not create json from this object {}", toString(), e);
        }
        log.trace("JSON serialization:",applicationJson);
        return applicationJson;
    }

    public String toXML() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n " +
                " <application>\n" +
                "   <applicationid>" + id + "</applicationid>\n" +
                "   <applicationname>" + name + "</applicationname>\n" +
                "   <defaultrolename>" + defaultRoleName + "</defaultrolename>\n" +
                "   <defaultorganizationname>" + defaultOrgName + "</defaultorganizationname>\n" +
                "  " + buildAvailableOrgAsXml() + "\n" +
                " </application>\n";
    }

    private String buildAvailableOrgAsXml() {
        if(availableOrgNames == null || availableOrgNames.size() == 0) {
            return "<organizationsnames/>";
        }else {
            StringBuilder availableXml = new StringBuilder("<organizationsnames>\n");
            for (String availableOrgId : availableOrgNames) {
                availableXml.append("<orgName>").append(availableOrgId).append("</orgName>").append("\n");
            }
            availableXml.append("</organizationsnames>");
            return availableXml.toString();
        }
    }

    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getDefaultRoleName() {
        return defaultRoleName;
    }
    public String getDefaultOrgName() {
        return defaultOrgName;
    }

    public List<String> getAvailableOrgNames() {
        return availableOrgNames;
    }

    public void setAvailableOrgNames(List<String> availableOrgNames) {
        if (availableOrgNames != null) {
            this.availableOrgNames = availableOrgNames;
        }
    }

    public void addAvailableOrgId(String availableOrgId) {
        if (availableOrgNames == null) {
            availableOrgNames = new ArrayList();
        }
        if (availableOrgId != null) {
            this.availableOrgNames.add(availableOrgId);
        }
    }

    public void removeAvailableOrgId(String availableOrgId) {
        if (availableOrgNames != null && availableOrgId != null) {
            availableOrgNames.remove(availableOrgId);
        }
    }


    public List<String> getAvailableRoleNames() {
        return availableRoleNames;
    }

    public void setAvailableRoleNames(List<String> availableRoleName) {
        if (availableRoleName != null) {
            this.availableRoleNames = availableRoleName;
        }
    }

    public void addAvailableRoleName(String availableRoleName) {
        if (availableRoleNames == null) {
            availableRoleNames = new ArrayList();
        }
        if (availableRoleName != null) {
            this.availableRoleNames.add(availableRoleName);
        }
    }

    public void removeAvailableRoleName(String availableRoleName) {
        if (availableRoleNames != null && availableRoleName != null) {
            availableRoleNames.remove(availableRoleName);
        }
    }

    public void setDefaultOrgName(String defaultOrgName) {
        this.defaultOrgName = defaultOrgName;
    }

    public void setDefaultRoleName(String defaultRoleName) {
        this.defaultRoleName = defaultRoleName;
    }

    @Override
    public String toString() {
        String availableNamesString = "";
         if (this.availableOrgNames != null) {
             availableNamesString = Joiner.on(", ").join(this.availableOrgNames);
         }
        return "Application{" +
                "appId='" + id + '\'' +
                ", name='" + name + '\'' +
                ", defaultrolename='" + defaultRoleName + '\'' +
                ", defaultorganizationname='" + defaultOrgName + '\'' +
                ", availableorganizationnames = '" + availableNamesString  + '\'' +
                '}';
    }

    public void setId(String id) {
        this.id = id;
    }
}
