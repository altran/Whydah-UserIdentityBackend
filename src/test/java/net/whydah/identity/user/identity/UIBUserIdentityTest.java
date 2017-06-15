package net.whydah.identity.user.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.whydah.sso.user.types.UserIdentity;
import org.junit.Test;

import java.util.Random;
import java.util.UUID;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2014-11-05
 */

public class UIBUserIdentityTest {
    @Test
    public void testValidateCellPhoneOK() {
        LDAPUserIdentity userIdentity = new LDAPUserIdentity("uid1", "username1", "firstName1", "lastName1", "valid@email.dk", "password1", "", "personRef1");
        String[] telephoneNumbers = new String[]{"12345678", "+47 12345678", "+4799999999", "90 90 90 90", "", null};

        for (String telephoneNumber : telephoneNumbers) {
            userIdentity.setCellPhone(telephoneNumber);
            userIdentity.validate();
        }
    }
    @Test(expected = InvalidUserIdentityFieldException.class)
    public void testValidateCellPhoneInvalid() {
        LDAPUserIdentity userIdentity = new LDAPUserIdentity("uid1", "username1", "firstName1", "lastName1", "valid@email.dk", "password1", "", "personRef1");
        userIdentity.setCellPhone("900-FLYING-CIRCUS");
        userIdentity.validate();
    }


    @Test
    public void testValidatePersonRefOK() {
        LDAPUserIdentity userIdentity = new LDAPUserIdentity("uid", "username1", "firstName1", "lastName1", "valid@email.dk", "password1", null, "personRef1");
        String[] personRefs = new String[]{"0", "123", "abc", "123abc", "valid@email.dk", "123-456", "123/456", "", null};
        for (String personRef : personRefs) {
            userIdentity.setPersonRef(personRef);
            userIdentity.validate();
        }
    }

    @Test
    public void testParseUserIdentityJson() throws Exception {
        String userIdentityJson = "{\"username\":\"totto\", \"firstName\":\"Thor Henning\", \"lastName\":\"Hetland\", \"personRef\":\"\", \"email\":\"totto@totto.org\", \"cellPhone\":\"+4793009556\"}";
        UserIdentity representation;
        ObjectMapper mapper = new ObjectMapper();

        representation = mapper.readValue(userIdentityJson, UserIdentity.class);
    }

    @Test
    public void testNorthboundParseUserIdentityJson() throws Exception {
        String userIdentityJson = "{\"username\":\"totto\", \"firstName\":\"Thor Henning\", \"lastName\":\"Hetland\",  \"email\":\"totto@totto.org\", \"cellPhone\":\"+4793009556\"}";
        UserIdentity representation;
        ObjectMapper mapper = new ObjectMapper();

        representation = mapper.readValue(userIdentityJson, UserIdentity.class);
    }

    @Test
    public void testtestTypeUserJson() throws Exception {

        Random rand = new Random();
        rand.setSeed(new java.util.Date().getTime());
        LDAPUserIdentity userIdentity = new LDAPUserIdentity("uid",
                "us" + UUID.randomUUID().toString().replace("-", "").replace("_", ""),
                "Mt Test",
                "Testesen",
                UUID.randomUUID().toString().replace("-", "").replace("_", "") + "@getwhydah.com",
                "47" + Integer.toString(rand.nextInt(100000000)),
                null,
                "pref");

        userIdentity.validate();
    }
}
