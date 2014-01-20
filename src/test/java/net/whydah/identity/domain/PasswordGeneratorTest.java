package net.whydah.identity.domain;

import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class PasswordGeneratorTest {
    @Test
    public void testGeneratePasswords() {
        PasswordGenerator pwg = new PasswordGenerator();
        for(int i=0; i<100; i++) {
            String password = pwg.generate();
            assertNotNull(password);
            assertTrue(password.length() > 0);
        }

    }

    @Test
    public void testUTF8() {
        String myString = "\u0048\u0065\u006C\u006C\u006F World";
        myString="HallstrÃ¸m";
        System.out.println(myString);
        byte[] myBytes = null;

        try
        {
            myBytes = myString.getBytes("UTF-8");
            System.out.println("UTF8:"+ new String(myString.getBytes("ISO-8859-1"), "UTF-8"));
            System.out.println("ISO-8859-1:"+ new String(myString.getBytes("ISO-8859-1"), "ISO-8859-1"));
        } catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
            System.out.println("UTF8"+myBytes);
            System.exit(-1);
        }

        for (int i=0; i < myBytes.length; i++) {
            System.out.println(myBytes[i]);
        }
    }
}
