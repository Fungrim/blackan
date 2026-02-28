package io.fungrim.github.blackan.gradle.util;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public final class DependencyMatcher {

    private DependencyMatcher() {}

    public static boolean shouldIndex(String coordinate, boolean indexAll, List<String> includes, List<String> excludes) {
        for (String pattern : excludes) {
            if (matchesWildcard(coordinate, pattern)) {
                return false;
            }
        }
        if (indexAll) {
            return true;
        }
        for (String pattern : includes) {
            if (matchesWildcard(coordinate, pattern)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesWildcard(String coordinate, String pattern) {
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*");
        return Pattern.matches(regex, coordinate);
    }

    public static String extractCoordinate(File jarFile) {
        String path = jarFile.getAbsolutePath();

        // Maven local: .../repository/<group-dirs>/<artifact>/<version>/<file>.jar
        String repoMarker = File.separator + "repository" + File.separator;
        int repoIdx = path.lastIndexOf(repoMarker);
        if (repoIdx >= 0) {
            String remainder = path.substring(repoIdx + repoMarker.length());
            String[] parts = remainder.split(escape(File.separator));
            if (parts.length >= 3) {
                String artifact = parts[parts.length - 3];
                String group = String.join(".", Arrays.copyOf(parts, parts.length - 3));
                return group + ":" + artifact;
            }
        }

        // Gradle cache: .../files-<ver>/<group>/<artifact>/<version>/<hash>/<file>.jar
        String cacheMarker = File.separator + "files-";
        int cacheIdx = path.lastIndexOf(cacheMarker);
        if (cacheIdx >= 0) {
            int markerEnd = path.indexOf(File.separator, cacheIdx + 1);
            if (markerEnd >= 0) {
                String remainder = path.substring(markerEnd + 1);
                String[] parts = remainder.split(escape(File.separator));
                if (parts.length >= 4) {
                    return parts[0] + ":" + parts[1];
                }
            }
        }

        return null;
    }

    private static String escape(String separator) {
        return separator.equals("\\") ? "\\\\" : separator;
    }
}
