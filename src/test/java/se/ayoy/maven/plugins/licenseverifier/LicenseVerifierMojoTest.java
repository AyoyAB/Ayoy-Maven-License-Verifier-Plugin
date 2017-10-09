package se.ayoy.maven.plugins.licenseverifier;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.License;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import se.ayoy.maven.plugins.licenseverifier.LicenseInfo.LicenseInfoFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LicenseVerifierMojoTest {

    @Mock
    private LicenseInfoFile licenseInfoFile;

    @Mock
    private MavenProject project;

    @Mock
    private ProjectBuilder projectBuilder;

    @Mock
    private MavenSession session;

    @Mock
    private ProjectBuildingRequest projectBuildingRequest;

    @Mock
    private ProjectBuildingResult projectBuildingResult;

    @InjectMocks
    private LicenseVerifierMojo licenseVerifierMojo;

    Set<Artifact> artifacts = new HashSet<Artifact>();

    List<License> licenses = new ArrayList<License>();

    @Before
    public void before() throws Exception {
        if (projectBuildingResult == null) {
            throw new NullPointerException("Failed to mock projectBuildingResult");
        }

        this.artifacts.clear();

        when(this.session.getProjectBuildingRequest()).thenReturn(this.projectBuildingRequest);
        when(this.project.getDependencyArtifacts()).thenReturn(artifacts);
        when(projectBuilder.build(any(Artifact.class), any(ProjectBuildingRequest.class)))
                .thenReturn(this.projectBuildingResult);
        when(this.projectBuildingResult.getProject()).thenReturn(this.project);

        when(this.project.getLicenses()).thenReturn(licenses);

        this.licenseVerifierMojo = new LicenseVerifierMojo(this.project, this.projectBuilder, this.session);
    }

    @Test
    public void missingFile() throws Exception {

        licenseVerifierMojo.licenseFile = "thisFileDoesntExist.xml";

        // Act
        try {
            licenseVerifierMojo.execute();
            fail();
        } catch (org.apache.maven.plugin.MojoExecutionException exc) {
            // Verify
            assertEquals("File thisFileDoesntExist.xml could not be found.", exc.getMessage());
        }
    }

    @Test
    public void handleOneValidLicense() throws Exception {
        this.artifacts.add(this.artifact);

        License license = new License();
        license.setName("The Apache Software License, Version 2.0");
        license.setUrl("http://www.apache.org/licenses/LICENSE-2.0.txt");
        licenses.add(license);

        licenseVerifierMojo.licenseFile = getFilePath("LicenseVerifierMojoTest-OneValid.xml");
        licenseVerifierMojo.verbose = "true";

        // Act
        licenseVerifierMojo.execute();

        // Verify
    }

    @Test
    public void missingLicense() throws Exception {
        this.artifacts.add(this.artifact);

        licenseVerifierMojo.licenseFile = getFilePath("LicenseVerifierMojoTest-OneValid.xml");

        // Act
        try {
            licenseVerifierMojo.execute();

            // Verify
            fail(); // Not implemented. Should throw exception.
        } catch (MojoExecutionException exc) {
            assertEquals("One or more artifacts is missing license information.", exc.getMessage());
        }
    }

    @Test
    public void unknownLicense() throws Exception {
        this.artifacts.add(this.artifact);

        License license = new License();
        license.setName("This is some strange license");
        license.setUrl("http://www.ayoy.se/licenses/SUPERSTRANGE.txt");
        licenses.add(license);
        licenseVerifierMojo.licenseFile = getFilePath("LicenseVerifierMojoTest-OneValid.xml");

        // Act
        try {
            licenseVerifierMojo.execute();

            // Verify
            fail(); // Not implemented. Should throw exception.
        } catch (MojoExecutionException exc) {
            assertEquals("One or more artifacts has licenses which is unclassified.", exc.getMessage());
        }
    }

    @Test
    public void forbiddenLicense() throws Exception {
        this.artifacts.add(this.artifact);

        License license = new License();
        license.setName("The Forbidden License");
        license.setUrl("http://www.ayoy.org/licenses/FORBIDDEN");
        licenses.add(license);
        licenseVerifierMojo.licenseFile = getFilePath("LicenseVerifierMojoTest-OneValid.xml");

        // Act
        try {
            licenseVerifierMojo.execute();

            // Verify
            fail(); // Not implemented. Should throw exception.
        } catch (MojoExecutionException exc) {
            assertEquals("One or more artifacts has licenses which is classified as forbidden.", exc.getMessage());
        }
    }

    @Test
    public void warningLicense() throws Exception {
        this.artifacts.add(this.artifact);

        License license = new License();
        license.setName("The Warning License");
        license.setUrl("http://www.ayoy.org/licenses/WARNING");
        licenses.add(license);
        licenseVerifierMojo.licenseFile = getFilePath("LicenseVerifierMojoTest-OneValid.xml");

        // Act
        try {
            licenseVerifierMojo.execute();

            // Verify
            fail(); // Not implemented. Should throw exception.
        } catch (MojoExecutionException exc) {
            assertEquals(
                    "One or more artifacts has licenses which is classified as warning.",
                    exc.getMessage());
        }
    }

    @Test
    public void bothInvalidAndValidLicense1() throws Exception {
        this.artifacts.add(this.artifact);

        License license = new License();
        license.setName("The Forbidden License");
        license.setUrl("http://www.ayoy.org/licenses/FORBIDDEN");
        licenses.add(license);
        license = new License();
        license.setName("The Apache Software License, Version 2.0");
        license.setUrl("http://www.apache.org/licenses/LICENSE-2.0.txt");
        licenses.add(license);
        licenseVerifierMojo.licenseFile = getFilePath("LicenseVerifierMojoTest-OneValid.xml");

        // Act
        try {
            licenseVerifierMojo.execute();

            // Verify
            fail(); // Not implemented. Should throw exception.
        } catch (MojoExecutionException exc) {
            assertEquals(
                    "One or more artifacts has licenses which is classified as forbidden.",
                    exc.getMessage());
        }
    }

    @Test
    public void bothInvalidAndValidLicense2() throws Exception {
        this.artifacts.add(this.artifact);

        License license = new License();
        license.setName("The Forbidden License");
        license.setUrl("http://www.ayoy.org/licenses/FORBIDDEN");
        licenses.add(license);
        license = new License();
        license.setName("The Forbidden License");
        license.setUrl("http://www.ayoy.org/licenses/FORBIDDEN");
        licenses.add(license);
        licenseVerifierMojo.licenseFile = getFilePath("LicenseVerifierMojoTest-OneValid.xml");

        licenseVerifierMojo.requireAllValid = "false";

        // Act
        licenseVerifierMojo.execute();
    }

    Artifact artifact = new DefaultArtifact(
            "groupId",
            "artifactId",
            "1.0.0",
            "compile",
            "type",
            "classifier",
            null);

    private String getFilePath(String filename) {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(filename).getFile());

        return file.getAbsolutePath();
    }
}
