package net.whydah.identity.user.identity;

import org.junit.Test;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2014-11-05
 */

public class UserIdentityTest {
    @Test
    public void testValidateCellPhoneOK() {
        UserIdentity userIdentity = new UserIdentity("uid1", "username1", "firstName1", "lastName1", "personRef1", "valid@email.dk", "", "password1");
        String[] telephoneNumbers = new String[]{"12345678", "+47 12345678", "+4799999999", "90 90 90 90", null};

        for (String telephoneNumber : telephoneNumbers) {
            userIdentity.setCellPhone(telephoneNumber);
            userIdentity.validate();
        }
    }

    @Test(expected = InvalidUserIdentityFieldException.class)
    public void testValidateCellPhoneInvalid() {
        UserIdentity userIdentity = new UserIdentity("uid1", "username1", "firstName1", "lastName1", "personRef1", "valid@email.dk", "", "password1");
        userIdentity.setCellPhone("900-FLYING-CIRCUS");
        userIdentity.validate();
    }
}
