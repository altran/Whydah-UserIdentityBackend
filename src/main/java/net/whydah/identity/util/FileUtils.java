package net.whydah.identity.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

public class FileUtils {
    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    public static void deleteDirectories(String... paths) {
        String directoriesAsString = String.join(",", paths);
        log.info("Deleting directories {}", directoriesAsString);
        for (String path : paths) {
            deleteDirectory(path);
        }
    }

    public static void deleteDirectory(String pathAsString) {
        if (pathAsString != null) {
            deleteDirectory(new File(pathAsString));
        }
    }

    public static void deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        if(!file.delete()) {
                            log.warn("Unable to delete file " + file);
                        }
                    }
                }
            }
        }
        boolean exist = path.exists();
        boolean deleted = path.delete();
        if (exist) {
            if (!deleted) {
                log.warn("Unable to delete directory " + path);
            } else {
                log.debug("Folder {} was deleted successfully.", path.getAbsolutePath());
            }
        }
    }


    public static void createDirectory(String pathAsString) {
        File dir = new File(pathAsString);
        if (!dir.exists()) {
            boolean dirsCreated = dir.mkdirs();
            if (!dirsCreated) {
                log.debug("{} was not successfully created.", dir.getAbsolutePath());
            }
        }
    }


    public static void close(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                log.trace("Exception closing InputStream. Doing nothing.", e);
            }
        }
    }

    public static boolean localFileExist(String fileName) {
        File file = new File(fileName);
        return file.exists();
    }

    public static InputStream openLocalFile(String fileName) {
        File file = new File(fileName);
        FileInputStream fis = null;
        try {
            if (file.exists()) {
                fis = new FileInputStream(file);
            } else {
                throw new RuntimeException("Config file " + fileName + " does not exist.");
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Config file " + fileName +  " not found.", e);
        } catch (Exception e) {
            throw new RuntimeException("Error reading " + fileName , e);
        }
        return fis;

    }

    public static InputStream openFileOnClasspath(String fileName) {
        InputStream is = FileUtils.class.getClassLoader().getResourceAsStream(fileName);
        if(is == null) {
            throw new RuntimeException("Error reading " + fileName + " from classpath.");
        }
        return is;
    }

    public static void copyFiles(String[] files, String targetDirectory, boolean overwrite) {
        log.info("Copying files " + Arrays.toString(files) + " to " + targetDirectory, ", overwrite=" + overwrite);
        if (overwrite) {
            FileUtils.deleteDirectory(targetDirectory);
        }

        FileUtils.createDirectory(targetDirectory);

        for (String file : files) {
            try (InputStream in = FileUtils.openFileOnClasspath(file)) {
                if ( in != null) {
                    Path outputFile = Paths.get(targetDirectory).resolve(Paths.get(file));
                    boolean fileExists = Files.exists(outputFile);
                    if (!fileExists || overwrite) {
                        Files.copy(in, outputFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                } else {
                    log.warn("Could not copy file, inputstream is null. " + file);
                }

            } catch (IOException e) {
                log.warn("Could not copy file " + file, e);
            }
        }
    }
}
