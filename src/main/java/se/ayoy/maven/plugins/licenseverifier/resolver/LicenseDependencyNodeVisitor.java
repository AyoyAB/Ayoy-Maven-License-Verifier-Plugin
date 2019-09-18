package se.ayoy.maven.plugins.licenseverifier.resolver;

import org.apache.maven.shared.dependency.graph.DependencyNode;
import se.ayoy.maven.plugins.licenseverifier.model.AyoyArtifact;

/**
 * A visitor to collect license information from dependencies.
 */
public class LicenseDependencyNodeVisitor
    implements org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor {

    private TreeNode<AyoyArtifact> tree;
    private TreeNode<AyoyArtifact> currentNode;

    /**
     * Create the constructor.
     * @param tree the tree to populate
     */
    public LicenseDependencyNodeVisitor(TreeNode<AyoyArtifact> tree) {
        this.tree = tree;
        this.currentNode = this.tree;
    }

    @Override
    public boolean visit(DependencyNode node) {
        this.currentNode = currentNode.addChild(
            new AyoyArtifact(node.getArtifact(), null));

        return true;
    }

    @Override
    public boolean endVisit(DependencyNode node) {
        this.currentNode = this.currentNode.getParent();
        if (this.currentNode == null) {
            this.currentNode = this.tree;
        }

        return true;
    }
}
