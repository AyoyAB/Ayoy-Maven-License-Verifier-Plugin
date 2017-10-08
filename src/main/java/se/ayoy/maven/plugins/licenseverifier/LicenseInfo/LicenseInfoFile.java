package se.ayoy.maven.plugins.licenseverifier.LicenseInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class LicenseInfoFile {

    private ArrayList<LicenseInfo> licenseInfos = new ArrayList<LicenseInfo>();

    public LicenseInfoFile(String filePathString)
            throws FileNotFoundException {
        if (filePathString == null) {
            throw new IllegalArgumentException("The path cannot be null");
        }

        File file = new File(filePathString);
        if (!file.exists()) {
            throw new FileNotFoundException(filePathString);
        }
    }

    public LicenseInfo getLicenseInfo(String name, String url) {
        for (LicenseInfo info : this.licenseInfos) {
            if (info.hasLicenceInfo(name, url)) {
                return info;
            }
        }

        return null;
    }
}
