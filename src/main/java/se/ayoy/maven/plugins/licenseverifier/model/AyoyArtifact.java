package se.ayoy.maven.plugins.licenseverifier.model;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.License;
import se.ayoy.maven.plugins.licenseverifier.LicenseInfo.LicenseInfo;
import se.ayoy.maven.plugins.licenseverifier.LicenseInfo.LicenseInfoStatusEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a found artifact, including license information.
 */
public class AyoyArtifact {

    private final Artifact artifact;

    private final AyoyArtifact parent;

    private final ArrayList<License> licenses;

    private final ArrayList<LicenseInfo> licenseInfos;

    /**
     * Initialize the instance.
     * @param artifact the maven artifact found.
     * @param parent   the parent artifact.
     */
    public AyoyArtifact(Artifact artifact, AyoyArtifact parent) {
        this.artifact = artifact;
        this.parent = parent;
        this.licenses = new ArrayList<License>();
        this.licenseInfos = new ArrayList<LicenseInfo>();
    }

    /**
     * Add licenses to the artifact.
     * @param licenses the licenses to add.
     */
    public void addLicenses(List<License> licenses) {
        if (licenses == null) {
            return;
        }

        this.licenses.addAll(licenses);
    }

    public List<License> getLicenses() {
        return new ArrayList<License>(this.licenses);
    }

    public AyoyArtifact getParent() {
        return this.parent;
    }

    public Artifact getArtifact() {
        return this.artifact;
    }

    @Override
    public String toString() {
        StringBuilder toReturn = new StringBuilder();

        toReturn.append("artifact ");
        toReturn.append(this.artifact);

        if (this.licenses.size() > 0) {
            toReturn.append(" with licenses: ");

            boolean firstLic = true;
            for (License license : this.licenses) {
                if (!firstLic) {
                    toReturn.append(", ");
                }

                firstLic = false;
                toReturn.append("\"");
                toReturn.append(license.getName());
                toReturn.append("\"");
            }
        }

        return toReturn.toString();
    }

    /**
     * Creates a string representing the dependency chain to this artifact.
     * @return a string representing the dependency chain to this artifact.
     */
    public String getChainString() {
        StringBuilder toReturn = new StringBuilder();

        if (this.parent != null) {
            toReturn.append("pom");

            AyoyArtifact activeParent = this.parent;
            while (activeParent != null) {
                toReturn.append(" -> ");
                toReturn.append(activeParent.artifact.getGroupId());
                toReturn.append(":");
                toReturn.append(activeParent.artifact.getArtifactId());

                activeParent = activeParent.getParent();
            }

            toReturn.append(" -> ");
            toReturn.append(artifact.getGroupId());
            toReturn.append(":");
            toReturn.append(artifact.getArtifactId());
        }

        return toReturn.toString();
    }

    /**
     * Adds a license info about the licenses for the artifact.
     * @param info the license information.
     */
    public void addLicenseInfo(LicenseInfo info) {
        this.licenseInfos.add(info);
    }

    /**
     * Check if licenses are valid.
     * @param requireAllValid if all licenses are required to be valid
     * @return true if valid.
     */
    public boolean isLicenseValid(boolean requireAllValid) {
        if (this.licenseInfos.size() == 0) {
            return false;
        }

        boolean anyValid = this.licenseInfos.stream().anyMatch(x -> x.getStatus() == LicenseInfoStatusEnum.VALID);

        // If not even one valid license is found, return false;
        if (!anyValid) {
            return false;
        }

        // If we don't require all licenses to be valid, return true as at least one is valid.
        if (!requireAllValid) {
            return true;
        }

        // Return false if a license with any other status than VALID is found.
        return !this.licenseInfos.stream().anyMatch(x -> x.getStatus() != LicenseInfoStatusEnum.VALID);
    }

    public List<LicenseInfo> getLicenseInfos() {
        return new ArrayList<LicenseInfo>(this.licenseInfos);
    }
}
