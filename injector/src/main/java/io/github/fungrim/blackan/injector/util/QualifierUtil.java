package io.github.fungrim.blackan.injector.util;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Named;
import jakarta.inject.Qualifier;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QualifierUtil {

    public static final DotName DEFAULT_NAME = DotName.createSimple(Default.class);
    public static final DotName ANY_NAME = DotName.createSimple(Any.class);
    public static final DotName NAMED_NAME = DotName.createSimple(Named.class);
    public static final DotName QUALIFIER_NAME = DotName.createSimple(Qualifier.class);

    private static final Set<DotName> BUILT_IN_QUALIFIERS = Set.of(
            DEFAULT_NAME, ANY_NAME, NAMED_NAME
    );

    private static final Set<DotName> KNOWN_NON_QUALIFIERS = Set.of(
            DotName.createSimple("jakarta.enterprise.context.ApplicationScoped"),
            DotName.createSimple("jakarta.enterprise.context.RequestScoped"),
            DotName.createSimple("jakarta.enterprise.context.SessionScoped"),
            DotName.createSimple("jakarta.enterprise.context.Dependent"),
            DotName.createSimple("jakarta.inject.Singleton"),
            DotName.createSimple("jakarta.inject.Inject"),
            DotName.createSimple("jakarta.enterprise.inject.Produces"),
            DotName.createSimple("jakarta.enterprise.inject.Alternative"),
            DotName.createSimple("jakarta.enterprise.inject.Vetoed"),
            DotName.createSimple("jakarta.annotation.Priority"),
            DotName.createSimple("jakarta.annotation.PostConstruct"),
            DotName.createSimple("jakarta.annotation.PreDestroy")
    );

    public static boolean isDefaultQualifier(Annotation annotation) {
        return annotation.annotationType() == Default.class;
    }

    public static boolean isAnyQualifier(Annotation annotation) {
        return annotation.annotationType() == Any.class;
    }

    public static boolean isNamedQualifier(Annotation annotation) {
        return annotation.annotationType() == Named.class;
    }

    public static boolean hasDefaultQualifier(List<Annotation> qualifiers) {
        return qualifiers.stream().anyMatch(QualifierUtil::isDefaultQualifier);
    }

    public static boolean hasAnyQualifier(List<Annotation> qualifiers) {
        return qualifiers.stream().anyMatch(QualifierUtil::isAnyQualifier);
    }

    public static boolean hasNamedQualifier(List<Annotation> qualifiers) {
        return qualifiers.stream().anyMatch(QualifierUtil::isNamedQualifier);
    }

    public static boolean hasCustomQualifier(List<Annotation> qualifiers) {
        for (Annotation a : qualifiers) {
            if (!isDefaultQualifier(a) && !isAnyQualifier(a) && !isNamedQualifier(a)) {
                if (a.annotationType().isAnnotationPresent(Qualifier.class)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean shouldImplyDefault(List<Annotation> qualifiers) {
        if (hasAnyQualifier(qualifiers)) {
            return false;
        }
        if (hasDefaultQualifier(qualifiers)) {
            return false;
        }
        return !hasCustomQualifier(qualifiers);
    }

    public static List<Annotation> normalizeForLookup(List<Annotation> qualifiers) {
        if (hasAnyQualifier(qualifiers)) {
            return qualifiers;
        }
        if (hasDefaultQualifier(qualifiers)) {
            return qualifiers;
        }
        if (hasCustomQualifier(qualifiers)) {
            return qualifiers;
        }
        List<Annotation> normalized = new ArrayList<>(qualifiers);
        normalized.add(Default.Literal.INSTANCE);
        return normalized;
    }

    public static boolean beanHasExplicitCustomQualifier(ClassInfo classInfo, IndexView index) {
        for (AnnotationInstance ann : classInfo.declaredAnnotations()) {
            DotName name = ann.name();
            if (name.equals(DEFAULT_NAME) || name.equals(ANY_NAME) || name.equals(NAMED_NAME)) {
                continue;
            }
            if (isQualifierAnnotation(name, index)) {
                return true;
            }
        }
        return false;
    }

    public static boolean beanHasImplicitDefault(ClassInfo classInfo, IndexView index) {
        if (classInfo.hasAnnotation(DEFAULT_NAME)) {
            return true;
        }
        return !beanHasExplicitCustomQualifier(classInfo, index);
    }

    public static boolean beanMatchesDefault(ClassInfo classInfo, IndexView index) {
        return beanHasImplicitDefault(classInfo, index);
    }

    private static boolean isQualifierAnnotation(DotName name, IndexView index) {
        if (BUILT_IN_QUALIFIERS.contains(name)) {
            return true;
        }
        if (KNOWN_NON_QUALIFIERS.contains(name)) {
            return false;
        }
        ClassInfo annotationClass = index.getClassByName(name);
        if (annotationClass != null) {
            return annotationClass.hasAnnotation(QUALIFIER_NAME);
        }
        try {
            Class<?> clazz = Class.forName(name.toString());
            return clazz.isAnnotationPresent(Qualifier.class);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean hasDefaultQualifierJandex(List<AnnotationInstance> qualifiers) {
        return qualifiers.stream().anyMatch(a -> a.name().equals(DEFAULT_NAME));
    }

    public static boolean hasAnyQualifierJandex(List<AnnotationInstance> qualifiers) {
        return qualifiers.stream().anyMatch(a -> a.name().equals(ANY_NAME));
    }

    public static List<AnnotationInstance> normalizeEventQualifiersForMatching(List<AnnotationInstance> eventQualifiers) {
        if (hasAnyQualifierJandex(eventQualifiers)) {
            return eventQualifiers;
        }
        if (eventQualifiers.isEmpty() || !hasCustomQualifierJandex(eventQualifiers)) {
            List<AnnotationInstance> normalized = new ArrayList<>(eventQualifiers);
            if (!hasDefaultQualifierJandex(eventQualifiers)) {
                normalized.add(AnnotationInstance.create(DEFAULT_NAME, null, List.of()));
            }
            return normalized;
        }
        return eventQualifiers;
    }

    private static boolean hasCustomQualifierJandex(List<AnnotationInstance> qualifiers) {
        for (AnnotationInstance a : qualifiers) {
            DotName name = a.name();
            if (!BUILT_IN_QUALIFIERS.contains(name)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasCustomQualifierJandex(List<AnnotationInstance> qualifiers, IndexView index) {
        for (AnnotationInstance a : qualifiers) {
            DotName name = a.name();
            if (!BUILT_IN_QUALIFIERS.contains(name) && isQualifierAnnotation(name, index)) {
                return true;
            }
        }
        return false;
    }
}
