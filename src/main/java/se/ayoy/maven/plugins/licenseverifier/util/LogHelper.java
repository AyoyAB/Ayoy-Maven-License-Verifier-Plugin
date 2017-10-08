package se.ayoy.maven.plugins.licenseverifier.util;

import org.apache.maven.model.License;

public class LogHelper {
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

        if (license.getUrl() == null){
            toReturn.append("(null)");
        } else {
            toReturn.append("\"");
            toReturn.append(license.getUrl());
            toReturn.append("\"");
        }


        return toReturn.toString();
    }

    public static String logNullableString(String toLog) {
        if (toLog == null) {
            return "(null)";
        }

        return "\"" + toLog + "\"";
    }
}
