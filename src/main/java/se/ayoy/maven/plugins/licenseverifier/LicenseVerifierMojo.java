package se.ayoy.maven.plugins.licenseverifier;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.License;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import se.ayoy.maven.plugins.licenseverifier.LicenseInfo.LicenseInfo;
import se.ayoy.maven.plugins.licenseverifier.LicenseInfo.LicenseInfoFile;
import se.ayoy.maven.plugins.licenseverifier.LicenseInfo.LicenseInfoStatusEnum;
import se.ayoy.maven.plugins.licenseverifier.MissingLicenseInfo.ExcludedMissingLicenseFile;
import se.ayoy.maven.plugins.licenseverifier.model.AyoyArtifact;
import se.ayoy.maven.plugins.licenseverifier.model.OverallStatus;
import se.ayoy.maven.plugins.licenseverifier.resolver.TreeNode;
import se.ayoy.maven.plugins.licenseverifier.util.LogHelper;
import se.ayoy.maven.plugins.licenseverifier.visualize.TreeNodeVisualizer;

import java.io.IOException;
import java.util.List;

/**
 * Validate the licenses against a list of known good.
 *
 */
@Mojo(name = "verify")
public class LicenseVerifierMojo extends LicenseAbstractMojo {

    /**
     * A filename to the file with info on approved licenses.
     * Example: &lt;configuration&gt; &lt;excludedScopes&gt; &lt;param&gt;test&lt;/param&gt;
     * &lt;param&gt;provided&lt;/param&gt; &lt;/excludedScopes&gt; &lt;/configuration&gt;
     * <br>
     * The path can be parameterized absolute or relative. Since version 1.0.4 the plugin
     * can be specified in the parent of multi-module project and the children projects find
     * the license XML file in the nearest parent location with given relative path.
     * The child relative path takes the precedence.
     */
    @Parameter(property = "verify.licenseFile", defaultValue = "src/licenses/licenses.xml")
    private String licenseFile;

    @Parameter(property = "verify.excludedMissingLicensesFile", defaultValue = "")
    private String excludedMissingLicensesFile;

    @Parameter(property = "verify.failOnForbidden", defaultValue = "true")
    private boolean failOnForbidden = true;

    @Parameter(property = "verify.failOnMissing", defaultValue = "true")
    private boolean failOnMissing = true;

    @Parameter(property = "verify.failOnWarning", defaultValue = "true")
    private boolean failOnWarning = true;

    @Parameter(property = "verify.failOnUnknown", defaultValue = "true")
    private boolean failOnUnknown = true;

    @Parameter(property = "verify.requireAllValid", defaultValue = "true")
    private boolean requireAllValid = true;

    public void setLicenseFile(String licenseFile) {
        this.licenseFile = licenseFile;
    }

    public void setExcludedMissingLicensesFile(String excludedMissingLicensesFile) {
        this.excludedMissingLicensesFile = excludedMissingLicensesFile;
    }

    public void setFailOnForbidden(String failOnForbidden) {
        this.failOnForbidden = Boolean.parseBoolean(failOnForbidden);
    }

    public void setFailOnMissing(String failOnMissing) {
        this.failOnMissing = Boolean.parseBoolean(failOnMissing);
    }

    public void setFailOnWarning(String failOnWarning) {
        this.failOnWarning = Boolean.parseBoolean(failOnWarning);
    }

    public void setFailOnUnknown(String failOnUnknown) {
        this.failOnUnknown = Boolean.parseBoolean(failOnUnknown);
    }

    public void setRequireAllValid(String requireAllValid) {
        this.requireAllValid = Boolean.parseBoolean(requireAllValid);
    }

