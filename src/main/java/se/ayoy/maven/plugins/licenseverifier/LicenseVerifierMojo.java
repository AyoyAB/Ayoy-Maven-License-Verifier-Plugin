package se.ayoy.maven.plugins.licenseverifier;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.License;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import se.ayoy.maven.plugins.licenseverifier.LicenseInfo.LicenseInfo;
import se.ayoy.maven.plugins.licenseverifier.LicenseInfo.LicenseInfoFile;
import se.ayoy.maven.plugins.licenseverifier.LicenseInfo.LicenseInfoStatusEnum;
import se.ayoy.maven.plugins.licenseverifier.model.AyoyArtifact;
import se.ayoy.maven.plugins.licenseverifier.util.LogHelper;

import javax.inject.Inject;
import java.util.List;

/**
 * Validate the licenses against a list of known good.
 *
 */
@Mojo( name = "verify")
public class LicenseVerifierMojo extends LicenseAbstractMojo {

    /**
     * A filename to the file with info on approved licenses.
     * Example: &lt;configuration&gt; &lt;excludedScopes&gt; &lt;param&gt;test&lt;/param&gt;
     * &lt;param&gt;provided&lt;/param&gt; &lt;/excludedScopes&gt; &lt;/configuration&gt;
     */
    @Parameter(property = "verify.licenseFile", defaultValue = "src/licenses/licenses.xml")
    String licenseFile;

    @Parameter(property = "verify.failOnForbidden", defaultValue = "true")
    String failOnForbidden = "true";
    private boolean failOnForbiddenBool;

    @Parameter(property = "verify.failOnMissing", defaultValue = "true")
    String failOnMissing = "true";
    private boolean failOnMissingBool;

    @Parameter(property = "verify.failOnWarning", defaultValue = "true")
    String failOnWarning = "true";
    private boolean failOnWarningBool;

    @Parameter(property = "verify.failOnUnknown", defaultValue = "true")
    String failOnUnknown = "true";
    private boolean failOnUnknownBool;

    @Parameter(property = "verify.requireAllValid", defaultValue = "true")
    String requireAllValid = "true";
    private boolean requireAllValidBool;

    @Inject
    public LicenseVerifierMojo(MavenProject project, ProjectBuilder projectBuilder, MavenSession session) {
        super(project, projectBuilder, session);
    }

    public void execute() throws MojoExecutionException {
        try {
            checkInjects();
            parseParameters();

            LicenseInfoFile file = this.getLicenseInfoFile(this.licenseFile);

            getLog().info("Parsing dependencies.");
            List<AyoyArtifact> artifacts = parseArtifacts();

            getLog().info("Found "
                    + artifacts.size()
                    + " artifacts. Now validating their licenses with the list.");

            boolean hasUnknown = false;
            boolean hasValid = false;
            boolean hasWarning = false;
            boolean hasForbidden = false;
            boolean hasNoLicense = false;

            for (AyoyArtifact artifact : artifacts) {
                logInfoIfVerbose("Artifact: " + artifact);
                boolean artifactHasNoLicense = true;
                boolean artifactHasValidLicense = false;
                boolean artifactHasForbiddenLicense = false;
                boolean artifactHasWarningLicense = false;
                boolean artifactHasUnknownLicense = false;
                for (License license : artifact.getLicenses()) {
                    artifactHasNoLicense = false;
                    logInfoIfVerbose("    Checking license: " + LogHelper.logLicense(license));
                    LicenseInfo info = file.getLicenseInfo(license.getName(), license.getUrl());

                    if (info == null) {
                        // License does not exist in file.
                        info = new LicenseInfo(
                                license.getName(),
                                license.getUrl(),
                                LicenseInfoStatusEnum.UNKNOWN);
                        file.addLicenseInfo(info);
                    }

                    logInfoIfVerbose("    Got licenseInfo with status : " + info.getStatus());

                    switch (info.getStatus()) {
                        case VALID:
                            artifactHasValidLicense = true;
                            logInfoIfVerbose("VALID      " + artifact);
                            logInfoIfVerbose("           license:  " + info);
                            break;
                        case WARNING:
                            artifactHasWarningLicense = true;
                            getLog().warn("WARNING   " + artifact);
                            getLog().warn("          license:  " + info);
                            break;
                        case FORBIDDEN:
                            artifactHasForbiddenLicense = true;
                            getLog().warn("FORBIDDEN " + artifact);
                            getLog().warn("          license:  " + info);
                            break;
                        case UNKNOWN:
                            artifactHasUnknownLicense = true;
                            getLog().warn("UNKNOWN   " + artifact);
                            getLog().warn("          license:  " + info);
                            break;
                        default:
                            throw new MojoExecutionException("Unknown license status for " + artifact);
                    }
                }

                if (artifactHasNoLicense) {
                    getLog().warn("MISSING   " + artifact);
                    hasNoLicense = true;
                }

                if (artifactHasValidLicense) {
                    hasValid = true;
                }

                if (requireAllValidBool || !requireAllValidBool && !hasValid) {
                    if (artifactHasForbiddenLicense) {
                        hasForbidden = true;
                    }

                    if (artifactHasWarningLicense) {
                        hasWarning = true;
                    }

                    if (artifactHasUnknownLicense) {
                        hasUnknown = true;
                    }
                }
            }

            if (failOnMissingBool && hasNoLicense) {
                throw new MojoExecutionException(
                        "One or more artifacts is missing license information.");
            }

            if (failOnWarningBool && hasWarning) {
                throw new MojoExecutionException(
                        "One or more artifacts has licenses which is classified as warning.");
            }

            if (failOnUnknownBool && hasUnknown) {
                throw new MojoExecutionException(
                        "One or more artifacts has licenses which is unclassified.");
            }

            if (failOnForbiddenBool && hasForbidden) {
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

    @Override
    void checkInjects() {
        super.checkInjects();

        if (this.licenseFile == null) {
            throw new NullPointerException("licenseFile cannot be null. Check your settings.");
        }
    }


    private void parseParameters() {
        this.failOnForbiddenBool = Boolean.parseBoolean(this.failOnForbidden);

        this.failOnMissingBool = Boolean.parseBoolean(this.failOnMissing);

        this.failOnWarningBool = Boolean.parseBoolean(this.failOnWarning);

        this.failOnUnknownBool = Boolean.parseBoolean(this.failOnUnknown);

        this.requireAllValidBool = Boolean.parseBoolean(this.requireAllValid);
    }
}