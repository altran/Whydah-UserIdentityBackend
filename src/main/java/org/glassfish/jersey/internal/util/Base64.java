package org.glassfish.jersey.internal.util;

import java.io.UnsupportedEncodingException;


public class Base64 {
    private static final byte[] CHAR_SET;
    private static final byte[] BASE64INDEXES;

    public Base64() {
    }

    public static byte[] encode(byte[] buffer) {
        int ccount = buffer.length / 3;
        int rest = buffer.length % 3;
        byte[] result = new byte[(ccount + (rest > 0 ? 1 : 0)) * 4];

        int temp;
        for (temp = 0; temp < ccount; ++temp) {
            result[temp * 4] = CHAR_SET[buffer[temp * 3] >> 2 & 255];
            result[temp * 4 + 1] = CHAR_SET[((buffer[temp * 3] & 3) << 4 | buffer[temp * 3 + 1] >> 4) & 255];
            result[temp * 4 + 2] = CHAR_SET[((buffer[temp * 3 + 1] & 15) << 2 | buffer[temp * 3 + 2] >> 6) & 255];
            result[temp * 4 + 3] = CHAR_SET[buffer[temp * 3 + 2] & 63];
        }

        temp = 0;
        if (rest > 0) {
            if (rest == 2) {
                result[ccount * 4 + 2] = CHAR_SET[(buffer[ccount * 3 + 1] & 15) << 2 & 255];
                temp = buffer[ccount * 3 + 1] >> 4;
            } else {
                result[ccount * 4 + 2] = CHAR_SET[CHAR_SET.length - 1];
            }

            result[ccount * 4 + 3] = CHAR_SET[CHAR_SET.length - 1];
            result[ccount * 4 + 1] = CHAR_SET[((buffer[ccount * 3] & 3) << 4 | temp) & 255];
            result[ccount * 4] = CHAR_SET[buffer[ccount * 3] >> 2 & 255];
        }

        return result;
    }

    public static byte[] decode(byte[] buffer) {
        if (buffer.length < 4 && buffer.length % 4 != 0) {
            return new byte[0];
        } else {
            int ccount = buffer.length / 4;
            int paddingCount = (buffer[buffer.length - 1] == 61 ? 1 : 0) + (buffer[buffer.length - 2] == 61 ? 1 : 0);
            byte[] result = new byte[3 * (ccount - 1) + (3 - paddingCount)];

            int i;
            for (i = 0; i < ccount - 1; ++i) {
                result[i * 3] = (byte) (BASE64INDEXES[buffer[i * 4]] << 2 | BASE64INDEXES[buffer[i * 4 + 1]] >> 4);
                result[i * 3 + 1] = (byte) (BASE64INDEXES[buffer[i * 4 + 1]] << 4 | BASE64INDEXES[buffer[i * 4 + 2]] >> 2);
                result[i * 3 + 2] = (byte) (BASE64INDEXES[buffer[i * 4 + 2]] << 6 | BASE64INDEXES[buffer[i * 4 + 3]]);
            }

            i = ccount - 1;
            switch (paddingCount) {
                case 0:
                    result[i * 3 + 2] = (byte) (BASE64INDEXES[buffer[i * 4 + 2]] << 6 | BASE64INDEXES[buffer[i * 4 + 3]]);
                    result[i * 3 + 1] = (byte) (BASE64INDEXES[buffer[i * 4 + 1]] << 4 | BASE64INDEXES[buffer[i * 4 + 2]] >> 2);
                    result[i * 3] = (byte) (BASE64INDEXES[buffer[i * 4]] << 2 | BASE64INDEXES[buffer[i * 4 + 1]] >> 4);
                    break;
                case 1:
                    result[i * 3 + 1] = (byte) (BASE64INDEXES[buffer[i * 4 + 1]] << 4 | BASE64INDEXES[buffer[i * 4 + 2]] >> 2);
                    result[i * 3] = (byte) (BASE64INDEXES[buffer[i * 4]] << 2 | BASE64INDEXES[buffer[i * 4 + 1]] >> 4);
                    break;
                case 2:
                    result[i * 3] = (byte) (BASE64INDEXES[buffer[i * 4]] << 2 | BASE64INDEXES[buffer[i * 4 + 1]] >> 4);
            }

            return result;
        }
    }

    public static String encodeAsString(byte[] buffer) {
        byte[] result = encode(buffer);

        try {
            return new String(result, "ASCII");
        } catch (UnsupportedEncodingException var3) {
            return new String(result);
        }
    }

    public static String encodeAsString(String text) {
        return encodeAsString(text.getBytes());
    }

    public static String decodeAsString(byte[] buffer) {
        byte[] result = decode(buffer);

        try {
            return new String(result, "ASCII");
        } catch (UnsupportedEncodingException var3) {
            return new String(result);
        }
    }

    public static String decodeAsString(String text) {
        return decodeAsString(text.getBytes());
    }

    static {
        String var0 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

        byte[] cs;
        try {
            cs = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=".getBytes("ASCII");
        } catch (UnsupportedEncodingException var3) {
            cs = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=".getBytes();
        }

        CHAR_SET = cs;
        BASE64INDEXES = new byte[]{64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 62, 64, 64, 64, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 64, 64, 64, 64, 64, 64, 64, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 64, 64, 64, 64, 64, 64, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64};
    }
}
