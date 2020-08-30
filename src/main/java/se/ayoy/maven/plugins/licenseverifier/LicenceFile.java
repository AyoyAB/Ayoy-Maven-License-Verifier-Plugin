package se.ayoy.maven.plugins.licenseverifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Abstract file used for common methods by LicenceInfoFile and ExcludedMissingLicenseFile.
 */
public abstract class LicenceFile {

    /**
     * Either reads a file or reads a resource from a package.
     * @param file           The file to read.
     * @param filePathString The path to read.
     * @return an input stream to read the configuration file.
     * @throws FileNotFoundException if the file could not be found.
     */
    protected InputStream getInputStreamFromFileOrResource(File file, String filePathString)
            throws FileNotFoundException {

        if (!file.exists()) {
            // lets try to get it as resource
            URL url = LicenseVerifierMojo.class.getResource(filePathString);
            if (url == null) {
                throw new FileNotFoundException(filePathString);
            }
            try {
                return url.openStream();
            } catch (IOException ex) {
                throw new FileNotFoundException(filePathString);
            }
        } else {
            return new FileInputStream(file);
        }
    }
}
