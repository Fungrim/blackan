package io.github.fungrim.blackan.injector.lookup;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.github.fungrim.blackan.injector.creator.ProviderFactory;
import io.github.fungrim.blackan.injector.util.QualifierUtil;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.inject.Provider;

public class RecursiveInstance implements LimitedInstance {

    private static final DotName DEFAULT = DotName.createSimple("jakarta.enterprise.inject.Default");
    private static final DotName ALTERNATIVE = DotName.createSimple("jakarta.enterprise.inject.Alternative");
    private static final DotName PRIORITY = DotName.createSimple("jakarta.annotation.Priority");

    private final RecursionKey key;
    private final Optional<ClassInfo> defaultCandidate;
    private final boolean ambiguous;
    private final ProviderFactory providerFactory;
    private final InstanceFactory instanceFactory;
    private final IndexView index;
    private final Set<DotName> vetoedTypes;

    public RecursiveInstance(RecursionKey key, ProviderFactory providerFactory, InstanceFactory instanceFactory, IndexView index, Set<DotName> vetoedTypes) {
        this.key = key;
        this.providerFactory = providerFactory;
        this.instanceFactory = instanceFactory;
        this.index = index;
        this.vetoedTypes = vetoedTypes;
        List<ClassInfo> candidates = computeCandidates();
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

        // Filter out disabled alternatives (those without @Priority)
        List<ClassInfo> nonAlternatives = candidates.stream()
                .filter(c -> !c.hasAnnotation(ALTERNATIVE))
                .toList();
        
        // Check for disabled alternatives - they cause ambiguity even though they can't win
        boolean hasDisabledAlternatives = candidates.stream()
                .anyMatch(c -> c.hasAnnotation(ALTERNATIVE) && !c.hasAnnotation(PRIORITY));
        
        // If there's exactly one bean with explicit @Default among non-alternatives, it wins
        // (explicit @Default beats disabled alternatives)
        List<ClassInfo> explicitDefaults = nonAlternatives.stream()
                .filter(c -> c.hasAnnotation(DEFAULT))
                .toList();
        if (explicitDefaults.size() == 1) {
            return Optional.of(explicitDefaults.get(0));
        }
        
        // If exactly one non-alternative candidate and no disabled alternatives, it wins
        // (disabled alternatives cause ambiguity with implicit @Default beans)
        if (nonAlternatives.size() == 1 && !hasDisabledAlternatives) {
            return Optional.of(nonAlternatives.get(0));
        }
        
        // Fall back to highest @Priority among non-alternative candidates
        return resolveByPriority(nonAlternatives);
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

    private List<ClassInfo> computeCandidates() {
        ClassInfo typeInfo = index.getClassByName(key.type());
        Collection<ClassInfo> allAssignables;
        boolean isConcreteClassLookup = false;
        if (typeInfo == null || typeInfo.isInterface()) {
            allAssignables = index.getAllKnownImplementations(key.type());
            if(allAssignables.isEmpty()) {
                allAssignables = index.getAllKnownSubclasses(key.type());
            }
        } else {
            List<ClassInfo> list = new ArrayList<>(index.getAllKnownSubclasses(key.type()));
            list.add(0, typeInfo);
            allAssignables = list;
            isConcreteClassLookup = true;
        }
        boolean lookupHasAny = QualifierUtil.hasAnyQualifier(key.qualifiers());
        boolean lookupRequiresDefault = QualifierUtil.shouldImplyDefault(key.qualifiers());
        final boolean concreteClassLookup = isConcreteClassLookup;
        
        return allAssignables.stream()
                .filter(c -> !vetoedTypes.contains(c.name()))
                .filter(c -> matchesCandidateQualifiers(c, key.qualifiers(), lookupHasAny, lookupRequiresDefault, concreteClassLookup))
                .toList();
    }

    private boolean matchesCandidateQualifiers(ClassInfo candidate, List<Annotation> lookupQualifiers, 
            boolean lookupHasAny, boolean lookupRequiresDefault, boolean concreteClassLookup) {
        if (lookupHasAny) {
            return true;
        }
        if (concreteClassLookup && candidate.name().equals(key.type())) {
            return true;
        }
        if (lookupRequiresDefault) {
            return QualifierUtil.beanMatchesDefault(candidate, index);
        }
        return matchesAllQualifiers(candidate, lookupQualifiers);
    }

    @Override
    public List<ClassInfo> candidates() {
        return computeCandidates().stream()
                .sorted((a, b) -> Integer.compare(priorityOf(a), priorityOf(b)))
                .toList();
    }

    private static int priorityOf(ClassInfo info) {
        AnnotationInstance ann = info.annotation(PRIORITY);
        return ann != null ? ann.value().asInt() : Integer.MAX_VALUE;
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
        return new LocalInstance<>(this, providerFactory, type);
    }
    
    public <U> LimitedInstance selectSubtype(Class<U> subtype, Annotation... qualifiers) {
        List<Annotation> combined = new ArrayList<>(key.qualifiers());
        combined.addAll(qualifiers == null ? List.of() : List.of(qualifiers));
        RecursionKey narrowedKey = RecursionKey.of(subtype, combined.toArray(new Annotation[0]));
        return instanceFactory.create(narrowedKey);
    }

    @Override
    public LimitedInstance select(Annotation... qualifiers) {
        List<Annotation> combined = new ArrayList<>(key.qualifiers());
        combined.addAll(qualifiers == null ? List.of() : List.of(qualifiers));
        RecursionKey narrowedKey = RecursionKey.of(key.type(), combined.toArray(new Annotation[0]));
        return instanceFactory.create(narrowedKey);
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
        return defaultCandidate.isEmpty() && !ambiguous;
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
