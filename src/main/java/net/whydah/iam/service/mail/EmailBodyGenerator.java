package net.whydah.iam.service.mail;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.whydah.iam.service.exceptions.SystemException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;


/**
 * Genererer email body for epost som skal sendes ut.
 */
public class EmailBodyGenerator {
    private final Configuration freemarkerConfig;
    private static final String NEW_USER_EMAIL_TEMPLATE = "WelcomeNewUser.ftl";
    private static final String RESET_PASSWORD_EMAIL_TEMPLATE = "PasswordResetEmail.ftl";

    public EmailBodyGenerator() {
        freemarkerConfig = new Configuration();
        freemarkerConfig.setTemplateLoader(new ClassTemplateLoader(getClass(), "/templates/email"));
        freemarkerConfig.setDefaultEncoding("UTF-8");
        freemarkerConfig.setLocalizedLookup(false);
        freemarkerConfig.setTemplateUpdateDelay(60000);
    }


    public String resetPassword(String url) {
        HashMap<String, String> model = new HashMap<String, String>();
        model.put("url", url);
        return createBody(RESET_PASSWORD_EMAIL_TEMPLATE, model);
    }

    public String newUser(String navn, String systemnavn, String url) {
        HashMap<String, String> model = new HashMap<String, String>();
        model.put("navn", navn);
        model.put("systemnavn", systemnavn);
        model.put("url", url);
        return createBody(NEW_USER_EMAIL_TEMPLATE, model);
    }

    private String createBody(String templateName, HashMap<String, String> model) {
        StringWriter stringWriter = new StringWriter();
        try {
            Template template = freemarkerConfig.getTemplate(templateName);
            template.process(model, stringWriter);
        } catch (TemplateException e) {
            throw new SystemException(e.getLocalizedMessage(), e);
        } catch (IOException e) {
            throw new SystemException(e.getLocalizedMessage(), e);
        }
        return stringWriter.toString();
    }
}
