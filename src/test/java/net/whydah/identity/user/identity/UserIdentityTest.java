package net.whydah.identity.user.identity;

import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2014-11-05
 */

public class UserIdentityTest {
    @Test
    public void testValidateCellPhoneOK() {
        UserIdentity userIdentity = new UserIdentity("uid1", "username1", "firstName1", "lastName1", "valid@email.dk", "password1", "", "personRef1");
        String[] telephoneNumbers = new String[]{"12345678", "+47 12345678", "+4799999999", "90 90 90 90", null};

        for (String telephoneNumber : telephoneNumbers) {
            userIdentity.setCellPhone(telephoneNumber);
            userIdentity.validate();
        }
    }
    @Test(expected = InvalidUserIdentityFieldException.class)
    public void testValidateCellPhoneInvalid() {
        UserIdentity userIdentity = new UserIdentity("uid1", "username1", "firstName1", "lastName1", "valid@email.dk", "password1", "", "personRef1");
        userIdentity.setCellPhone("900-FLYING-CIRCUS");
        userIdentity.validate();
    }


    @Test
    public void testValidatePersonRefOK() {
        UserIdentity userIdentity = new UserIdentity("uid1", "username1", "firstName1", "lastName1", "valid@email.dk", "password1", null, "personRef1");
        String[] personRefs = new String[]{"0", "123", "abc", "123abc", "", null};
        for (String personRef : personRefs) {
            userIdentity.setPersonRef(personRef);
            userIdentity.validate();
        }
    }
    @Test
    public void testValidatePersonRefThrowsException() {
        UserIdentity userIdentity = new UserIdentity("uid1", "username1", "firstName1", "lastName1", "valid@email.dk", "password1", null, "personRef1");
        String[] personRefs = new String[]{"valid@email.dk", "123-456", "123/456"};
        for (String personRef : personRefs) {
            userIdentity.setPersonRef(personRef);
            try {
                userIdentity.validate();
                fail("Expected InvalidUserIdentityFieldException.");
            } catch (InvalidUserIdentityFieldException e) {
                //Expect exception
            }
        }
    }

}
