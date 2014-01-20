package net.whydah.identity.mail;

import net.whydah.identity.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a>
 */
public class MailSender {
    private static final Logger log = LoggerFactory.getLogger(MailSender.class);
    public static final String FROM_ADDRESS = "notworking@whydah.net";
    private final String username;
    private final String password;

    public MailSender() {
        this.username = AppConfig.appConfig.getProperty("gmail.username");
        this.password = AppConfig.appConfig.getProperty("gmail.password");
    }

    /*
    public static void main(String[] args) {
        new MailSender().send("erik@cantara.no", "Whydah MailSender test", "Email via Gmail test");
    }
    */

    public void send(String recipients, String subject, String body) {
        log.debug("Sending email to recipients={}, subject={}, body={}", new String[]{recipients, subject, body});

        //Gmail props
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        //Cantara smtp, will only work with @cantara-adresses
        /*
        Properties props = new Properties();
        props.put("mail.smtp.auth", "false");
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.smtp.host", "www.cantara.no");
        props.put("mail.smtp.port", "25");
        Session session = Session.getInstance(props);
        */
        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_ADDRESS));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
            message.setSubject(subject);

            message.setContent(body, "text/html; charset=utf-8");
//            message.setText(body);
            Transport.send(message);
            log.info("Sent email to " + recipients);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
