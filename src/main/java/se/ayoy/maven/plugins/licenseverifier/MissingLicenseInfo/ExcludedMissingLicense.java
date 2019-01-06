package se.ayoy.maven.plugins.licenseverifier.MissingLicenseInfo;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Represents an exclusion for a missing license.
 */
public class ExcludedMissingLicense {
    private String groupId;
    private String artifactId;
    private String version;

    /**
     * Initialize the instance from an XML node.
     * @param node the node that holds the information.
     */
    public ExcludedMissingLicense(Node node) {
        if (node == null) {
            throw new NullPointerException("Node cannot be null");
        }

        NodeList children = node.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            switch (child.getNodeName()) {
                case "groupId":
                    this.groupId = child.getTextContent();
                    break;
                case "artifactId":
                    this.artifactId = child.getTextContent();
                    break;
                case "version":
                    this.version = child.getTextContent();
                    break;
                default:
                    break;
            }
        }
    }

    public String getGroupId() {
        return this.groupId;
    }

    public String getArtifactId() {
        return this.artifactId;
    }

    public String getVersion() {
        return this.version;
    }
}
