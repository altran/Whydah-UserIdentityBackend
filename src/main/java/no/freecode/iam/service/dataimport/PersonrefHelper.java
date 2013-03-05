package no.freecode.iam.service.dataimport;

import java.io.UnsupportedEncodingException;
import java.util.Random;

/**
 * Obfuskerer personnummer for oppslag mot person.
 */
public final class PersonrefHelper {
    private static final Random random = new Random();

    public static String createPersonref(String personnummer) {
       
		//byte[] inbytes = personnummer.getBytes(); 
    	byte[] inbytes = null;
		try {
			inbytes = personnummer.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    			
    			
        byte[] outbytes = new byte[15];
        int shift = random.nextInt(7) + 2;
        for (int i=0; i<inbytes.length; i++) {
            byte b = (byte)(((inbytes[i] - 48 + shift ) % 10) + 48);
          outbytes[i+2] = b;
        }
        outbytes[0] = (byte)(random.nextInt(10) + 48);
        outbytes[1] = (byte)(shift/2 + 48);
        outbytes[13] = (byte)(shift%2 + 48);
        outbytes[14] = (byte)(random.nextInt(10) + 48);
        return new String(outbytes);
    }

    public static String decodePersonref(String personref) {
        byte[] inbytes = personref.getBytes();
        byte[] outbytes = new byte[11];
        int shift = (inbytes[1] - 48) * 2 + inbytes[13]- 48;
        for (int i=2; i<13; i++) {
          outbytes[i-2] = (byte)(((inbytes[i] + (10-shift) - 48) % 10) + 48);
        }
        return new String(outbytes);
    }

    private PersonrefHelper() {
    }
}