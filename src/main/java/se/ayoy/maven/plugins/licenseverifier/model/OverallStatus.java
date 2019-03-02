package se.ayoy.maven.plugins.licenseverifier.model;

/**
 * Represents the total status of all artifacts.
 */
public class OverallStatus {
    private boolean hasNoLicense = false;
    private boolean hasForbiddenLicense = false;
    private boolean hasWarningLicense = false;
    private boolean hasUnknownLicense = false;

    public void setHasNoLicense(boolean value) {
        this.hasNoLicense = value;
    }

    public boolean getHasNoLicense() {
        return this.hasNoLicense;
    }

    public void setHasForbiddenLicense(boolean value) {
        this.hasForbiddenLicense = value;
    }

    public boolean getHasForbiddenLicense() {
        return this.hasForbiddenLicense;
    }

    public void setHasWarningLicense(boolean value) {
        this.hasWarningLicense = value;
    }

    public boolean getHasWarningLicense() {
        return this.hasWarningLicense;
    }

    public void setHasUnknownLicense(boolean value) {
        this.hasUnknownLicense = value;
    }

    public boolean getHasUnknownLicense() {
        return this.hasUnknownLicense;
    }
}
