package io.github.fungrim.blackan.injector;

import java.io.Closeable;
import java.util.Comparator;
import java.util.Optional;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;

import io.github.fungrim.blackan.common.util.Arguments;
import io.github.fungrim.blackan.injector.context.ClassAccessImpl;
import io.github.fungrim.blackan.injector.context.ClassInfoAccessImpl;
import io.github.fungrim.blackan.injector.context.ContextImpl;
import io.github.fungrim.blackan.injector.context.ProcessScopeProvider;
import io.github.fungrim.blackan.injector.context.RootContext;
import io.github.fungrim.blackan.injector.creator.ConstructionException;
import io.github.fungrim.blackan.injector.lookup.LimitedInstance;
import io.github.fungrim.blackan.injector.producer.ProducerRegistry;

public interface Context extends Closeable {

    public static interface ClassAccess {

        public static ClassAccess of(final Class<?> cl) {
            return new ClassAccessImpl(cl);
        }

        public static ClassAccess of(final ClassInfo info) {
            return new ClassInfoAccessImpl(info);
        }

        boolean isInterface();
    
        Class<?> load(ClassLoader loader);

        DotName name();

    }

    IndexView index();

    Optional<Context> parent();

    Scope scope();

    ClassLoader classLoader();

    ProcessScopeProvider processScopeProvider();

    ProducerRegistry producerRegistry();

    Optional<ClassAccess> findClass(DotName name);

    Comparator<ClassInfo> eventOrdering();

    @Override
    void close();

    public default Optional<ClassAccess> findClass(Class<?> type) {
        Arguments.notNull(type, "Type");
        return findClass(DotName.createSimple(type));
    }

    public static Context of(Index index) {
        Arguments.notNull(index, "Index");
        return RootContext.of(index);
    }

    public default Context subcontext(Scope scope) {
        Arguments.notNull(scope, "Scope");
        return new ContextImpl(index(), this, scope, classLoader(), processScopeProvider(), producerRegistry(), eventOrdering());
    }

    public default Context subcontext(Scope scope, ClassLoader classLoader) {
        Arguments.notNull(scope, "Scope");
        Arguments.notNull(classLoader, "ClassLoader");
        return new ContextImpl(index(), this, scope, classLoader, processScopeProvider(), producerRegistry(), eventOrdering());
    }

    public default Class<?> loadClass(Class<?> type) {
        Arguments.notNull(type, "Type");
        return loadClass(DotName.createSimple(type));
    }

    public default Class<?> loadClass(DotName clazz) {
        Arguments.notNull(clazz, "Class");
        return loadClass(findClass(clazz).orElseThrow(() -> new ConstructionException("Failed to find class: " + clazz.toString())));
    }

    public default Class<?> loadClass(ClassAccess access) {
        Arguments.notNull(access, "Access");
        return access.load(classLoader());
    }

    public default Context root() {
        Optional<Context> p = parent();
        if(p.isEmpty()) {
            return this;
        } else {
            Context root = p.get();
            while(root.parent().isPresent()) {
                root = root.parent().get();
            }
            return root;
        }
    }

    public default <T> T get(Class<T> type) {
        Arguments.notNull(type, "Type");
        return getInstance(type).get(type);
    }

    LimitedInstance getInstance(DotName type);

    LimitedInstance getInstance(ClassInfo type);

    default LimitedInstance getInstance(Class<?> type) {
        Arguments.notNull(type, "Type");
        return getInstance(DotName.createSimple(type));
    }
}
