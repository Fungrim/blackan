package io.fungrim.github.blackan.gradle.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class AppLibsReaderTest {

    private AppLibsReader.AppLibs parse(String toml) throws IOException {
        try (InputStream is = new ByteArrayInputStream(toml.getBytes(StandardCharsets.UTF_8))) {
            return AppLibsReader.parse(is);
        }
    }

    @Test
    void parsesBootAndRuntimeSections() throws IOException {
        String toml = """
                [boot]
                bootstrap = "io.github.fungrim.blackan.isolates:bootstrap:0.0.1"

                [runtime]
                runtime = "io.github.fungrim.blackan.isolates:runtime:0.0.1"
                slf4j = "io.github.fungrim.blackan.extension:slf4j:0.0.1"
                """;
        AppLibsReader.AppLibs result = parse(toml);
        assertEquals(1, result.boot().size());
        assertEquals("io.github.fungrim.blackan.isolates:bootstrap:0.0.1", result.boot().get(0));
        assertEquals(2, result.runtime().size());
        assertEquals("io.github.fungrim.blackan.isolates:runtime:0.0.1", result.runtime().get(0));
        assertEquals("io.github.fungrim.blackan.extension:slf4j:0.0.1", result.runtime().get(1));
    }

    @Test
    void emptyInputReturnsEmptyLists() throws IOException {
        AppLibsReader.AppLibs result = parse("");
        assertTrue(result.boot().isEmpty());
        assertTrue(result.runtime().isEmpty());
    }

    @Test
    void missingBootSectionReturnsEmptyBootList() throws IOException {
        String toml = """
                [runtime]
                runtime = "group:artifact:1.0"
                """;
        AppLibsReader.AppLibs result = parse(toml);
        assertTrue(result.boot().isEmpty());
        assertEquals(1, result.runtime().size());
    }

    @Test
    void missingRuntimeSectionReturnsEmptyRuntimeList() throws IOException {
        String toml = """
                [boot]
                bootstrap = "group:artifact:1.0"
                """;
        AppLibsReader.AppLibs result = parse(toml);
        assertEquals(1, result.boot().size());
        assertTrue(result.runtime().isEmpty());
    }

    @Test
    void handlesValuesWithoutQuotes() throws IOException {
        String toml = """
                [boot]
                bootstrap = group:artifact:1.0
                """;
        AppLibsReader.AppLibs result = parse(toml);
        assertEquals("group:artifact:1.0", result.boot().get(0));
    }

    @Test
    void ignoresLinesBeforeFirstSection() throws IOException {
        String toml = """
                orphan = "should:be:ignored"
                [boot]
                bootstrap = "group:artifact:1.0"
                """;
        AppLibsReader.AppLibs result = parse(toml);
        assertEquals(1, result.boot().size());
    }

    @Test
    void ignoresUnknownSections() throws IOException {
        String toml = """
                [boot]
                bootstrap = "group:boot:1.0"
                [other]
                something = "group:other:1.0"
                [runtime]
                runtime = "group:runtime:1.0"
                """;
        AppLibsReader.AppLibs result = parse(toml);
        assertEquals(1, result.boot().size());
        assertEquals(1, result.runtime().size());
    }

    @Test
    void handlesBlankLinesAndWhitespace() throws IOException {
        String toml = """
                
                [boot]
                
                  bootstrap = "group:artifact:1.0"
                
                [runtime]
                  runtime = "group:runtime:1.0"
                
                """;
        AppLibsReader.AppLibs result = parse(toml);
        assertEquals(1, result.boot().size());
        assertEquals("group:artifact:1.0", result.boot().get(0));
        assertEquals(1, result.runtime().size());
    }

    @Test
    void handlesMultipleEntriesPerSection() throws IOException {
        String toml = """
                [boot]
                a = "group:a:1.0"
                b = "group:b:2.0"
                c = "group:c:3.0"
                """;
        AppLibsReader.AppLibs result = parse(toml);
        assertEquals(3, result.boot().size());
        assertEquals("group:a:1.0", result.boot().get(0));
        assertEquals("group:b:2.0", result.boot().get(1));
        assertEquals("group:c:3.0", result.boot().get(2));
    }

    @Test
    void ignoresLinesWithoutEqualsSign() throws IOException {
        String toml = """
                [boot]
                this line has no equals
                bootstrap = "group:artifact:1.0"
                """;
        AppLibsReader.AppLibs result = parse(toml);
        assertEquals(1, result.boot().size());
    }

    @Test
    void readLoadsFromClasspath() {
        AppLibsReader.AppLibs result = AppLibsReader.fromResources();
        assertNotNull(result);
        assertFalse(result.boot().isEmpty());
        assertFalse(result.runtime().isEmpty());
    }
}
