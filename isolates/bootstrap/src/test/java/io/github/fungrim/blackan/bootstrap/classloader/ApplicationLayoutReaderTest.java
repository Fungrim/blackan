package io.github.fungrim.blackan.bootstrap.classloader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.bootstrap.layout.ApplicationLayout;

class ApplicationLayoutReaderTest {

    @Test
    void readsJarFilesFromApplicationLibrariesPath() {
        ApplicationLayout layout = ApplicationLayout.builder()
                .applicationLibrariesPath(Path.of("test-runtime"))
                .build();
        ApplicationLayoutReader reader = new ApplicationLayoutReader(layout);

        List<URL> jars = reader.readApplicationJars();

        assertFalse(jars.isEmpty());
        assertEquals(1, jars.size());
        assertTrue(jars.get(0).toString().contains("test-lib.jar"));
    }
}
