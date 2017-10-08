package se.ayoy.maven.plugins.licenseverifier;



import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.License;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuilder;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.mojo.license.api.DependenciesTool;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Says "Hi" to the user.
 *
 */
@Mojo( name = "sayhi")
public class LicenseVerifier extends AbstractMojo
{

    private static final Locale LOCALE = Locale.ENGLISH;
    /**
     * This is the repo system required by Aether
     *
     * For more, visit http://blog.sonatype.com/people/2011/01/how-to-use-aether-in-maven-plugins/
     */
    @Component
    RepositorySystem repoSystem;

    @Component
    final MavenProject project = null;

    /**
     * The current repository and network configuration of Maven
     *
     * For more, visit http://blog.sonatype.com/people/2011/01/how-to-use-aether-in-maven-plugins/
     *
     * @readonly
     */
    @Parameter(defaultValue = "${repositorySystemSession}")
    RepositorySystemSession repoSession;

    /**
     * This is the project's remote repositories that can be used for resolving plugins and their dependencies
     */
    @Parameter(defaultValue = "${project.remotePluginRepositories}")
    List<RemoteRepository> remoteRepos;

    /**
     * This is the maximum number of parents to search through (in case there's a malformed pom).
     */
    @Parameter(property = "os-check.recursion-limit", defaultValue = "12")
    int maxSearchDepth;

    /**
     * A list of artifacts that should be excluded from consideration. Example: &lt;configuration&gt; &lt;excludes&gt;
     * &lt;param&gt;full:artifact:coords&lt;/param&gt;>
     * &lt;/excludes&gt; &lt;/configuration&gt;
     */
    @Parameter(property = "os-check.excludes")
    String[] excludes;

    /**
     * A list of artifacts that should be excluded from consideration. Example: &lt;configuration&gt;
     * &lt;excludesRegex&gt; &lt;param&gt;full:artifact:coords&lt;/param&gt;>
     * &lt;/excludesRegex&gt; &lt;/configuration&gt;
     */
    @Parameter(property = "os-check.excludesRegex")
    String[] excludesRegex;

    @Parameter(property = "os-check.excludesNoLicense")
    boolean excludeNoLicense;

    /**
     * A list of blacklisted licenses. Example: &lt;configuration&gt; &lt;blacklist&gt;
     * &lt;param&gt;agpl-3.0&lt;/param&gt; &lt;param&gt;gpl-2.0&lt;/param&gt;
     * &lt;param&gt;gpl-3.0&lt;/param&gt; &lt;/blacklist&gt; &lt;/configuration&gt;
     */
    @Parameter(property = "os-check.blacklist")
    String[] blacklist;

    /**
     * A list of whitelisted licenses. Example: &lt;configuration&gt; &lt;whitelist&gt;
     * &lt;param&gt;agpl-3.0&lt;/param&gt; &lt;param&gt;gpl-2.0&lt;/param&gt;
     * &lt;param&gt;gpl-3.0&lt;/param&gt; &lt;/blacklist&gt; &lt;/configuration&gt;
     */
    @Parameter(property = "os-check.whitelist")
    String[] whitelist;

    /**
     * A list of scopes to exclude. May be used to exclude artifacts with test or provided scope from license check.
     * Example: &lt;configuration&gt; &lt;excludedScopes&gt; &lt;param&gt;test&lt;/param&gt;
     * &lt;param&gt;provided&lt;/param&gt; &lt;/excludedScopes&gt; &lt;/configuration&gt;
     */
    @Parameter(property = "os-check.excludedScopes")
    String[] excludedScopes;


    /**
     * dependencies tool.
     *
     * @since 1.1
     */
    @Component
    private DependenciesTool dependenciesTool;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    /**
     * Used to hold the list of license descriptors. Generation is lazy on the first method call to use it.
     */
    //List<LicenseDescriptor> descriptors = null;

    @Component
    private ProjectBuilder projectBuilder;

    public void execute() throws MojoExecutionException
    {
        getLog().info( "Hello, world." );

        getLog().info( "." );
        getLog().info( "Hello, my dependencies" );

        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());

        final Set<Artifact> artifacts = project.getDependencyArtifacts();
        for (final Artifact artifact : artifacts) {
            getLog().info(artifact.toString());
            try {
                buildingRequest.setProject(null);

                MavenProject mavenProject = projectBuilder.build(artifact, buildingRequest).getProject();
                final List<License> licenses = mavenProject.getLicenses();
                for (final License license : licenses) {
                    getLog().info("    " + license.getName());
                    getLog().info("    " + license.getUrl());
                }
            } catch (ProjectBuildingException e) {
                getLog().error(e.getMessage());
            }
        }
    }
}