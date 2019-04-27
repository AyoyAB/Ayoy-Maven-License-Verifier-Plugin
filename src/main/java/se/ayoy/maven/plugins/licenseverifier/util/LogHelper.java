package se.ayoy.maven.plugins.licenseverifier.util;

import org.apache.maven.model.License;

/**
 * A small utility to simpler log strings.
 */
public final class LogHelper {
    private LogHelper() {
    }

    /**
     * Log a license.
     * @param license the license to log.
     * @return the string representation of the license, suitable for logging.
     */
    public static String logLicense(License license) {
        if (license == null) {
            return "(null)";
        }

        StringBuilder toReturn = new StringBuilder();

        toReturn.append("Name: ");
        if (license.getName() == null) {
            toReturn.append("(null)");
        } else {
            toReturn.append("\"");
            toReturn.append(license.getName());
            toReturn.append("\"");
        }

        toReturn.append(", ");

        if (license.getUrl() == null) {
            toReturn.append("(null)");
        } else {
            toReturn.append("\"");
            toReturn.append(license.getUrl());
            toReturn.append("\"");
        }

        return toReturn.toString();
    }

    /**
     * Log a nullable string.
     * @param toLog the string to log.
     * @return the string representation of the string, suitable for logging.
     */
    public static String logNullableString(String toLog) {
        if (toLog == null) {
            return "(null)";
        }

        return "\"" + toLog + "\"";
    }
}
