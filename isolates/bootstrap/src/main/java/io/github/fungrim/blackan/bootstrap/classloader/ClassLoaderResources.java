package io.github.fungrim.blackan.bootstrap.classloader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClassLoaderResources {

    public static List<URL> readJarsFromFolder(Path folder) {
        try (ScanResult scanResult = new ClassGraph().acceptPaths(folder.toString()).scan()) {
            return scanResult.getResourcesWithExtension("jar")
                .stream()
                .map(Resource::getURL)
                .collect(Collectors.toList());
        }
    }

    public static List<URL> readJarsFromFileSystem(Path path) {
        File folder = getUrlPath().resolve(path).toFile();
        if(!folder.isDirectory()) {
            throw new IllegalArgumentException("Path is not a directory: " + folder.toPath());
        }
        return Arrays.stream(folder.listFiles((dir, name) -> name.endsWith(".jar")))
            .map(File::toURI)
            .map(t -> {
                try {
                    return t.toURL();
                } catch (MalformedURLException e) {
                    throw new IllegalStateException(e);
                }
            })
            .collect(Collectors.toList());
    }

    // this code is curtesy of quarkus
    private static Path getUrlPath() {
        String path = null;
        URL location = ClassLoaderResources.class.getProtectionDomain().getCodeSource().getLocation();
        if (location == null) {
            path = getPathFromClassName();
        } else {
            path = location.getPath();
        }
        if (path == null) {
            throw new IllegalStateException("Unable to determine launch jar path");
        }
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        System.out.println("ClassLoaderResources.getUrlPath: ");
        System.out.println(" - decodedPath: " + decodedPath);
        // the first get parent is the boot folder, the second the root
        Path root = new File(decodedPath).toPath().getParent().getParent();
        System.out.println(" - root: " + root);
        return root;
    }

    // account for https://bugs.openjdk.org/browse/JDK-8376576
    private static String getPathFromClassName() {
        String className = ClassLoaderResources.class.getSimpleName() + ".class";
        URL resource = ClassLoaderResources.class.getResource(className);
        if (resource != null) {
            String fullPath = resource.toString();
            if (fullPath.startsWith("jar:")) {
                return fullPath.substring(9, fullPath.indexOf("!"));
            }
        }
        return null;
    }
}
