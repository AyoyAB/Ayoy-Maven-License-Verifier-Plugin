package se.ayoy.maven.plugins.licenseverifier.util;

import org.apache.maven.artifact.Artifact;
import se.ayoy.maven.plugins.licenseverifier.model.AyoyArtifact;

import java.util.ArrayList;

/**
 * Represents a filtered list of AyoyArtifacts.
 */
public class AyoyArtifactList extends ArrayList<AyoyArtifact> {
    private static final long serialVersionUID = 1L;

    /**
     * Checks if there is already a AyoyArtifact which references this artifact.
     * @param artifact the artifact.
     * @return true if the artifact is already referenced.
     */
    public boolean containsArtifact(Artifact artifact) {
        for (AyoyArtifact ayoyArtifact: this) {
            if (ayoyArtifact.getArtifact().equals(artifact)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Adds an AyoyArtifact to the list, if it's not already in the list.
     * @param ayoyArtifact the artifact to add.
     * @return true if artifact was added to the list.
     */
    public boolean add(AyoyArtifact ayoyArtifact) {
        if (containsArtifact(ayoyArtifact.getArtifact())) {
            return false;
        }

        super.add(ayoyArtifact);
        return true;
    }
}
