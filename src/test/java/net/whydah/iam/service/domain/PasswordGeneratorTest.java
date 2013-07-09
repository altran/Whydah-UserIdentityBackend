package net.whydah.iam.service.domain;

import net.whydah.iam.service.domain.PasswordGenerator;

import org.junit.Test;

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
}
