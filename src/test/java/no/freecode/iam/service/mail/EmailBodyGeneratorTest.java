package no.freecode.iam.service.mail;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class EmailBodyGeneratorTest {
    private final EmailBodyGenerator emailBodyGenerator = new EmailBodyGenerator();

    @Test
    public void newUser() {
        String navn = "Ola Dunk";
        String systemnavn = "Roterommet";
        String url = "https://sso.obos.no/newuser/jkhg4jkhg4kjhg4kjhgk3jhg4kj3grkj34hgr3hk4gk3rjhg4kj3hgr4kj3";
        String newUserEmailBody = emailBodyGenerator.newUser(navn, systemnavn, url);
        assertTrue(newUserEmailBody.contains(navn));
        assertTrue(newUserEmailBody.contains(systemnavn));
        assertTrue(newUserEmailBody.contains(url));
        assertTrue(newUserEmailBody.contains("Ny FreeCode-bruker"));
    }
    @Test
    public void resetPassword() {
        String url = "https://sso.obos.no/newuser/jkhg4jkhg4kjhg4kjhgk3jhg4kj3grkj34hgr3hk4gk3rjhg4kj3hgr4kj3";
        String newUserEmailBody = emailBodyGenerator.resetPassword(url);
        assertTrue(newUserEmailBody.contains(url));
    }
}
