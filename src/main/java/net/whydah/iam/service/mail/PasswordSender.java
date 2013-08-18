package net.whydah.iam.service.mail;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.whydah.iam.service.config.AppConfig;
import net.whydah.iam.service.domain.ChangePasswordToken;
import net.whydah.iam.service.domain.PasswordGenerator;
import net.whydah.iam.service.ldap.LDAPHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

/**
 * Set temporary password for user and send reset password email to user.
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 18.08.13
 */
@Singleton
public class PasswordSender {
    private static final Logger log = LoggerFactory.getLogger(MailSender.class);
    private static final String RESET_PASSWORD_SUBJECT = "Whydah password reset";
    private static final String CHANGE_PASSWORD_PATH = "/changepassword/";

    private final PasswordGenerator passwordGenerator;
    private final LDAPHelper ldapHelper;
    private final EmailBodyGenerator bodyGenerator;
    private final MailSender mailSender;
    private final String ssoLoginServiceUrl;


    @Inject
    public PasswordSender(PasswordGenerator passwordGenerator, LDAPHelper ldapHelper, EmailBodyGenerator bodyGenerator,
                          MailSender mailSender) {
        this.passwordGenerator = passwordGenerator;
        this.ldapHelper = ldapHelper;
        this.bodyGenerator = bodyGenerator;
        this.mailSender = mailSender;
        this.ssoLoginServiceUrl = AppConfig.appConfig.getProperty("ssologinservice");
    }

    public void resetPassword(String username, String userEmail) {
        String newPassword = passwordGenerator.generate();
        ChangePasswordToken changePasswordToken = new ChangePasswordToken(username, newPassword);
        String salt = passwordGenerator.generate();
        ldapHelper.setTempPassword(username, newPassword, salt);
        String token;
        try {
            token = changePasswordToken.generateTokenString(salt.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        String resetUrl = ssoLoginServiceUrl + CHANGE_PASSWORD_PATH + token;
        String body = bodyGenerator.resetPassword(resetUrl);

        mailSender.send(userEmail, RESET_PASSWORD_SUBJECT, body);
        log.info("Password reset for user {}, token={}", username, token);
        log.debug("salt=" + salt);
    }
}
