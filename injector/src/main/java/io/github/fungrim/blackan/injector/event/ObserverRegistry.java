package io.github.fungrim.blackan.injector.event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;

public class ObserverRegistry {

    private static final DotName OBSERVES = DotName.createSimple(Observes.class);
    private static final DotName OBSERVES_ASYNC = DotName.createSimple(ObservesAsync.class);

    private final List<ObserverMethod> observers = new ArrayList<>();

    public void scan(IndexView index) {
        scanAnnotation(index, OBSERVES, false);
        scanAnnotation(index, OBSERVES_ASYNC, true);
    }

    private void scanAnnotation(IndexView index, DotName annotation, boolean async) {
        for (AnnotationInstance instance : index.getAnnotations(annotation)) {
            if (instance.target().kind() != AnnotationTarget.Kind.METHOD_PARAMETER) {
                continue;
            }
            MethodParameterInfo paramInfo = instance.target().asMethodParameter();
            MethodInfo method = paramInfo.method();
            DotName eventType = method.parameterType(paramInfo.position()).name();
            List<AnnotationInstance> qualifiers = collectQualifiers(method, paramInfo.position());
            observers.add(new ObserverMethod(method, eventType, qualifiers, async));
        }
    }

    private static List<AnnotationInstance> collectQualifiers(MethodInfo method, int paramPosition) {
        return method.annotations().stream()
                .filter(a -> a.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER)
                .filter(a -> a.target().asMethodParameter().position() == paramPosition)
                .filter(a -> !a.name().equals(OBSERVES) && !a.name().equals(OBSERVES_ASYNC))
                .toList();
    }

    public List<ObserverMethod> matchSync(Object event, List<AnnotationInstance> qualifiers, Comparator<org.jboss.jandex.ClassInfo> ordering) {
        return match(event, qualifiers, false, ordering);
    }

    public List<ObserverMethod> matchAsync(Object event, List<AnnotationInstance> qualifiers, Comparator<org.jboss.jandex.ClassInfo> ordering) {
        return match(event, qualifiers, true, ordering);
    }

    private List<ObserverMethod> match(Object event, List<AnnotationInstance> qualifiers, boolean async, Comparator<org.jboss.jandex.ClassInfo> ordering) {
        DotName eventTypeName = DotName.createSimple(event.getClass());
        List<ObserverMethod> matched = observers.stream()
                .filter(o -> o.async() == async)
                .filter(o -> isAssignable(eventTypeName, o.eventType()))
                .filter(o -> matchesQualifiers(o.qualifiers(), qualifiers))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (ordering != null) {
            matched.sort(Comparator.comparing(o -> o.method().declaringClass(), ordering));
        }
        return matched;
    }

    private static boolean isAssignable(DotName actualType, DotName observedType) {
        return actualType.equals(observedType) || observedType.equals(DotName.createSimple(Object.class));
    }

    private static boolean matchesQualifiers(List<AnnotationInstance> observerQualifiers, List<AnnotationInstance> eventQualifiers) {
        if (observerQualifiers.isEmpty()) {
            return true;
        }
        for (AnnotationInstance required : observerQualifiers) {
            boolean found = eventQualifiers.stream()
                    .anyMatch(eq -> eq.name().equals(required.name()) && valuesMatch(required, eq));
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private static boolean valuesMatch(AnnotationInstance a, AnnotationInstance b) {
        if (a.values().isEmpty() && b.values().isEmpty()) {
            return true;
        }
        return a.values().stream().allMatch(av ->
                b.values().stream().anyMatch(bv ->
                        Objects.equals(av.name(), bv.name()) && Objects.equals(av.value(), bv.value())));
    }
}
