package se.ayoy.maven.plugins.licenseverifier;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import se.ayoy.maven.plugins.licenseverifier.model.AyoyArtifact;
import se.ayoy.maven.plugins.licenseverifier.resolver.TreeNode;
import se.ayoy.maven.plugins.licenseverifier.visualize.TreeNodeVisualizer;

import java.io.IOException;

/**
 * A tree mojo, much like maven-dependency-plugin:tree.
 */
@Mojo(name = "tree")
public class TreeMojo extends LicenseAbstractMojo {
    /**
     * Query the dependency tree for dependencies and visualize.
     * @throws MojoExecutionException when something goes south.
     */
    @Override
    public void execute() throws MojoExecutionException {
        try {
            getLog().info("Checking injects.");
            checkInjects();

            getLog().info("Parsing dependencies to dependency tree.");
            TreeNode<AyoyArtifact> tree = buildDependencyTree();
            String dependencyTreeString = TreeNodeVisualizer.visualize(tree, 0);
            logMultiLine(dependencyTreeString, getLog());
        } catch (DependencyGraphBuilderException | IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    void checkInjects() {
        if (this.getProject() == null) {
            throw new NullPointerException("project cannot be null.");
        }

        if (this.getSession() == null) {
            throw new NullPointerException("session cannot be null");
        }
    }
}
