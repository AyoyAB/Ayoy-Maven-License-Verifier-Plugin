package se.ayoy.maven.plugins.licenseverifier.MissingLicenseInfo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import se.ayoy.maven.plugins.licenseverifier.model.AyoyArtifact;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Parses the file for exclusions of missing license information.
 */
public class ExcludedMissingLicenseFile {
    private ArrayList<ExcludedMissingLicense> missingInfos = new ArrayList<ExcludedMissingLicense>();
    private Log log;

    /**
     * Initialize the instance from a file.
     * @param filePathString          The path to the file.
     * @param log                     The log instance to be able to log.
     * @throws FileNotFoundException  thrown when the file could not be found.
     * @throws MojoExecutionException thrown when something goes wrong during initialization.
     */
    public ExcludedMissingLicenseFile(String filePathString, Log log)
        throws FileNotFoundException, MojoExecutionException {
        this.log = log;

        if (filePathString == null) {
            return;
        }

        this.log.info(
            "Path to file with dependencies to ignore (without licenses) is "
            + filePathString);
        File file = new File(filePathString);
        if (!file.exists()) {
            throw new FileNotFoundException(filePathString);
        }

        log.debug("Reading file " + filePathString);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            // Disable doctype
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            // Disable external DTDs as well
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            // and these as well, per Timothy Morgan's 2014 paper: "XML Schema, DTD, and Entity Attacks"
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);

            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document document = builder.parse(file);

            parseInfos(document);

        } catch (ParserConfigurationException e) {
            throw new MojoExecutionException(e.getMessage(), e);

        } catch (SAXException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        log.debug(
            "Found approved dependencies with missing license information: "
            + this.missingInfos.size());
    }

    /**
     * Check if a certain artifact with missing license information is excluded.
     * @param artifact the artifact.
     * @return true if excluded.
     */
    public boolean isExcluded(AyoyArtifact artifact) {
        for (ExcludedMissingLicense missingInfo : this.missingInfos) {

            if (missingInfo.getGroupId().equals(artifact.getArtifact().getGroupId())
                && missingInfo.getArtifactId().equals(artifact.getArtifact().getArtifactId())
                && missingInfo.getVersion().equals(artifact.getArtifact().getVersion())) {
                return true;
            }
        }
        return false;
    }

    private void parseInfos(Document document) {
        String nodeName = "dependency";
        log.debug("Parsing document for " + nodeName);
        NodeList licensesList = document.getDocumentElement().getElementsByTagName(nodeName);
        for (int i = 0; i < licensesList.getLength(); i++) {
            Node node = licensesList.item(i);
            if (!node.getNodeName().equals("dependency")) {

                // log.debug("This isn't a dependency: " + node.getNodeName());
                continue;
            }

            ExcludedMissingLicense info = new ExcludedMissingLicense(node);

            if (info.getGroupId() == null) {
                this.log.warn("Found entry in file for excluded missing licenses with missing groupId");
            } else if (info.getArtifactId() == null) {
                this.log.warn("Found entry in file for excluded missing licenses with missing artifactId");
            } else if (info.getVersion() == null) {
                this.log.warn("Found entry in file for excluded missing licenses with missing version");
            } else {
                missingInfos.add(info);
            }
        }
    }
}