    /**
     * Execute the plugin.
     * @throws MojoExecutionException   if anything goes south,
     *                                  the thrown exception is always MojoExecutionException.
     */
    public void execute() throws MojoExecutionException {
        try {
            getLog().info("Checking injects.");
            checkInjects();

            getLog().info("Reading configuration");
            LicenseInfoFile licenseInfoFile = this.getLicenseInfoFile(
                getPathForRelativeFile(
                    this.licenseFile,
                    "LicenseInfo"));

            ExcludedMissingLicenseFile excludedMissingLicenseFile =
                this.getExcludedMissingLicensesFile(
                    getPathForRelativeFile(
                        this.excludedMissingLicensesFile,
                        "ExcludedMissingLicenses"));

            getLog().info("Parsing dependencies to dependency tree.");
            TreeNode<AyoyArtifact> tree = buildDependencyTree();

            if (this.getVerbose()) {
                String dependencyTreeString = TreeNodeVisualizer.visualize(tree, 0);
                logMultiLine(dependencyTreeString, getLog());
            }

            getLog().info("");
            getLog().debug("Removing filtered artifacts from tree.");
            removeFilteredArtifacts(tree, excludedMissingLicenseFile);

            getLog().info("Parsing for licenses.");
            ProjectBuildingRequest buildingRequest =
                new DefaultProjectBuildingRequest(getSession().getProjectBuildingRequest());
            checkForLicenses(buildingRequest, tree);

            getLog().info("");
            getLog().info("Determine license status.");
            determineArtifactStatus(tree, licenseInfoFile);

            getLog().info("");
            getLog().info("Determine overall status.");
            OverallStatus status = new OverallStatus();
            calculateOverallStatus(status, tree);

            if (failOnMissing && status.getHasNoLicense()) {
                throw new MojoExecutionException(
                    "One or more artifacts is missing license information.");
            }
            if (failOnWarning && status.getHasWarningLicense()) {
                throw new MojoExecutionException(
                    "One or more artifacts has licenses which is classified as warning.");
            }
            if (failOnUnknown && status.getHasUnknownLicense()) {
                throw new MojoExecutionException(
                    "One or more artifacts has licenses which is unclassified.");
            }
            if (failOnForbidden && status.getHasForbiddenLicense()) {
                throw new MojoExecutionException(
                    "One or more artifacts has licenses which is classified as forbidden.");
            }

            getLog().info("All licenses verified.");
        } catch (MojoExecutionException exc) {
            throw exc;
        } catch (DependencyGraphBuilderException | IOException exc) {
            throw new MojoExecutionException(exc.getMessage(), exc);
        }
    }

    private void checkForLicenses(ProjectBuildingRequest buildingRequest, TreeNode<AyoyArtifact> tree) {
        for (TreeNode<AyoyArtifact> childNode : tree) {
            AyoyArtifact ayoyArtifact = childNode.getData();
            logInfoIfVerbose("Checking license for " + ayoyArtifact.toString());

            List<License> licenses = getLicenses(ayoyArtifact.getArtifact(), buildingRequest);
            if (licenses != null) {
                ayoyArtifact.addLicenses(licenses);
            } else {
                getLog().info("Missing license for " + ayoyArtifact);
            }

            checkForLicenses(buildingRequest, childNode);
        }
    }

    private void removeFilteredArtifacts(
        TreeNode<AyoyArtifact> treeNode,
        ExcludedMissingLicenseFile excludedArtifacts) {

        for (int i = 0; i < treeNode.getChildren().size(); i++) {
            TreeNode<AyoyArtifact> childNode = treeNode.getChildren().get(i);
            if (!shouldArtifactBeIncluded(childNode.getData().getArtifact(), excludedArtifacts)) {
                getLog().info("Removing dependency with children: "
                    + childNode.getData().getArtifact().toString());
                treeNode.removeChild(childNode);
                i--;
            } else {
                // Check child
                removeFilteredArtifacts(childNode, excludedArtifacts);
            }
        }
    }

    /**
     * Determine the license status for the individual artifacts from the license information file.
     * @param treeNode        the individual artifacts.
     * @param licenseInfoFile the license information file.
     * @return the updated list of artifacts.
     */
    private void determineArtifactStatus(
        TreeNode<AyoyArtifact> treeNode,
        LicenseInfoFile licenseInfoFile) {

        for (TreeNode<AyoyArtifact> childNode : treeNode.getChildren()) {

            AyoyArtifact artifactToCheck = childNode.getData();
            logInfoIfVerbose("Artifact: "
                + artifactToCheck
                + " with "
                + artifactToCheck.getLicenses().size()
                + " licenses.");
            for (License license : artifactToCheck.getLicenses()) {
                logInfoIfVerbose("    Fetching license info: " + LogHelper.logLicense(license));
                LicenseInfo info = licenseInfoFile.getLicenseInfo(license.getName(), license.getUrl());

                if (info == null) {
                    // License does not exist in file.
                    info = new LicenseInfo(
                        license.getName(),
                        license.getUrl(),
                        LicenseInfoStatusEnum.UNKNOWN);
                    licenseInfoFile.addLicenseInfo(info);
                }

                logInfoIfVerbose("    Got licenseInfo with status : " + info.getStatus());
                artifactToCheck.addLicenseInfo(info);

            }
            determineArtifactStatus(childNode, licenseInfoFile);

        }
    }

