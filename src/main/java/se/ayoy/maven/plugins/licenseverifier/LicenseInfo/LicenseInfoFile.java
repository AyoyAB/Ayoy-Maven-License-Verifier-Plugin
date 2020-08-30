package se.ayoy.maven.plugins.licenseverifier.LicenseInfo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException; // catching unsupported features

import se.ayoy.maven.plugins.licenseverifier.LicenceFile;

/**
 * Represents the file in which licenses are categorized.
 */
public class LicenseInfoFile extends LicenceFile {

    private ArrayList<LicenseInfo> licenseInfos = new ArrayList<LicenseInfo>();
    private Log log;

    /**
     * Initialize the instance from a file.
     * @param filePathString          The path to the file.
     * @param log                     The log instance to be able to log.
     * @throws FileNotFoundException  thrown when the file could not be found.
     * @throws MojoExecutionException thrown when something goes wrong during initialization.
     */
    public LicenseInfoFile(String filePathString, Log log)
            throws FileNotFoundException, MojoExecutionException {
        this.log = log;

        if (filePathString == null) {
            throw new IllegalArgumentException("The path cannot be null");
        }

        this.log.info(
                "Path to file with licenses is "
                        + filePathString);

        File file = new File(filePathString);
        InputStream inputStream = getInputStreamFromFileOrResource(file, filePathString);

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
            Document document = builder.parse(inputStream);

            parseLicenses(document, "valid", LicenseInfoStatusEnum.VALID);
            parseLicenses(document, "warning", LicenseInfoStatusEnum.WARNING);
            parseLicenses(document, "forbidden", LicenseInfoStatusEnum.FORBIDDEN);

        } catch (ParserConfigurationException e) {
            throw new MojoExecutionException(e.getMessage(), e);

        } catch (SAXException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        log.debug("Found licenses: " + this.licenseInfos.size());
    }

    /**
     * Search for a licence using name or url.
     * @param name name to search for.
     * @param url  url to search for.
     * @return the found license or null.
     */
    public LicenseInfo getLicenseInfo(String name, String url) {
        for (LicenseInfo info : this.licenseInfos) {
            if (info.hasLicenceInfo(name, url)) {
                return info;
            }
        }

        return null;
    }

    /**
     * Add a license to the "database".
     * @param licenseInfo the license to add.
     */
    public void addLicenseInfo(LicenseInfo licenseInfo) {
        this.licenseInfos.add(licenseInfo);
    }

    private void parseLicenses(Document document, String nodeName, LicenseInfoStatusEnum status)
            throws MojoExecutionException {
        log.debug("Parsing document for " + nodeName);
        NodeList approvedList = document.getDocumentElement().getElementsByTagName(nodeName);
        if (approvedList.getLength() == 0) {
            log.debug("There are no nodes for \"" + nodeName + "\".");
            return;
        }

        if (approvedList.getLength() != 1) {
            throw new MojoExecutionException("There may be only one tag with name \"" + nodeName + "\".");
        }

        NodeList licensesList = approvedList.item(0).getChildNodes();
        for (int i = 0; i < licensesList.getLength(); i++) {
            Node node = licensesList.item(i);
            if (!node.getNodeName().equals("license")) {

                // log.debug("This isn't a license: " + node.getNodeName());
                continue;
            }

            LicenseInfo info = new LicenseInfo(node, status);
            licenseInfos.add(info);
        }
    }
}
