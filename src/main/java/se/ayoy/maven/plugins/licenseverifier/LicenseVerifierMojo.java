package se.ayoy.maven.plugins.licenseverifier;

import org.apache.maven.model.License;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import se.ayoy.maven.plugins.licenseverifier.LicenseInfo.LicenseInfo;
import se.ayoy.maven.plugins.licenseverifier.LicenseInfo.LicenseInfoFile;
import se.ayoy.maven.plugins.licenseverifier.LicenseInfo.LicenseInfoStatusEnum;
import se.ayoy.maven.plugins.licenseverifier.MissingLicenseInfo.ExcludedMissingLicenseFile;
import se.ayoy.maven.plugins.licenseverifier.model.AyoyArtifact;
import se.ayoy.maven.plugins.licenseverifier.model.OverallStatus;
import se.ayoy.maven.plugins.licenseverifier.util.LogHelper;

import java.util.List;
import java.util.stream.Collectors;

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

    @Parameter(property = "verify.checkTransitiveDependencies", defaultValue = "true")
    private boolean checkTransitiveDependencies = true;

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

    public void setCheckTransitiveDependencies(String checkTransitiveDependencies) {
        this.checkTransitiveDependencies = Boolean.parseBoolean(checkTransitiveDependencies);
    }

    /**
     * Execute the plugin.
     * @throws MojoExecutionException   if anything goes south,
     *                                  the thrown exception is always MojoExecutionException.
     */
    public void execute() throws MojoExecutionException {
        try {
            checkInjects();

            LicenseInfoFile licenseInfoFile = this.getLicenseInfoFile(
                    getPathForRelativeFile(
                            this.licenseFile,
                            "LicenseInfo"));

            ExcludedMissingLicenseFile excludedMissingLicenseFile =
                this.getExcludedMissingLicensesFile(
                        getPathForRelativeFile(
                                this.excludedMissingLicensesFile,
                                "ExcludedMissingLicenses"));

            getLog().info("Parsing dependencies.");
            List<AyoyArtifact> unfilteredArtifacts = parseArtifacts(excludedMissingLicenseFile);

            getLog().info("Found " + unfilteredArtifacts.size() + " artifacts.");

            // Filter away excluded artifacts.
            List<AyoyArtifact> filteredArtifacts =
                    unfilteredArtifacts
                            .stream()
                            .filter(artifact -> shouldArtifactBeIncluded(
                                    artifact.getArtifact(),
                                    excludedMissingLicenseFile))
                            .collect(Collectors.toList());

            getLog().info("Found "
                    + filteredArtifacts.size()
                    + " artifacts after filtering. Now validating their licenses with the list.");

            // Loop through all artifacts and determine status
            filteredArtifacts = determineArtifactStatus(filteredArtifacts, licenseInfoFile);

            // Loop through all artifacts and get the overall status
            OverallStatus status = calculateOverallStatus(filteredArtifacts);

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
        } catch (Exception exc) {
            throw new MojoExecutionException(exc.getMessage(), exc);
        }
    }

    /**
     * Determine the license status for the individual artifacts from the license information file.
     * @param artifacts       the individual artifacts.
     * @param licenseInfoFile the license information file.
     * @return the updated list of artifacts.
     */
    private List<AyoyArtifact> determineArtifactStatus(
            List<AyoyArtifact> artifacts,
            LicenseInfoFile licenseInfoFile) {

        for (AyoyArtifact artifactToCheck : artifacts) {
            logInfoIfVerbose("Artifact: " + artifactToCheck);
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
        }

        return artifacts;
    }

    private OverallStatus calculateOverallStatus(List<AyoyArtifact> filteredArtifacts) throws MojoExecutionException {

        OverallStatus status = new OverallStatus();

        for (AyoyArtifact artifact : filteredArtifacts) {
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
                        logInfoIfVerbose("VALID      " + artifact);
                        logInfoIfVerbose("           license:  " + info);
                        break;
                    case WARNING:
                        artifactHasWarningLicense = true;
                        getLog().warn("WARNING   " + artifact);
                        getLog().warn("          license:  " + info);
                        getLog().warn("          dependency chain: " + artifact.getChainString());
                        break;
                    case FORBIDDEN:
                        artifactHasForbiddenLicense = true;
                        getLog().warn("FORBIDDEN " + artifact);
                        getLog().warn("          license:  " + info);
                        getLog().warn("          dependency chain: " + artifact.getChainString());
                        break;
                    case UNKNOWN:
                        artifactHasUnknownLicense = true;
                        getLog().warn("UNKNOWN   " + artifact);
                        getLog().warn("          license:  " + info);
                        getLog().warn("          dependency chain: " + artifact.getChainString());
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

        return status;
    }

    @Override
    void checkInjects() {
        super.checkInjects();

        if (this.licenseFile == null) {
            throw new NullPointerException("licenseFile cannot be null. Check your settings.");
        }
    }

    @Override
    protected boolean shouldCheckTransitiveDependencies() {
        return checkTransitiveDependencies;
    }
}
