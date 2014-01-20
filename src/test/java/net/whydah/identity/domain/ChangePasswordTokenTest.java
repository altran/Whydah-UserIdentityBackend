package net.whydah.identity.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ChangePasswordTokenTest {
    @Test
    public void testToken() {
        String user = "bentesolvang@hotmail.org";
        String password = "N34jh87a";
        byte[] salt = "jdhUhj\\?".getBytes();
        ChangePasswordToken t = new ChangePasswordToken(user, password);
        String token = t.generateTokenString(salt);
        ChangePasswordToken t2 = ChangePasswordToken.fromTokenString(token, salt);
        assertEquals(user, t2.getUserid());
        assertEquals(password, t2.getPassword());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongSalt() {
        String user = "bentesolvang@hotmail.org";
        String password = "N34jh87a";
        byte[] salt = "jdhUhj\\?".getBytes();
        ChangePasswordToken t = new ChangePasswordToken(user, password);
        String token = t.generateTokenString(salt);
        ChangePasswordToken.fromTokenString(token, "salt".getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testtimedoutToken() {
        ChangePasswordToken.fromTokenString("bmFsbGU6MTMwMTgzMDE1NjcyNjpVMTkxUjFOMVpnSnJlbVZlVUVoNlRINVlkazU4ZG5ZQWZFaHlUSDltYWdkOFdHb0o", "1234".getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFakeToken() {
        ChangePasswordToken.fromTokenString("dilldall", "salt".getBytes());
    }
}
