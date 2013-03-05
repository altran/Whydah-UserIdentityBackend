package no.freecode.iam.service;

import junit.framework.TestCase;
import no.freecode.iam.service.prestyr.PstyrImporterTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import static org.junit.Assert.*;
public class MainTest extends TestCase {


    @BeforeClass
    public static void init() {
    	 PstyrImporterTest.deleteDirectory(new File("/tmp/ssotest/"));
    }

    @AfterClass
    public static void cleanup() {
    	PstyrImporterTest.deleteDirectory(new File("/tmp/ssotest/"));
    }
   

    @Test
    public void testexitIfEnvironmentNotSpecified() throws Exception{
        ProcessBuilder pb = new ProcessBuilder("java", "-cp", System.getProperty("java.class.path"), "no.freecode.iam.service.Main");
        pb.environment().remove("IAM_MODE");
        Process p = pb.start();
        assertTrue(p.waitFor() != 0);
    }

    @Test
    public void testexitIfUnknownEnvironmentSpecified() throws Exception{
        ProcessBuilder pb = new ProcessBuilder("java", "-cp", System.getProperty("java.class.path"), "no.freecode.iam.service.Main");
        pb.environment().put("IAM_MODE", "NUDLER");
        Process p = pb.start();
        assertTrue(p.waitFor() != 0);
    }

    @Test
    public void dontExitIfGoodEnvironmentSpecified() throws Exception {
        Process p = null;
        Logger logger = LoggerFactory.getLogger(getClass());
        try {
            ProcessBuilder pb = new ProcessBuilder("java", "-cp", System.getProperty("java.class.path"), "no.freecode.iam.service.Main");
            pb.environment().put("IAM_MODE", "JUNIT");
            p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while((line=br.readLine()) != null) {
                logger.debug(line);
                if(line.contains("Jersey webapp started")) {
                    p.destroy();
                    break;
                }
            }
        } finally {
            if(p != null) {
                p.destroy();
            }
        }
    }
}