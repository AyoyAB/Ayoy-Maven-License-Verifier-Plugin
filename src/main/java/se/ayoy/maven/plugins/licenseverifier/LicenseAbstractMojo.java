package se.ayoy.maven.plugins.licenseverifier;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
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
import org.apache.maven.repository.RepositorySystem;
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

import static java.lang.System.lineSeparator;

abstract class LicenseAbstractMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Component
    private ProjectBuilder projectBuilder;

    /**
     * @since 1.0.4
     */
    @Component
    private RepositorySystem repositorySystem;

    /**
     * ArtifactRepository of the localRepository directory.
     * @since 1.0.4
     */
    @Parameter(defaultValue = "${localRepository}", required = true, readonly = true)
    private ArtifactRepository localRepository;

    /**
     * The remote plugin repositories declared in the POM.
     * @since 1.0.4
     */
    @Parameter(defaultValue = "${project.pluginArtifactRepositories}")
    private List<ArtifactRepository> remoteRepositories;

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

    List<AyoyArtifact> parseArtifacts() throws MojoExecutionException {
        ArrayList<AyoyArtifact> toReturn = new ArrayList<AyoyArtifact>();

        ProjectBuildingRequest projectBuildingRequest = session.getProjectBuildingRequest();
        if (projectBuildingRequest == null) {
            throw new NullPointerException("Got null ProjectBuildingRequest from session.");
        }

        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(projectBuildingRequest);

        final Set<Artifact> artifacts = project.getDependencyArtifacts();
        for (final Artifact artifact : artifacts) {
            boolean isExcludedScope = matchesAnyScope(artifact, excludedScopes);

            if (isExcludedScope) {
                getLog().info("Artifact is excluded from scope \""
                        + artifact.getScope()
                        + "\": "
                        + artifact.getGroupId()
                        + ":"
                        + artifact.getArtifactId());
                continue;
            }

            toReturn.add(toAyoyArtifact(artifact, buildingRequest));

            Set<Artifact> transitiveArtifacts = resolveTransitiveArtifact(artifact);

            StringBuilder transitiveArtifactsList = new StringBuilder();
            for (Artifact transitiveArtifact : transitiveArtifacts) {
                toReturn.add(toAyoyArtifact(transitiveArtifact, buildingRequest));
                transitiveArtifactsList.append(lineSeparator())
                        .append(transitiveArtifact.toString());
            }
            if (getLog().isDebugEnabled() && !transitiveArtifacts.isEmpty()) {
                getLog().debug("Verifying "
                        + transitiveArtifacts.size()
                        + " transitive artifacts for "
                        + artifact.getGroupId()
                        + ":"
                        + artifact.getArtifactId()
                        + ":"
                        + transitiveArtifactsList
                );
            }
        }

        return toReturn;
    }

    private AyoyArtifact toAyoyArtifact(Artifact artifact, ProjectBuildingRequest buildingRequest) 
        throws MojoExecutionException {
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
        } catch (ProjectBuildingException e) {
            getLog().error(e.getMessage());
            throw new MojoExecutionException("Could not build the project", e);
        }
        return licenseInfo;
    }

    private static boolean matchesAnyScope(Artifact artifact, String... scopes) {
        if (scopes != null) {
            for (String scope : scopes) {
                if (scope.equals(artifact.getScope())) {
                    return true;
                }
            }
        }
        return false;
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

    private Set<Artifact> resolveTransitiveArtifact(Artifact providerArtifact) {
        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
                .setArtifact(providerArtifact)
                .setRemoteRepositories(remoteRepositories)
                .setLocalRepository(localRepository)
                .setResolveTransitively(true);

        ArtifactResolutionResult resolutionResult = repositorySystem.resolve(request);

        resolutionResult.getArtifacts()
                .remove(providerArtifact);

        resolutionResult.getArtifacts()
                .removeIf(transitive -> matchesAnyScope(transitive, excludedScopes));

        return resolutionResult.getArtifacts();
    }
}
