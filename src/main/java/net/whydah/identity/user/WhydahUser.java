package net.whydah.identity.user;

import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.user.role.UserPropertyAndRole;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class WhydahUser {
    private UserIdentity identity = null;
    private List<UserPropertyAndRole> propsandroles = new ArrayList<>();

    public WhydahUser(UserIdentity identity, List<UserPropertyAndRole> propsandroles) {
        this.identity = identity;
        this.propsandroles = propsandroles;
    }

    public WhydahUser() {
    }

    public String toXML() {
        StringBuilder strb = new StringBuilder();
        String headAndIdentity = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<whydahuser>\n" +
                "    <identity>\n" +
                "        <username>" + identity.getUsername() + "</username>\n" +
                "        <cellPhone>" + (identity.getCellPhone() != null ? identity.getCellPhone() : "") + "</cellPhone>\n" +
                "        <email>" + identity.getEmail() + "</email>\n" +
                "        <firstname>" + identity.getFirstName() +"</firstname>\n" +
                "        <lastname>" + identity.getLastName() + "</lastname>\n" +
                "        <personRef>" + (identity.getPersonRef() != null ? identity.getPersonRef() : "") + "</personRef>\n" +
                "        <UID>" + identity.getUid() + "</UID>\n" +
                "    </identity>\n" +
                "    <applications>\n";
         strb.append(headAndIdentity);

        for (UserPropertyAndRole u : propsandroles) {
                    strb.append(
                    "        <application>\n" +
                    "            <appId>" + u.getAppId() + "</appId>\n" +
                    "            <applicationName>" + u.getApplicationName() + "</applicationName>\n" +
                    "            <orgID>" + u.getOrgId() + "</orgID>\n" +
                    "            <roleName>" + u.getRoleName() + "</roleName>\n" +
                    "            <roleValue>" + u.getRoleValue() + "</roleValue>\n" +
                    "        </application>\n"
                    );
        }
        strb.append(
                "    </applications>\n" +
                "</whydahuser>"
        );
        return strb.toString();
    }

    /**
     * Copy from UserToken.parseAndUpdatefromUserIdentity from SecurityTokenService
     */
    public static WhydahUser fromXML(String userIdentityXML) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
            Document doc = documentBuilder.parse(new InputSource(new StringReader(userIdentityXML)));
            XPath xPath = XPathFactory.newInstance().newXPath();
            String uid = (String) xPath.evaluate("//identity/UID", doc, XPathConstants.STRING);
            String userName = (String) xPath.evaluate("//identity/username", doc, XPathConstants.STRING);
            String firstName = (String) xPath.evaluate("//identity/firstname", doc, XPathConstants.STRING);
            String lastName = (String) xPath.evaluate("//lastname", doc, XPathConstants.STRING);
            String email = (String) xPath.evaluate("//email", doc, XPathConstants.STRING);
            String personRef = (String) xPath.evaluate("//personRef", doc, XPathConstants.STRING);

            UserIdentity identity = new UserIdentity();
            identity.setUid(uid);
            identity.setUsername(userName);
            identity.setFirstName(firstName);
            identity.setLastName(lastName);
            identity.setEmail(email);
            identity.setPersonRef(personRef);

            /*
            NodeList applicationNodes = (NodeList) xPath.evaluate("//application", doc, XPathConstants.NODESET);
            for(int i=0; i<applicationNodes.getLength(); i++) {
                Node appNode = applicationNodes.item(i);
                NodeList children = appNode.getChildNodes();
                HashMap<String, String> values = getAppValues(children);
                //putApplicationCompanyRoleValue(values.get("appId"), values.get("applicationName"), values.get("orgID"), values.get("organizationName"), values.get("roleName"), values.get("roleValue"));
            }
            */
            return new WhydahUser(identity, null);
        } catch (Exception e) {
            //log.error("Error parsing userIdentityXML " + userIdentityXML, e);
        }
        return null;
    }

     /*
    private static HashMap<String, String> getAppValues(NodeList children) {
        HashMap<String, String> values = new HashMap<>();
        for(int j=0; j < children.getLength(); j++) {
            Node node = children.item(j);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                values.put(node.getNodeName(), node.getTextContent());
            }
        }
        return values;
    }

    public void putApplicationCompanyRoleValue(String p_application_ID, String p_application_Name, String p_company_ID, String p_company_name, String p_role, String p_value) {
        if (applicationCompanyRoleValueMap.containsKey(p_application_ID)) {
            ApplicationData application = applicationCompanyRoleValueMap.get(p_application_ID);
            CompanyRoles company = application.getCompaniesAndRolesMap().get(p_company_ID);
            if (company != null) {  // Application and company exists, just update the rolemap
                company.getRoleMap().put(p_role, p_value);
            } else {
                company = new CompanyRoles();
                company.setCompanyNumber(p_company_ID);
                company.setCompanyName(p_company_name);
                Map<String, String> rolemap = new HashMap<String, String>();
                rolemap.put(p_role, p_value);
                company.setRoleMap(rolemap);
                application.addCompanyWithRoles(company.getCompanyNumber(), company);
                applicationCompanyRoleValueMap.put(application.getApplicationID(), application);
            }
            // Add or update existing application
        } else {
            ApplicationData application = new ApplicationData();
            application.setApplicationID(p_application_ID);
            application.setApplicationName(p_application_Name);
            CompanyRoles company = new CompanyRoles();
            company.setCompanyNumber(p_company_ID);
            company.setCompanyName(p_company_name);
            Map<String,String> rolemap = new HashMap<String, String>();
            rolemap.put(p_role, p_value);
            company.setRoleMap(rolemap);
            application.addCompanyWithRoles(company.getCompanyNumber(), company);
            applicationCompanyRoleValueMap.put(application.getApplicationID(), application);
        }
    }
    */




    public UserIdentity getIdentity() {
        return identity;
    }

    public void setIdentity(UserIdentity identity) {
        this.identity = identity;
    }

    public List<UserPropertyAndRole> getPropsAndRoles() {
        return propsandroles;
    }

    public void addPropsAndRoles(UserPropertyAndRole propsandrole) {
        this.propsandroles.add(propsandrole);
    }

    public void setPropsAndRoles(List<UserPropertyAndRole> propsandroles) {
        this.propsandroles = propsandroles;
    }
}
