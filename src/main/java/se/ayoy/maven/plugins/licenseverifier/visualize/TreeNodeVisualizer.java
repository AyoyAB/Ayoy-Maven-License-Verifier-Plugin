package se.ayoy.maven.plugins.licenseverifier.visualize;

import org.apache.maven.artifact.Artifact;
import se.ayoy.maven.plugins.licenseverifier.LicenseInfo.LicenseInfo;
import se.ayoy.maven.plugins.licenseverifier.model.AyoyArtifact;
import se.ayoy.maven.plugins.licenseverifier.resolver.TreeNode;

import java.util.List;

/**
 * Visualizes a tree node with children.
 */
public final class TreeNodeVisualizer {
    private TreeNodeVisualizer() {
    }

    /**
     * Visualize a tree node with children.
     * @param treeNode the tree node
     * @param depth    the depth.
     * @return a string visualization.
     */
    public static String visualize(TreeNode<AyoyArtifact> treeNode, int depth) {
        StringBuilder toReturn = new StringBuilder();

        if (depth > 0) {
            // This is a child of the pom.
            indent(toReturn, treeNode);

            toReturn.append(formatAyoyArtifact(treeNode.getData()));
            toReturn.append(System.lineSeparator());
        }

        for (TreeNode<AyoyArtifact> childNode : treeNode) {
            toReturn.append(visualize(childNode, depth + 1));
            toReturn.append(System.lineSeparator());
        }

        // Return string without ending white spaces, such as crlf
        return toReturn.toString().replaceAll("\\s+$", "");
    }

    private static void indent(StringBuilder toReturn, TreeNode<AyoyArtifact> node) {
        for (int i = 0; i < node.getLevel(); i++) {
            toReturn.append("  ");
        }
    }

    private static String formatAyoyArtifact(AyoyArtifact ayoyArtifact) {
        StringBuilder toReturn = new StringBuilder();

        Artifact artifact = ayoyArtifact.getArtifact();
        toReturn.append(artifact.getGroupId());
        toReturn.append(":");
        toReturn.append(artifact.getArtifactId());
        toReturn.append(":");
        toReturn.append(artifact.getVersion());

        List<LicenseInfo> licenseInfos = ayoyArtifact.getLicenseInfos();
        for (LicenseInfo licenceInfo : licenseInfos) {
            toReturn.append(licenceInfo.getStatus());
            toReturn.append(licenceInfo.getName());
        }

        return toReturn.toString();
    }
}
