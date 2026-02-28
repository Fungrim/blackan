package io.github.fungrim.blackan.injector.event;

import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

public record ObserverMethod(
    MethodInfo method,
    DotName eventType,
    List<AnnotationInstance> qualifiers,
    boolean async
) {}
