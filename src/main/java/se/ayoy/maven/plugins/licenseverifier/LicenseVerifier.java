package se.ayoy.maven.plugins.licenseverifier;

import org.apache.maven.model.License;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import se.ayoy.maven.plugins.licenseverifier.LicenseInfo.LicenseInfo;
import se.ayoy.maven.plugins.licenseverifier.LicenseInfo.LicenseInfoFile;
import se.ayoy.maven.plugins.licenseverifier.model.AyoyArtifact;

import java.util.ArrayList;
import java.util.List;

/**
 * Validate the licenses against a list of known good.
 *
 */
@Mojo( name = "verify")
public class LicenseVerifier extends LicenseAbstractMojo {

    /**
     * A filename to the file with info on approved licenses.
     * Example: &lt;configuration&gt; &lt;excludedScopes&gt; &lt;param&gt;test&lt;/param&gt;
     * &lt;param&gt;provided&lt;/param&gt; &lt;/excludedScopes&gt; &lt;/configuration&gt;
     */
    @Parameter(property = "verify.licenseFile", defaultValue = "src/licenses/licenses.xml")
    String licenseFile;

    public void execute() throws MojoExecutionException {

        LicenseInfoFile file = this.getLicenseInfoFile(this.licenseFile);

        getLog().info( "Parsing dependencies.");
        List<AyoyArtifact> artefacts = parseArtefacts();

        getLog().info("Found "
                + artefacts.size()
                + " artefacts. Now validating their licenses with the list.");

        for (AyoyArtifact artefact : artefacts) {
            ArrayList<LicenseInfo> thisLicenseInfos = new ArrayList<LicenseInfo>();
            for (License license : artefact.getLicenses()) {
                LicenseInfo info = file.getLicenseInfo(license.getName(), license.getUrl());

                if (info == null) {
                    throw new MojoExecutionException("Unknown license for "
                        + artefact.getArtifact().getGroupId() + ":"
                        + artefact.getArtifact().getArtifactId()
                        + " - "
                        + "\""
                        + license.getName()
                        + "\", "
                        + license.getUrl());
                }
            }
        }
    }
}