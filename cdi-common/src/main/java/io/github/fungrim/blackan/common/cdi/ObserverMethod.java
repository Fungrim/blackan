package io.github.fungrim.blackan.common.cdi;

import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

public record ObserverMethod(
    MethodInfo method,
    DotName eventType,
    List<AnnotationInstance> qualifiers,
    boolean async,
    int priority
) {}
