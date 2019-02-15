package se.ayoy.maven.plugins.licenseverifier;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.License;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.repository.RepositorySystem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import se.ayoy.maven.plugins.licenseverifier.model.AyoyArtifact;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.io.File.separator;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.powermock.reflect.Whitebox.invokeMethod;
import static org.powermock.reflect.Whitebox.setInternalState;

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

    @Mock
    private RepositorySystem repositorySystem;

    @Mock
    private ArtifactResolutionResult resolutionResult;

    @InjectMocks
    private LicenseVerifierMojo licenseVerifierMojo;

    private Set<Artifact> artifacts = new HashSet<>();

    private List<License> licenses = new ArrayList<>();

    @Before
    public void before() throws Exception {
        assertNotNull("Failed to mock projectBuildingResult", projectBuildingResult);

        when(this.session.getProjectBuildingRequest()).thenReturn(this.projectBuildingRequest);
        when(this.project.getDependencyArtifacts()).thenReturn(artifacts);
        when(projectBuilder.build(any(Artifact.class), any(ProjectBuildingRequest.class)))
                .thenReturn(this.projectBuildingResult);
        when(this.projectBuildingResult.getProject()).thenReturn(this.project);

        when(this.project.getLicenses()).thenReturn(licenses);

        transitiveArtifact3.setOptional(true);
        when(repositorySystem.resolve(Mockito.any(ArtifactResolutionRequest.class))).thenAnswer(new Answer<ArtifactResolutionResult>() {
            @Override
            public ArtifactResolutionResult answer(InvocationOnMock invocationOnMock) throws Throwable {
                ArtifactResolutionRequest arg1 = (ArtifactResolutionRequest)invocationOnMock.getArguments()[0];
                Artifact queryArtifact = arg1.getArtifact();

                ArtifactResolutionResult toReturn = new ArtifactResolutionResult();
                toReturn.getArtifacts().add(queryArtifact);
                if (queryArtifact.equals(artifact)) {
                    toReturn.getArtifacts().add(transitiveArtifact1);
                    toReturn.getArtifacts().add(transitiveArtifact2);
                    toReturn.getArtifacts().add(transitiveArtifact3);
                } else if (queryArtifact.equals(transitiveArtifact1)
                        || queryArtifact.equals(transitiveArtifact2)
                        || queryArtifact.equals(transitiveArtifact3)) {
                    // Return empty
                } else {
                    // Return empty
                }

                return toReturn;
            }
        });
        when(resolutionResult.getArtifacts()).thenCallRealMethod();

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

    @Test
    public void bothUnknownAndValidLicense1() throws Exception {
        this.artifacts.add(this.artifact);

        License license = new License();
        license.setName("The unknown License");
        license.setUrl("http://www.ayoy.org/licenses/UNKNOWN");
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

    @Test
    public void bothUnknownAndValidLicense2() throws Exception {
        this.artifacts.add(this.artifact);

        License license = new License();
        license.setName("The unknown License");
        license.setUrl("http://www.ayoy.org/licenses/UNKNOWN");
        licenses.add(license);
        license = new License();
        license.setName("The Apache Software License, Version 2.0");
        license.setUrl("http://www.apache.org/licenses/LICENSE-2.0.txt");
        licenses.add(license);
        licenseVerifierMojo.setLicenseFile(getFilePath("LicenseVerifierMojoTest-OneValid.xml"));

        licenseVerifierMojo.setRequireAllValid("true");

        // Act
        try {
            licenseVerifierMojo.execute();

            fail();
        } catch(MojoExecutionException exc) {
            assertEquals("One or more artifacts has licenses which is unclassified.", exc.getMessage());
        }
    }

    @Test
    public void shouldResolveOnlyTransitiveDependencies() throws Exception {
        Set<Artifact> artifactsSet = new HashSet<Artifact>();
        artifactsSet.add(artifact);

        List<AyoyArtifact> trans = invokeMethod(
                licenseVerifierMojo,
                "resolveArtifacts",
                artifactsSet,
                projectBuildingRequest,
                null,
                null);

        assertThat(trans.size(), is(3));
        List<Artifact> resultArtifactList = trans
                .stream()
                .map(ayoyArtifact -> ayoyArtifact.getArtifact())
                .collect(Collectors.toList());

        assertThat(resultArtifactList, hasItem(artifact));
        assertThat(resultArtifactList, hasItem(transitiveArtifact1));
        assertThat(resultArtifactList, hasItem(transitiveArtifact2));
    }

    @Test
    public void shouldResolveOnlyCompiletimeTransitiveDependencies() throws Exception {
        Set<Artifact> artifactsSet = new HashSet<Artifact>();
        artifactsSet.add(artifact);

        setInternalState(licenseVerifierMojo, "excludedScopes", new String[]{"runtime"});
        List<AyoyArtifact> trans = invokeMethod(
                licenseVerifierMojo,
                "resolveArtifacts",
                artifactsSet,
                projectBuildingRequest,
                null,
                null);

        assertThat(trans.size(), is(2));
        List<Artifact> resultArtifactList = trans
                .stream()
                .map(ayoyArtifact -> ayoyArtifact.getArtifact())
                .collect(Collectors.toList());

        assertThat(resultArtifactList, hasItem(artifact));
        assertThat(resultArtifactList, hasItem(transitiveArtifact1));
    }

    private Artifact artifact = new DefaultArtifact(
            "groupId",
            "artifactId",
            "1.0.0",
            "compile",
            "type",
            "classifier",
            null);

    private Artifact transitiveArtifact1 = new DefaultArtifact(
            "groupId.transitive1",
            "artifactId-transitive1",
            "1.0.0",
            "compile",
            "jar",
            "",
            null);

    private Artifact transitiveArtifact2 = new DefaultArtifact(
            "groupId.transitive2",
            "artifactId-transitive2",
            "1.0.0",
            "runtime",
            "jar",
            "",
            null);

    private Artifact transitiveArtifact3 = new DefaultArtifact(
            "groupId.transitive2",
            "artifactId-transitive2",
            "1.0.0",
            "runtime",
            "jar",
            "",
            null);

    private String getFilePath(String filename) {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(filename).getFile());

        return file.getAbsolutePath();
    }
}
