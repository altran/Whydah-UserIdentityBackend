package net.whydah.iam.service.mail;

import com.google.inject.Singleton;

import java.util.HashMap;

@Singleton
public class MockMail {
    private final HashMap<String, String> passwords = new HashMap<String, String>();

    public void sendpasswordmail(String to, String user, String token) {
        passwords.put(user,  token);
        System.out.println("Sender liksom mail til " + to + " med token " + token);
    }

    public String getToken(String to) {
        return passwords.get(to);
    }
}
