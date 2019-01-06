package se.ayoy.maven.plugins.licenseverifier.LicenseInfo;

/**
 * Represents the statuses a license can have.
 */
public enum LicenseInfoStatusEnum {
    /**
     * A valid license.
     */
    VALID,
    /**
     * A license with some property that makes it a warning.
     */
    WARNING,
    /**
     * A license with some property that makes it forbidden to use.
     */
    FORBIDDEN,
    /**
     * A license which is not predefined, therefor unknown.
     */
    UNKNOWN
}
