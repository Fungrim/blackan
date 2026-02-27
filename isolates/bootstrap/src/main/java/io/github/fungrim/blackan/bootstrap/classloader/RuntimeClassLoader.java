package io.github.fungrim.blackan.bootstrap.classloader;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import io.github.fungrim.blackan.bootstrap.layout.ApplicationLayout;
import lombok.Getter;

public class RuntimeClassLoader extends URLClassLoader {

    public static class Builder {

        private ApplicationLayout layout;
        private ClassLoader parent;

        public Builder withLayout(ApplicationLayout layout) {
            this.layout = layout;
            return this;
        }

        public Builder withParentClassLoader(ClassLoader parent) {
            this.parent = parent;
            return this;
        }

        public RuntimeClassLoader build() {
            if(layout == null) {
                throw new IllegalArgumentException("ApplicationLayout must not be null");
            }
            if(parent == null) {
                parent = Thread.currentThread().getContextClassLoader();
                if(parent == null) {
                    parent = RuntimeClassLoader.class.getClassLoader();
                }
            }
            ApplicationLayoutReader layoutReader = new ApplicationLayoutReader(layout);
            List<URL> urls = layoutReader.readApplicationJars();
            System.out.println("RuntimeClassLoader.build: ");
            urls.forEach(url -> System.out.println(" - " + url));
            return new RuntimeClassLoader(parent, layout, urls.toArray(new URL[urls.size()]));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Getter
    private final ApplicationLayout layout;

    @Getter
    private final List<URL> applicationJars;

    private RuntimeClassLoader(ClassLoader parent, ApplicationLayout layout, URL[] jars) {
        super(jars, parent);
        this.applicationJars = List.of(jars);
        this.layout = layout;
    }
}
