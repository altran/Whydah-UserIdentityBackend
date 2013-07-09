package net.whydah.iam.service.domain;

import java.io.UnsupportedEncodingException;

import com.sun.jersey.core.util.Base64;

public class ChangePasswordToken {
    private static final long lifetime = 3 * 24 * 60 * 60 * 1000; // 3 dager

    private final String userid;
    private final String password;

    public ChangePasswordToken(String userid, String password) {
        this.userid = userid;
        this.password = password;
    }

    public static ChangePasswordToken fromTokenString(String outerTokenstring, byte[] salt) {
        try {
            String outerToken = Base64.base64Decode(outerTokenstring);
            String[] outerTokenelems = outerToken.split(":");
            String user1 = outerTokenelems[0];
            String timeout1 = outerTokenelems[1];
            String encodedInnerToken = Base64.base64Decode(outerTokenelems[2]);
            byte[] innerTokenbytes = null;
			try {
				innerTokenbytes = encodedInnerToken.getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}         
            
            for (int i = 0; i < innerTokenbytes.length; i++) {
                innerTokenbytes[i] = (byte) (innerTokenbytes[i] ^ salt[i % salt.length]);
            }
            String decodedInnerToken = new String(Base64.decode(innerTokenbytes));
            String[] innerTokenElems = decodedInnerToken.split(":");
            String user2 = innerTokenElems[0];
            String timeout2 = innerTokenElems[2];
            String password = innerTokenElems[1];
            if (!user1.equals(user2) || !timeout1.equals(timeout2)) {
                throw new IllegalArgumentException("Invalid token");
            }
            if (Long.parseLong(timeout1) < System.currentTimeMillis()) {
                throw new IllegalArgumentException("Token has timed out");
            }
            return new ChangePasswordToken(user2, password);
        } catch (ArrayIndexOutOfBoundsException re) {
            throw new IllegalArgumentException("Invalid token");
        }
    }

    public String generateTokenString(byte[] salt) {
        long timestamp = System.currentTimeMillis() + lifetime;
        String innerToken = userid + ":" + password + ":" + timestamp;
        byte[] innerTokenBytes = Base64.encode(innerToken);
        for (int i = 0; i < innerTokenBytes.length; i++) {
            innerTokenBytes[i] = (byte) (innerTokenBytes[i] ^ salt[i % salt.length]);
        }
        String outerToken = userid + ":" + timestamp + ":" + new String(Base64.encode(innerTokenBytes));
        return new String(Base64.encode(outerToken));
    }

    public String getUserid() {
        return userid;
    }

    public String getPassword() {
        return password;
    }
}
