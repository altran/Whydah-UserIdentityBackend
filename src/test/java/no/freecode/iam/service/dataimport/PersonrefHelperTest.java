package no.freecode.iam.service.dataimport;

import junit.framework.TestCase;

import java.util.Random;

public class PersonrefHelperTest extends TestCase{
    Random rnd = new Random(System.currentTimeMillis());
    public void testGenerateAndDecode() {
        for(int i=0; i<100000; i++) {
            String personnummer = generateString();
            String ref = PersonrefHelper.createPersonref(personnummer);
            assertEquals(15, ref.length());
            String decodedRef = PersonrefHelper.decodePersonref(ref);
            assertEquals(personnummer, decodedRef);
        }
    }

    private String generateString() {
        byte[] bytes = new byte[11];
        for(int i=0; i<bytes.length; i++) {
            bytes[i] = (byte)(rnd.nextInt(10) + '0');
        }

        return new String(bytes);
    }
}
