package se.ayoy.maven.plugins.licenseverifier.util;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import se.ayoy.maven.plugins.licenseverifier.model.AyoyArtifact;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * Tests the AyoyArtifactList class.
 */
@RunWith(MockitoJUnitRunner.class)
public class AyoyArtifactListTest {

    private final Artifact artifact = new DefaultArtifact(
            "groupId",
            "artifactId",
            "1.0.0",
            "compile",
            "type",
            "classifier",
            null);

    @Test
    public void checkAlreadyAddedArtifact() {
        AyoyArtifactList list = new AyoyArtifactList();

        Artifact artifact = new DefaultArtifact(
                "groupId",
                "artifactId",
                "1.0.0",
                "compile",
                "type",
                "classifier",
                null);

        AyoyArtifact ayoyArtifact = new AyoyArtifact(artifact, null);

        // Act
        list.add(ayoyArtifact);

        assertTrue(list.containsArtifact(artifact));
    }

    @Test
    public void checkAlreadyAddedAyoyArtifact() {
        AyoyArtifactList list = new AyoyArtifactList();

        AyoyArtifact ayoyArtifact = new AyoyArtifact(artifact, null);

        // Act
        list.add(ayoyArtifact);
        assertEquals(1, list.size());
        list.add(ayoyArtifact);
        assertEquals(1, list.size());
    }
}
