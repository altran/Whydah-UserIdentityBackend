package net.whydah.identity.user.email;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.whydah.identity.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Send reset password email to user.
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 18.08.13
 */
@Singleton
public class PasswordSender {
    private static final Logger log = LoggerFactory.getLogger(PasswordSender.class);
    private static final String RESET_PASSWORD_SUBJECT = "Whydah password reset";
    private static final String CHANGE_PASSWORD_PATH = "/changepassword/";
    private final String ssoLoginServiceUrl;

    private final EmailBodyGenerator bodyGenerator;
    private final MailSender mailSender;

    @Inject
    public PasswordSender(EmailBodyGenerator bodyGenerator, MailSender mailSender) {
        this.bodyGenerator = bodyGenerator;
        this.mailSender = mailSender;
        this.ssoLoginServiceUrl = AppConfig.appConfig.getProperty("ssologinservice");
    }

    public void sendResetPasswordEmail(String username, String token, String userEmail) {
        String resetUrl = ssoLoginServiceUrl + CHANGE_PASSWORD_PATH + token;
        log.info("Sending resetPassword email for user {} to {}, token={}", username, userEmail, token);
        String body = bodyGenerator.resetPassword(resetUrl, username);
        mailSender.send(userEmail, RESET_PASSWORD_SUBJECT, body);
    }
}
