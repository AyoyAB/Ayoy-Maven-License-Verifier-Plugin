package se.ayoy.maven.plugins.licenseverifier;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.License;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import se.ayoy.maven.plugins.licenseverifier.LicenseInfo.LicenseInfoFile;
import se.ayoy.maven.plugins.licenseverifier.MissingLicenseInfo.ExcludedMissingLicenseFile;
import se.ayoy.maven.plugins.licenseverifier.model.AyoyArtifact;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

abstract class LicenseAbstractMojo extends AbstractMojo {
    @Component
    private MavenProject project = null;

    @Component
    private ProjectBuilder projectBuilder = null;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    /**
     * If the plugin should be verbose.
     */
    @Parameter(property = "verbose", defaultValue = "false")
    private boolean verbose = false;

    /**
     * A list of scopes to exclude. May be used to exclude artifacts with test or provided scope from license check.
     * Example: &lt;configuration&gt; &lt;excludedScopes&gt; &lt;param&gt;test&lt;/param&gt;
     * &lt;param&gt;provided&lt;/param&gt; &lt;/excludedScopes&gt; &lt;/configuration&gt;
     */
    @Parameter(property = "excludedScopes")
    private String[] excludedScopes;

    LicenseAbstractMojo(
            MavenProject project,
            ProjectBuilder projectBuilder,
            MavenSession session) {
        this.project = project;
        this.projectBuilder = projectBuilder;
        this.session = session;
    }

    List<AyoyArtifact> parseArtifacts() {
        ArrayList<AyoyArtifact> toReturn = new ArrayList<AyoyArtifact>();

        ProjectBuildingRequest projectBuildingRequest = session.getProjectBuildingRequest();
        if (projectBuildingRequest == null) {
            throw new NullPointerException("Got null ProjectBuildingRequest from session.");
        }

        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(projectBuildingRequest);

        final Set<Artifact> artifacts = project.getDependencyArtifacts();
        for (final Artifact artifact : artifacts) {
            boolean isExcludedScope = false;
            if (this.excludedScopes != null) {
                for (String excludedScope : this.excludedScopes) {
                    if (excludedScope.equals(artifact.getScope())) {
                        isExcludedScope = true;
                    }
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
                if (mavenProject == null) {
                    throw new NullPointerException("MavenProject retrieved from ProjectBuilder.build is null");
                }

                List<License> licenses = mavenProject.getLicenses();
                if (licenses == null) {
                    throw new NullPointerException("Licenses is null, from mavenProject from " + artifact);
                }

                licenseInfo.addLicenses(licenses);

                toReturn.add(licenseInfo);
            } catch (ProjectBuildingException e) {
                getLog().error(e.getMessage());
            }
        }

        return toReturn;
    }

    public void setVerbose(String verbose) {
        this.verbose = Boolean.parseBoolean(verbose);
    }

    LicenseInfoFile getLicenseInfoFile(String licenseFile) throws MojoExecutionException {

        try {
            LicenseInfoFile file = new LicenseInfoFile(licenseFile, this.getLog());
            return file;
        } catch (FileNotFoundException e) {

            Path filePath = Paths.get(licenseFile);
            throw new MojoExecutionException("File \""
                + licenseFile
                + "\" (expanded to \""
                + filePath.toAbsolutePath().normalize()
                + "\")"
                + " could not be found.",
                e);
        }
    }

    ExcludedMissingLicenseFile getExcludedMissingLicensesFile(String licenseFile) throws MojoExecutionException {

        try {
            ExcludedMissingLicenseFile file = new ExcludedMissingLicenseFile(licenseFile, this.getLog());
            return file;
        } catch (FileNotFoundException e) {

            Path filePath = Paths.get(licenseFile);
            throw new MojoExecutionException("File \""
                + licenseFile
                + "\" (expanded to \""
                + filePath.toAbsolutePath().normalize()
                + "\")"
                + " could not be found.",
                e);
        }
    }

    void logInfoIfVerbose(String message) {
        if (this.verbose) {
            this.getLog().info(message);
        }
    }

    void checkInjects() {
        if (this.project == null) {
            throw new NullPointerException("project cannot be null.");
        }

        if (this.projectBuilder == null) {
            throw new NullPointerException("projectBuilder cannot be null");
        }

        if (this.session == null) {
            throw new NullPointerException("session cannot be null");
        }
    }


    String getPathForRelativeFile(String filePath, String fileDescription) {
        if (filePath == null || filePath.startsWith("/")) {
            // This is not a relative file.
            return filePath;
        }

        File tmpFile = new File(filePath);
        if (tmpFile.exists()) {
            // Found file relative this project
            return filePath;
        }

        // Relative path and not in this project. Perhaps in parent or parent-parent?
        this.getLog().debug(fileDescription
                + " - '"
                + filePath
                + "' is a relative file but could not be found in current project. Perhaps in parent projects?");
        String newFilename = filePath;
        MavenProject parentProject = project;
        while (!(new File(newFilename).exists())) {
            parentProject = parentProject.getParent();
            if (parentProject == null) {
                // Trying to find parent project but current project is null.
                return filePath;
            }
            if (parentProject.getBasedir() == null) {
                // Trying to find parent project baseDir but it's null.
                return filePath;
            }

            newFilename = new File(parentProject.getBasedir(), filePath).getPath();

            this.getLog().debug(fileDescription + " - Checking for file " + newFilename);
        }

        if (new File(newFilename).exists()) {
            return newFilename;
        }

        this.getLog().warn(fileDescription + " - Could not find file " + filePath);

        return filePath;
    }
}