    private void calculateOverallStatus(
        OverallStatus status,
        TreeNode<AyoyArtifact> node)
        throws MojoExecutionException {

        if (node.getData() != null) {
            logInfoIfVerbose("Checking overall status with " + node.getData().toString());
        }
        for (TreeNode<AyoyArtifact> childNode : node.getChildren()) {
            AyoyArtifact artifact = childNode.getData();

            // And recursive
            calculateOverallStatus(status, childNode);

            // Determine this license.
            if (artifact.isLicenseValid(requireAllValid)) {
                logInfoIfVerbose("VALID      " + artifact);
                for (LicenseInfo info : artifact.getLicenseInfos()) {
                    logInfoIfVerbose("           license:  " + info);
                }

                continue;
            }

            boolean artifactHasNoLicense = true;
            boolean artifactHasForbiddenLicense = false;
            boolean artifactHasWarningLicense = false;
            boolean artifactHasUnknownLicense = false;

            for (LicenseInfo info : artifact.getLicenseInfos()) {
                artifactHasNoLicense = false;

                switch (info.getStatus()) {
                    case VALID:
                        logInfoIfVerbose("VALID          " + artifact);
                        logInfoIfVerbose("               license:  " + info);
                        logInfoIfVerbose("               dependency chain: " + getChainString(childNode));
                        break;
                    case WARNING:
                        artifactHasWarningLicense = true;
                        getLog().warn("WARNING   " + artifact);
                        getLog().warn("          license:  " + info);
                        getLog().warn("          dependency chain: " + getChainString(childNode));
                        break;
                    case FORBIDDEN:
                        artifactHasForbiddenLicense = true;
                        getLog().warn("FORBIDDEN " + artifact);
                        getLog().warn("          license:  " + info);
                        getLog().warn("          dependency chain: " + getChainString(childNode));
                        break;
                    case UNKNOWN:
                        artifactHasUnknownLicense = true;
                        getLog().warn("UNKNOWN   " + artifact);
                        getLog().warn("          license:  " + info);
                        getLog().warn("          dependency chain: " + getChainString(childNode));
                        break;
                    default:
                        throw new MojoExecutionException("Unknown license status for " + artifact);
                }
            }

            if (artifactHasNoLicense) {
                getLog().warn("MISSING   " + artifact);
                status.setHasNoLicense(true);
            }

            if (artifactHasForbiddenLicense) {
                status.setHasForbiddenLicense(true);
            }

            if (artifactHasWarningLicense) {
                status.setHasWarningLicense(true);
            }

            if (artifactHasUnknownLicense) {
                status.setHasUnknownLicense(true);
            }
        }
    }

    /**
     * Creates a string representing the dependency chain to this artifact.
     * @param node the node to create the chain string from.
     * @return a string representing the dependency chain to this artifact.
     */
    public String getChainString(TreeNode<AyoyArtifact> node) {
        if (node.getData() == null) {
            return "";
        }

        StringBuilder toReturn = new StringBuilder();

        TreeNode<AyoyArtifact> currentNode = node;

        while (currentNode.getData() != null) {
            Artifact artifact = currentNode.getData().getArtifact();
            String artifactInfo = " -> "
                + artifact.getGroupId()
                + ":"
                + artifact.getArtifactId();
            toReturn.insert(0, artifactInfo);

            currentNode = currentNode.getParent();
        }

        toReturn.insert(0, "pom");

        return toReturn.toString();
    }

    @Override
    void checkInjects() {
        super.checkInjects();

        if (this.licenseFile == null) {
            throw new NullPointerException("licenseFile cannot be null. Check your settings.");
        }
    }
}
