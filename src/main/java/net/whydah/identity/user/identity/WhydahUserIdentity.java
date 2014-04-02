package net.whydah.identity.user.identity;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.Serializable;

/**
 * A class representing the identity of a User - backed by LDAP scheme.
 * See getLdapAttributes in LDAPHelper for mapping to LDAP attributes.
 *
 * @author totto
 */
public class WhydahUserIdentity implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(WhydahUserIdentity.class);
    private static final long serialVersionUID = 1;

    private String uid;
    private String username;
    private String firstName;
    private String lastName;
    private String personRef;
    private String email;
    private String cellPhone;

    private transient String password;

    public WhydahUserIdentity() {
    }

    public WhydahUserIdentity(String uid, String username, String firstName, String lastName, String personRef,
                              String email, String cellPhone, String password) {
        this.uid = uid;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.personRef = personRef;
        this.email = email;
        this.cellPhone = cellPhone; //TODO Validate valid cellPhone
        this.password = password;
    }

    public boolean validate() {
        if (uid == null || uid.length() < 2) {
            logger.error("UID {} not valid", uid);
            return false;
        }
        if (username == null || username.length() < 3) {
            logger.error("username {} not valid", username);
            return false;
        }
        if (firstName == null || firstName.length() < 2) {
            logger.error("firstName {} not valid", firstName);
            return false;
        }
        if (lastName == null || lastName.length() < 2) {
            logger.error("lastName {} not valid", lastName);
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "WhydahUserIdentity{" +
                "uid='" + uid + '\'' +
                ", username='" + username + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", personRef='" + personRef + '\'' +
                ", email='" + email + '\'' +
                ", cellPhone='" + cellPhone + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WhydahUserIdentity that = (WhydahUserIdentity) o;

        if (uid != null ? !uid.equals(that.uid) : that.uid != null) {
            return false;
        }
        if (username != null ? !username.equals(that.username) : that.username != null) {
            return false;
        }
        if (cellPhone != null ? !cellPhone.equals(that.cellPhone) : that.cellPhone != null) {
            return false;
        }
        if (email != null ? !email.equals(that.email) : that.email != null) {
            return false;
        }
        if (firstName != null ? !firstName.equals(that.firstName) : that.firstName != null) {
            return false;
        }
        if (lastName != null ? !lastName.equals(that.lastName) : that.lastName != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = uid != null ? uid.hashCode() : 0;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (cellPhone != null ? cellPhone.hashCode() : 0);
        return result;
    }

    public String getPersonName() {
        return firstName + ' ' + lastName;
    }
    public String getPersonRef() {
        return personRef;
    }
    public String getUid() {
        return uid;
    }
    public String getUsername() {
        return username;
    }
    public String getFirstName() {
        return firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public String getEmail() {
        return email;
    }
    public String getCellPhone() {
        return cellPhone;
    }
    public String getPassword() {
        return password;
    }

    public void setPersonRef(String personRef) {
        this.personRef = personRef;
    }
    public void setUid(String uid) {
        this.uid = uid;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setCellPhone(String cellPhone) {
        this.cellPhone = cellPhone;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public static WhydahUserIdentity fromJson(String userJson) {
        try {
            WhydahUserIdentity userIdentity = new WhydahUserIdentity();

            JSONObject jsonobj = new JSONObject(userJson);

            String username = jsonobj.getString("username");
            InternetAddress internetAddress = new InternetAddress();
            String email = jsonobj.getString("email");
            if (email.contains("+")){
                email = replacePlusWithEmpty(email);
            }
            internetAddress.setAddress(email);
            try {
                internetAddress.validate();
                userIdentity.setEmail(email);
            } catch (AddressException e) {
                //log.error(String.format("E-mail: %s is of wrong format.", email));
                //return Response.status(Response.Status.BAD_REQUEST).build();
                throw new IllegalArgumentException(String.format("E-mail: %s is of wrong format.", email));
            }
            userIdentity.setUsername(username);
            userIdentity.setFirstName(jsonobj.getString("firstName"));
            userIdentity.setLastName(jsonobj.getString("lastName"));

            userIdentity.setCellPhone(jsonobj.getString("cellPhone"));
            userIdentity.setPersonRef(jsonobj.getString("personRef"));
            //userIdentity.setUid(UUID.randomUUID().toString());
            return userIdentity;
        } catch (JSONException e) {
            throw new IllegalArgumentException("Error parsing json", e);
        }
    }

    private static String replacePlusWithEmpty(String email){
        String[] words = email.split("[+]");
        if (words.length == 1) {
            return email;
        }
        email  = "";
        for (String word : words) {
            email += word;
        }
        return email;
    }

}
