package net.whydah.identity.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    public static void deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        if(!file.delete()) {
                            logger.warn("Unable to delete directory file " + file);
                        }
                    }
                }
            }
        }
        boolean exist = path.exists();
        boolean deleted = path.delete();
        if(exist && !deleted)  {
            logger.warn("Unable to delete directory " + path);
        }
    }
}
