package se.ayoy.maven.plugins.licenseverifier;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.License;
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
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.BuildingDependencyNodeVisitor;
import se.ayoy.maven.plugins.licenseverifier.LicenseInfo.LicenseInfoFile;
import se.ayoy.maven.plugins.licenseverifier.MissingLicenseInfo.ExcludedMissingLicenseFile;
import se.ayoy.maven.plugins.licenseverifier.model.AyoyArtifact;
import se.ayoy.maven.plugins.licenseverifier.resolver.LicenseDependencyNodeVisitor;
import se.ayoy.maven.plugins.licenseverifier.resolver.TreeNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

abstract class LicenseAbstractMojo extends AbstractMojo {
    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @SuppressWarnings("unused")
    @Component
    private ProjectBuilder projectBuilder;

    /**
     * The dependency tree builder to use.
     */
    @SuppressWarnings("unused")
    @Component(hint = "default")
    private DependencyGraphBuilder dependencyGraphBuilder;

    /**
     * Contains the full list of projects in the reactor.
     */
    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    private List<MavenProject> reactorProjects;

    @SuppressWarnings("unused")
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

    MavenProject getProject() {
        return this.project;
    }

    MavenSession getSession() {
        return this.session;
    }

    /**
     * Serializes the specified dependency tree to a string.
     *
     * @return the dependency tree.
     */
    TreeNode<AyoyArtifact> buildDependencyTree() throws DependencyGraphBuilderException {

        ProjectBuildingRequest buildingRequest =
            new DefaultProjectBuildingRequest(getSession().getProjectBuildingRequest());

        buildingRequest.setProject(getProject());

        // non-verbose mode use dependency graph component, which gives consistent results with Maven version
        // running
        DependencyNode rootDependencyNode = dependencyGraphBuilder.buildDependencyGraph(
            buildingRequest,
            null,
            reactorProjects);

        TreeNode<AyoyArtifact> tree = new TreeNode<AyoyArtifact>(null);
        LicenseDependencyNodeVisitor nodeVisitor = new LicenseDependencyNodeVisitor(tree);

        BuildingDependencyNodeVisitor dependencyNodeVisitor =
            new BuildingDependencyNodeVisitor(nodeVisitor);

        rootDependencyNode.accept(dependencyNodeVisitor);

        return tree;
    }

    /**
     * Check if an artifact should be included in lists.
     * @param a                 the artifact.
     * @param excludedArtifacts the list of excluded artifacts.
     * @return true if included.
     */
    boolean shouldArtifactBeIncluded(Artifact a, ExcludedMissingLicenseFile excludedArtifacts) {
        if (a == null) {
            return false;
        }

        if (a.isOptional()) {
            logInfoIfVerbose("Excluding optional artifact: " + toString(a));
            return false;
        }

        if (matchesAnyScope(a, excludedScopes)) {
            logInfoIfVerbose("Excluding artifact with scope \""
                    + a.getScope()
                    + "\": " + toString(a));
            return false;
        }

        if (excludedArtifacts != null && excludedArtifacts.isExcluded(a)) {
            logInfoIfVerbose("Excluding artifact, found in configuration: "
                    + toString(a));
            return false;
        }

        return true;
    }

    List<License> getLicenses(Artifact artifact, ProjectBuildingRequest buildingRequest) {
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

            return licenses;
        } catch (ProjectBuildingException e) {
            getLog().error("Could not build the project for " + artifact.toString());
            getLog().error(e.getMessage());
        }

        return null;
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

    public boolean getVerbose() {
        return this.verbose;
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
        } else {
            this.getLog().debug(message);
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

    private String toString(Artifact artifact) {
        return artifact.getGroupId()
                + ":"
                + artifact.getArtifactId()
                + ":"
                + artifact.getVersion();
    }

    /**
     * Writes the specified string to the log at info level.
     *
     * @param string the string to write
     * @param log where to log information.
     * @throws IOException if an I/O error occurs
     */
    static synchronized void logMultiLine(String string, Log log)
        throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(string));

        String line;

        while ((line = reader.readLine()) != null) {
            log.info(line);
        }

        reader.close();
    }
}
