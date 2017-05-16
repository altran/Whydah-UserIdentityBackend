package net.whydah.identity.user.identity;

import net.whydah.sso.user.types.UserIdentity;
import org.apache.directory.api.ldap.model.schema.syntaxCheckers.TelephoneNumberSyntaxChecker;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

public class UserIdentityValidatorUtil {

    public static final String UID = "uid";
    private static final TelephoneNumberSyntaxChecker telephoneNumberSyntaxChecker = new TelephoneNumberSyntaxChecker();


    public static void validate(UserIdentity userIdentity) throws InvalidUserIdentityFieldException {
        validateSetAndMinimumLength(UID, userIdentity.getUid(), 2);
        validateSetAndMinimumLength("username", userIdentity.getUsername(), 3);

        validateEmail(userIdentity.getEmail());

        //Printable string (alphabetic, digits, ', (, ), +, ,, -, ., /, :, ?, and space) and "
        //http://pic.dhe.ibm.com/infocenter/zvm/v6r3/index.jsp?topic=%2Fcom.ibm.zvm.v630.kldl0%2Fkldl023.htm
        if (userIdentity.getCellPhone() != null && !telephoneNumberSyntaxChecker.isValidSyntax(userIdentity.getCellPhone())) {
            throw new InvalidUserIdentityFieldException("cellPhone", userIdentity.getCellPhone());
        }

        // valid
    }

    private static void validateSetAndMinimumLength(String key, String value, int minLength) {
        if (value == null || value.length() < minLength) {
            throw new InvalidUserIdentityFieldException(key, value);
        }
    }


    private static void validateEmail(String email) {
        if (email == null || email.length() < 5) {
            throw new InvalidUserIdentityFieldException("email", email);
        }

        InternetAddress internetAddress = new InternetAddress();
        internetAddress.setAddress(email);
        try {
            internetAddress.validate();
        } catch (AddressException e) {
            throw new InvalidUserIdentityFieldException("email", email);
        }
    }

}
