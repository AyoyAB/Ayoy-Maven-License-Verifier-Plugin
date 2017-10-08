package se.ayoy.maven.plugins.licenseverifier;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import se.ayoy.maven.plugins.licenseverifier.LicenseInfo.LicenseInfoFile;
import se.ayoy.maven.plugins.licenseverifier.model.AyoyArtifact;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

abstract class LicenseAbstractMojo extends AbstractMojo {
    @Component
    private final MavenProject project = null;

    @Component
    private final ProjectBuilder projectBuilder = null;

    /**
     * If the plugin should be verbose.
     */
    @Parameter(property = "verbose", defaultValue = "false")
    private String verbose;

    /**
     * A list of scopes to exclude. May be used to exclude artifacts with test or provided scope from license check.
     * Example: &lt;configuration&gt; &lt;excludedScopes&gt; &lt;param&gt;test&lt;/param&gt;
     * &lt;param&gt;provided&lt;/param&gt; &lt;/excludedScopes&gt; &lt;/configuration&gt;
     */
    @Parameter(property = "excludedScopes")
    private String[] excludedScopes;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    List<AyoyArtifact> parseArtifacts() {
        ArrayList<AyoyArtifact> toReturn = new ArrayList<AyoyArtifact>();

        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());

        final Set<Artifact> artifacts = project.getDependencyArtifacts();
        for (final Artifact artifact : artifacts) {
            boolean isExcludedScope = false;
            for(String excludedScope : this.excludedScopes) {
                if (excludedScope.equals(artifact.getScope())) {
                    isExcludedScope = true;
                }
            }

            if (isExcludedScope) {
                getLog().info("Artifact is excluded from scope \""
                        + artifact.getScope()
                        + "\": "
                        + artifact.getGroupId()
                        + ":"
                        + artifact.getArtifactId());
                continue;
            }

            AyoyArtifact licenseInfo = new AyoyArtifact(artifact);

            getLog().debug("Getting license for " + artifact.toString());
            try {
                buildingRequest.setProject(null);

                MavenProject mavenProject = projectBuilder.build(artifact, buildingRequest).getProject();
                licenseInfo.addLicenses(mavenProject.getLicenses());

                toReturn.add(licenseInfo);
            } catch (ProjectBuildingException e) {
                getLog().error(e.getMessage());
            }
        }

        return toReturn;
    }

    LicenseInfoFile getLicenseInfoFile(String licenseFile) throws MojoExecutionException {

        try {
            LicenseInfoFile file = new LicenseInfoFile(licenseFile, this.getLog());
            return file;
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("File "
                    + licenseFile
                    + " could not be found.",
                    e);
        }
    }

    boolean verboseBool = false;
    void logInfoIfVerbose(String message) {
        if (Boolean.parseBoolean(this.verbose)) {
            this.getLog().info(message);
        }
    }
}
