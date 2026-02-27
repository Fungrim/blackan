package io.github.fungrim.blackan.injector.context;

import java.io.IOException;
import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import io.github.fungrim.blackan.common.util.Arguments;
import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.Scope;

public class RootContext extends ContextImpl {

    private RootContext(IndexView index, Scope scope, ClassLoader classLoader, ProcessScopeProvider scopeProvider) {
        super(index, null, scope, classLoader, scopeProvider);
        scanProducers();
    }

    private void scanProducers() {
        for (ClassInfo classInfo : index.getKnownClasses()) {
            try {
                Class<?> clazz = classLoader.loadClass(classInfo.name().toString());
                producerRegistry.scan(this, clazz);
            } catch (ClassNotFoundException e) {
                // skip classes that can't be loaded
            }
        }
    }

    public static class Builder {

        private IndexView index;
        private List<Class<?>> classes;
        private ClassLoader classLoader;
        private ProcessScopeProvider scopeProvider;

        public Builder withScopeProvider(ProcessScopeProvider scopeProvider) {
            this.scopeProvider = scopeProvider;
            return this;
        }

        public Builder withClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public Builder withClasses(List<Class<?>> classes) {
            this.classes = classes;
            return this;
        }

        public Builder withIndex(IndexView index) {
            this.index = index;
            return this;
        }
     
        public RootContext build() throws IOException {
            if(index != null && classes != null) {
                throw new IllegalArgumentException("Index and classes cannot both be specified");
            }
            if(index == null && classes == null) {
                throw new IllegalArgumentException("Index or classes must be specified");
            }
            if(classes != null) {
                Indexer indexer = new Indexer();
                for (Class<?> cl : classes) {
                    indexer.indexClass(cl);
                }
                index = indexer.complete();
            }
            if(classLoader == null) {
                classLoader = Thread.currentThread().getContextClassLoader();
                if(classLoader == null) {
                    classLoader = getClass().getClassLoader();
                }
            }
            return new RootContext(index, Scope.APPLICATION, classLoader, scopeProvider);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Context of(Index index) {
        Arguments.notNull(index, "Index");
        try {
            return RootContext.builder().withIndex(index).build();
        } catch(IOException e) {
            throw new IllegalStateException(e); // this should never happen
        }
    }
}
