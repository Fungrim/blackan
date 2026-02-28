package io.fungrim.github.blackan.gradle.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.GradleException;

public final class AppLibsReader {

    private static final String APP_LIBS_RESOURCE = "app-libs.toml";

    public record AppLibs(List<String> boot, List<String> runtime) {}

    public static AppLibs fromResources() {
        try (InputStream is = AppLibsReader.class.getClassLoader().getResourceAsStream(APP_LIBS_RESOURCE)) {
            if (is == null) {
                throw new GradleException("Could not find " + APP_LIBS_RESOURCE + " in plugin classpath");
            }
            return parse(is);
        } catch (IOException e) {
            throw new GradleException("Failed to read " + APP_LIBS_RESOURCE, e);
        }
    }

    static AppLibs parse(InputStream is) throws IOException {
        Map<String, List<String>> sections = new LinkedHashMap<>();
        String currentSection = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSection = line.substring(1, line.length() - 1);
                    sections.putIfAbsent(currentSection, new ArrayList<>());
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx < 0 || currentSection == null) {
                    continue;
                }
                String gav = line.substring(eqIdx + 1).trim();
                if (gav.startsWith("\"") && gav.endsWith("\"")) {
                    gav = gav.substring(1, gav.length() - 1);
                }
                sections.get(currentSection).add(gav);
            }
        }
        return new AppLibs(
                sections.getOrDefault("boot", List.of()),
                sections.getOrDefault("runtime", List.of())
        );
    }
}
