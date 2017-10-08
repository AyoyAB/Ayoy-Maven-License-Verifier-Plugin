package se.ayoy.maven.plugins.licenseverifier;

import org.apache.maven.model.License;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import se.ayoy.maven.plugins.licenseverifier.LicenseInfo.LicenseInfo;
import se.ayoy.maven.plugins.licenseverifier.LicenseInfo.LicenseInfoFile;
import se.ayoy.maven.plugins.licenseverifier.LicenseInfo.LicenseInfoStatusEnum;
import se.ayoy.maven.plugins.licenseverifier.model.AyoyArtifact;
import se.ayoy.maven.plugins.licenseverifier.util.LogHelper;

import java.util.ArrayList;
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

    public void execute() throws MojoExecutionException {
        try {

            LicenseInfoFile file = this.getLicenseInfoFile(this.licenseFile);

            getLog().info("Parsing dependencies.");
            List<AyoyArtifact> artifacts = parseArtifacts();

            getLog().info("Found "
                    + artifacts.size()
                    + " artifacts. Now validating their licenses with the list.");


            boolean hasUnknown = false;
            boolean hasValid = false;
            boolean hasWarning = false;
            boolean hasInvalid = false;
            ArrayList<LicenseInfo> thisLicenseInfos = new ArrayList<LicenseInfo>();
            for (AyoyArtifact artifact : artifacts) {
                logInfoIfVerbose("Artifact: " + artifact);
                for (License license : artifact.getLicenses()) {
                    logInfoIfVerbose("    Checking license: " + LogHelper.logLicense(license));
                    LicenseInfo info = file.getLicenseInfo(license.getName(), license.getUrl());
                    logInfoIfVerbose("    Got licenseInfo: " + info);

                    if (info == null) {
                        // License does not exist in file.
                        info = new LicenseInfo(
                                license.getName(),
                                license.getUrl(),
                                LicenseInfoStatusEnum.UNKNOWN);
                        file.addLicenseInfo(info);
                    }

                    switch (info.getStatus()) {
                        case VALID:
                            hasValid = true;
                            logInfoIfVerbose("VALID      artifact: " + artifact);
                            logInfoIfVerbose("           license:  " + info);
                            break;
                        case WARNING:
                            hasWarning = true;
                            getLog().warn("WARNING   artifact: " + artifact);
                            getLog().warn("          license:  " + info);
                            break;
                        case FORBIDDEN:
                            hasInvalid = true;
                            getLog().warn("FORBIDDEN artifact: " + artifact);
                            getLog().warn("          license:  " + info);
                            break;
                        case UNKNOWN:
                            hasUnknown = true;
                            getLog().warn("UNKNOWN   artifact: " + artifact);
                            getLog().warn("          license:  " + info);
                            break;
                        default:
                            throw new MojoExecutionException("Unknown license status for " + artifact);
                    }

                    thisLicenseInfos.add(info);
                }
            }

            if (hasInvalid || hasWarning || hasUnknown) {
                throw new MojoExecutionException("Has invalid licenses");
            }

            getLog().info("All licenses verified.");
        } catch (MojoExecutionException exc) {
            throw exc;
        } catch (Exception exc) {
            exc.printStackTrace();
            throw new MojoExecutionException(exc.getMessage(), exc);
        }
    }
}