package io.github.fungrim.blackan.injector.lookup;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.github.fungrim.blackan.injector.creator.ProviderFactory;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Provider;

public class RecursiveInstance implements LimitedInstance {

    private static final DotName DEFAULT = DotName.createSimple("jakarta.enterprise.inject.Default");
    private static final DotName ALTERNATIVE = DotName.createSimple("jakarta.enterprise.inject.Alternative");
    private static final DotName PRIORITY = DotName.createSimple("jakarta.annotation.Priority");

    private final RecursionKey key;
    private final Collection<ClassInfo> candidates;
    private final Optional<ClassInfo> defaultCandidate;
    private final boolean ambiguous;
    private final ProviderFactory providerFactory;
    private final InstanceFactory instanceFactory;

    public RecursiveInstance(RecursionKey key, Collection<ClassInfo> candidates, ProviderFactory providerFactory, InstanceFactory instanceFactory) {
        this.key = key;
        this.candidates = candidates;
        this.providerFactory = providerFactory;
        this.instanceFactory = instanceFactory;
        if (candidates.isEmpty()) {
            this.defaultCandidate = Optional.empty();
            this.ambiguous = false;
        } else if (candidates.size() == 1) {
            this.defaultCandidate = Optional.of(candidates.iterator().next());
            this.ambiguous = false;
        } else {
            // Multiple candidates — try to disambiguate
            this.defaultCandidate = resolveDefaultCandidate(candidates);
            this.ambiguous = defaultCandidate.isEmpty();
        }
    }

    private static Optional<ClassInfo> resolveDefaultCandidate(Collection<ClassInfo> candidates) {
        // Enabled @Alternative beans (with @Priority) take precedence over everything
        List<ClassInfo> enabledAlternatives = candidates.stream()
                .filter(c -> c.hasAnnotation(ALTERNATIVE) && c.hasAnnotation(PRIORITY))
                .toList();
        if (!enabledAlternatives.isEmpty()) {
            return resolveByPriority(enabledAlternatives);
        }

        // If there's a @Default bean and no enabled alternatives, it wins
        for (ClassInfo c : candidates) {
            if (c.hasAnnotation(DEFAULT)) {
                return Optional.of(c);
            }
        }
        // Fall back to highest @Priority among all candidates
        return resolveByPriority(candidates);
    }

    private static Optional<ClassInfo> resolveByPriority(Collection<ClassInfo> candidates) {
        ClassInfo highest = null;
        int highestPriority = Integer.MIN_VALUE;
        boolean tie = false;
        for (ClassInfo c : candidates) {
            AnnotationInstance priorityAnn = c.annotation(PRIORITY);
            if (priorityAnn != null) {
                int value = priorityAnn.value().asInt();
                if (value > highestPriority) {
                    highest = c;
                    highestPriority = value;
                    tie = false;
                } else if (value == highestPriority) {
                    tie = true;
                }
            }
        }
        if (highest != null && !tie) {
            return Optional.of(highest);
        }
        return Optional.empty();
    }

    @Override
    public Iterator<ClassInfo> candidates() {
        return candidates.stream().toList().iterator();
    }

    @Override
    public ClassInfo getCandidate() {
        checkGet();
        if(defaultCandidate.isPresent()) {
            return defaultCandidate.get();
        }
        return select().getCandidate();
    }

    private void checkGet() {
        if(isUnsatisfied() ) {
            throw new UnsatisfiedResolutionException("No candidates found for: " + key);
        }
        if(isAmbiguous()) {
            throw new AmbiguousResolutionException("Multiple candidates found for: " + key);
        }
    }

    @Override
    public <T> T get(Class<T> clazz) {
        return toProvider(clazz).get();
    }

    @Override
    public <T> Provider<T> toProvider(Class<T> type) {
        checkGet();
        if(defaultCandidate.isPresent()) {
            return providerFactory.create(defaultCandidate.get(), type);
        }
        return () -> select().get(type);
    }
    
    @Override
    public <T> Instance<T> toInstance(Class<T> type) {
        return new Instance<T>() {

            @Override
            public Iterator<T> iterator() {
                return StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(candidates(), Spliterator.ORDERED),
                        false).map(c -> providerFactory.create(c, type).get()).iterator();
            }

            @Override
            public T get() {
                return RecursiveInstance.this.get(type);
            }

            @Override
            public Instance<T> select(Annotation... qualifiers) {
                return RecursiveInstance.this.select(qualifiers).toInstance(type);
            }

            @Override
            public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
                throw new UnsupportedOperationException("Unimplemented method 'select'");
            }

            @Override
            public <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
                throw new UnsupportedOperationException("Unimplemented method 'select'");
            }

            @Override
            public boolean isUnsatisfied() {
                return RecursiveInstance.this.isUnsatisfied();
            }

            @Override
            public boolean isAmbiguous() {
                return RecursiveInstance.this.isAmbiguous();
            }

            @Override
            public void destroy(T instance) {
                throw new UnsupportedOperationException("Unimplemented method 'destroy'");
            }

            @Override
            public Handle<T> getHandle() {
                throw new UnsupportedOperationException("Unimplemented method 'getHandle'");
            }

            @Override
            public Iterable<? extends Handle<T>> handles() {
                throw new UnsupportedOperationException("Unimplemented method 'handles'");
            }
            
        };
    }
    
    @Override
    public LimitedInstance select(Annotation... qualifiers) {
        List<Annotation> combined = new ArrayList<>(key.qualifiers());
        combined.addAll(qualifiers == null ? List.of() : List.of(qualifiers));
        List<ClassInfo> filtered = candidates.stream()
                .filter(candidate -> matchesAllQualifiers(candidate, combined))
                .toList();
        RecursionKey narrowedKey = RecursionKey.of(key.type(), combined.toArray(new Annotation[combined.size()]));
        return instanceFactory.create(narrowedKey, filtered);
    }
    
    private boolean matchesAllQualifiers(ClassInfo candidate, List<Annotation> qualifiers) {
        for (Annotation qualifier : qualifiers) {
            if (!matchesQualifier(candidate, qualifier)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesQualifier(ClassInfo candidate, Annotation qualifier) {
        DotName annotationName = DotName.createSimple(qualifier.annotationType().getName());
        AnnotationInstance candidateAnn = candidate.annotation(annotationName);
        if (candidateAnn == null) {
            return false;
        }
        for (Method member : qualifier.annotationType().getDeclaredMethods()) {
            if (member.isAnnotationPresent(jakarta.enterprise.util.Nonbinding.class)) {
                continue;
            }
            Object expected = invokeMember(qualifier, member);
            org.jboss.jandex.AnnotationValue candidateValue = candidateAnn.value(member.getName());
            Object actual = candidateValue != null ? candidateValue.value() : defaultValue(member);
            if (!Objects.deepEquals(expected, actual)) {
                return false;
            }
        }
        return true;
    }

    private static Object defaultValue(Method member) {
        return member.getDefaultValue();
    }

    private static Object invokeMember(Annotation annotation, Method member) {
        try {
            return member.invoke(annotation);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to read qualifier member: " + member.getName(), e);
        }
    }

    @Override
    public boolean isUnsatisfied() {
        return candidates.isEmpty();
    }

    @Override
    public boolean isAmbiguous() {
        return ambiguous;
    }

    @Override
    public boolean isResolvable() {
        return !isUnsatisfied() && !isAmbiguous();
    }
}
