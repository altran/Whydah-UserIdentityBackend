package net.whydah.identity.user.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class MockMail {
    private static final Logger log = LoggerFactory.getLogger(MockMail.class);

    private final HashMap<String, String> passwords = new HashMap<>();

    public void sendpasswordmail(String to, String user, String token) {
        passwords.put(user, token);
        log.debug("Sending mocked mail to " + to + " with token " + token);
    }

    public String getToken(String to) {
        return passwords.get(to);
    }
}
