package se.ayoy.maven.plugins.licenseverifier;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.License;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.io.File.separator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LicenseVerifierMojoTest {

    @Mock
    private Log log;

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

    private Set<Artifact> artifacts = new HashSet<>();

    private List<License> licenses = new ArrayList<>();

    @Before
    public void before() throws Exception {
        assertNotNull("Failed to mock projectBuildingResult", projectBuildingResult);

        this.artifacts.clear();

        when(this.session.getProjectBuildingRequest()).thenReturn(this.projectBuildingRequest);
        when(this.project.getDependencyArtifacts()).thenReturn(artifacts);
        when(projectBuilder.build(any(Artifact.class), any(ProjectBuildingRequest.class)))
                .thenReturn(this.projectBuildingResult);
        when(this.projectBuildingResult.getProject()).thenReturn(this.project);

        when(this.project.getLicenses()).thenReturn(licenses);

        licenseVerifierMojo.setLog(log);
    }

    @Test
    public void missingFile() throws Exception {

        licenseVerifierMojo.setLicenseFile("thisFileDoesntExist.xml");

        // Act
        try {
            licenseVerifierMojo.execute();
            fail();
        } catch (org.apache.maven.plugin.MojoExecutionException exc) {
            String message = exc.getMessage();
            // Verify
            assertTrue(message.startsWith("File \"thisFileDoesntExist.xml\" (expanded to \""));
            assertTrue(message.endsWith("Ayoy-Maven-License-Verifier-Plugin"
                    + separator + "thisFileDoesntExist.xml\") could not be found."));
        }
    }

    @Test
    public void handleOneValidLicense() throws Exception {
        this.artifacts.add(this.artifact);

        License license = new License();
        license.setName("The Apache Software License, Version 2.0");
        license.setUrl("http://www.apache.org/licenses/LICENSE-2.0.txt");
        licenses.add(license);

        licenseVerifierMojo.setLicenseFile(getFilePath("LicenseVerifierMojoTest-OneValid.xml"));

        // Act
        licenseVerifierMojo.execute();

        // Verify
    }

    @Test
    public void missingLicense() throws Exception {
        this.artifacts.add(this.artifact);

        licenseVerifierMojo.setLicenseFile(getFilePath("LicenseVerifierMojoTest-OneValid.xml"));

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
    public void missingExcludedLicenseFile() throws Exception {
        this.artifacts.add(this.artifact);

        licenseVerifierMojo.setLicenseFile(getFilePath("LicenseVerifierMojoTest-OneValid.xml"));
        licenseVerifierMojo.setExcludedMissingLicensesFile("thisFileDoesntExist.xml");

        // Act
        try {
            licenseVerifierMojo.execute();

            // Verify
            fail(); // Not implemented. Should throw exception.
        } catch (org.apache.maven.plugin.MojoExecutionException exc) {
            String message = exc.getMessage();
            // Verify
            assertTrue(message.startsWith("File \"thisFileDoesntExist.xml\" (expanded to \""));
            assertTrue(message.endsWith("Ayoy-Maven-License-Verifier-Plugin"
                    + separator + "thisFileDoesntExist.xml\") could not be found."));
        }
    }

    @Test
    public void missingLicenseButExcluded() throws Exception {
        this.artifacts.add(this.artifact);

        licenseVerifierMojo.setLicenseFile(getFilePath("LicenseVerifierMojoTest-OneValid.xml"));
        licenseVerifierMojo.setExcludedMissingLicensesFile(getFilePath("ExcludedMissingLicense.xml"));

        // Act
        licenseVerifierMojo.execute();
    }

    @Test
    public void unknownLicense() throws Exception {
        this.artifacts.add(this.artifact);

        License license = new License();
        license.setName("This is some strange license");
        license.setUrl("http://www.ayoy.se/licenses/SUPERSTRANGE.txt");
        licenses.add(license);
        licenseVerifierMojo.setLicenseFile(getFilePath("LicenseVerifierMojoTest-OneValid.xml"));

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
        licenseVerifierMojo.setLicenseFile(getFilePath("LicenseVerifierMojoTest-OneValid.xml"));

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
        licenseVerifierMojo.setLicenseFile(getFilePath("LicenseVerifierMojoTest-OneValid.xml"));

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
        licenseVerifierMojo.setLicenseFile(getFilePath("LicenseVerifierMojoTest-OneValid.xml"));

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
        license.setName("The Apache Software License, Version 2.0");
        license.setUrl("http://www.apache.org/licenses/LICENSE-2.0.txt");
        licenses.add(license);
        licenseVerifierMojo.setLicenseFile(getFilePath("LicenseVerifierMojoTest-OneValid.xml"));

        licenseVerifierMojo.setRequireAllValid("false");

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
