package se.ayoy.maven.plugins.licenseverifier.LicenseInfo;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import se.ayoy.maven.plugins.licenseverifier.util.LogHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a license with information.
 */
public class LicenseInfo {
    private String name;

    private final ArrayList<String> configuredNames = new ArrayList<String>();

    private final ArrayList<String> configuredUrls = new ArrayList<String>();

    private LicenseInfoStatusEnum licenseInfoStatus;

    /**
     * Initialize the instance from XML.
     * @param node   The XML node to read from.
     * @param status The status of the license.
     */
    public LicenseInfo(Node node, LicenseInfoStatusEnum status) {
        this.licenseInfoStatus = status;

        if (node == null) {
            throw new NullPointerException("Node cannot be null");
        }

        NodeList children = node.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeName().equals("name")) {
                this.name = child.getTextContent();
            } else if (child.getNodeName().equals("names")) {
                parseNamesNode(child);
            } else if (child.getNodeName().equals("urls")) {
                parseUrlsNode(child);
            }
        }
    }

    /**
     * Initialize the instance from raw information.
     * @param name       The name of the license.
     * @param url        The URL of the license.
     * @param infoStatus The status of the licence.
     */
    public LicenseInfo(String name, String url, LicenseInfoStatusEnum infoStatus) {
        this.name = name;
        this.configuredNames.add(name);
        this.configuredUrls.add(url);
        this.licenseInfoStatus = infoStatus;
    }

    @Override
    public String toString() {
        StringBuilder toReturn = new StringBuilder();

        toReturn.append("name: ");
        toReturn.append(LogHelper.logNullableString(this.name));

        toReturn.append(", names: [");
        boolean isFirst = true;
        for (String name : this.configuredNames) {
            if (!isFirst) {
                toReturn.append(", ");
            }

            isFirst = false;
            toReturn.append(LogHelper.logNullableString(name));
        }

        toReturn.append("], urls: [");
        isFirst = true;
        for (String url : this.configuredUrls) {
            if (!isFirst) {
                toReturn.append(", ");
            }

            isFirst = false;
            toReturn.append(LogHelper.logNullableString(url));
        }
        toReturn.append("]");

        return toReturn.toString();
    }

    boolean hasLicenceInfo(String name, String url) {
        for (String configuredName : this.configuredNames) {
            if (configuredName == null) {
                continue;
            }

            if (configuredName.equals(name)) {
                return true;
            }
        }

        for (String configuredUrl : this.configuredUrls) {
            if (configuredUrl == null) {
                continue;
            }

            if (configuredUrl.equals(url)) {
                return true;
            }
        }

        return false;
    }

    public String getName() {
        return this.name;
    }

    public LicenseInfoStatusEnum getStatus() {
        return this.licenseInfoStatus;
    }

    private void parseNamesNode(Node node) {
        this.configuredNames.addAll(parseMultiValueNode(node, "name"));
    }

    private void parseUrlsNode(Node node) {
        this.configuredUrls.addAll(parseMultiValueNode(node, "url"));
    }

    private List<String> parseMultiValueNode(Node node, String subNodeName) {
        NodeList children = node.getChildNodes();

        ArrayList<String> values = new ArrayList<String>();
        for (int i = 0; i < children.getLength(); i++) {
            Node dataNode = children.item(i);

            if (!dataNode.getNodeName().equals(subNodeName)) {
                continue;
            }

            values.add(dataNode.getTextContent().trim());
        }

        return values;
    }
}
