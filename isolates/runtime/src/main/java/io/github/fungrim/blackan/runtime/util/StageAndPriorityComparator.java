package io.github.fungrim.blackan.runtime.util;

import java.util.Comparator;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;

import io.github.fungrim.blackan.common.api.BootStage;
import io.github.fungrim.blackan.common.api.Stage;
import jakarta.annotation.Priority;

public class StageAndPriorityComparator implements Comparator<ClassInfo> {

    @Override
    public int compare(ClassInfo o1, ClassInfo o2) {
        Stage s1 = extractStage(o1);
        Stage s2 = extractStage(o2);
        if(s1.order().compareTo(s2.order()) == 0) {
            return extractPriority(o1).compareTo(extractPriority(o2));
        } else {
            return s1.order().compareTo(s2.order());
        }
    }

    private Stage extractStage(ClassInfo c) {
        AnnotationInstance annotation = c.annotation(BootStage.class);
        return annotation == null ? Stage.APPLICATION : Stage.valueOf(annotation.value().asEnum());
    }

    private Integer extractPriority(ClassInfo c) {
        AnnotationInstance annotation = c.annotation(Priority.class);
        return annotation == null ? Integer.MAX_VALUE : annotation.value().asInt();
    }
}
