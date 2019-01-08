package se.ayoy.maven.plugins.licenseverifier.model;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.License;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a found artifact, including license information.
 */
public class AyoyArtifact {

    private final Artifact artifact;

    private final ArrayList<License> licenses;

    /**
     * Initialize the instance.
     * @param artifact the maven artefact found.
     */
    public AyoyArtifact(Artifact artifact) {
        this.artifact = artifact;
        this.licenses = new ArrayList<License>();
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
}
