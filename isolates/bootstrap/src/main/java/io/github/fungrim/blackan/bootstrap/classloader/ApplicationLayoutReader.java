package io.github.fungrim.blackan.bootstrap.classloader;

import java.net.URL;
import java.util.List;

import io.github.fungrim.blackan.bootstrap.layout.ApplicationLayout;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ApplicationLayoutReader {

    private ApplicationLayout layout;

    public List<URL> readApplicationJars() {
        List<URL> urls = ClassLoaderResources.readJarsFromFolder(layout.getApplicationLibrariesPath());
        if(urls.isEmpty()) {
            urls = ClassLoaderResources.readJarsFromFileSystem(layout.getApplicationLibrariesPath());
        }
        return urls;
    }
}
