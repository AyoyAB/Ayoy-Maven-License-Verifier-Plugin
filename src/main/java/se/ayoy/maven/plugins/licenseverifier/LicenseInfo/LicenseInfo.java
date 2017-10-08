package se.ayoy.maven.plugins.licenseverifier.LicenseInfo;

import java.util.ArrayList;

public class LicenseInfo {
    private String name;

    private ArrayList<String> configuredNames;

    private ArrayList<String> configuredUrls;

    private LicenseInfoStatusEnum licenseInfoStatus;

    boolean hasLicenceInfo(String name, String url) {
        for (String configuredName : this.configuredNames) {
            if (configuredName == null) {
                continue;
            }

            if (configuredName.equals(name)) {
                return true;
            }
        }

        for (String configuredUrl : this.configuredUrls) {
            if (configuredUrl == null) {
                continue;
            }

            if (configuredUrl.equals(url)) {
                return true;
            }
        }

        return false;
    }

    String getName() {
        return this.name;
    }
}
