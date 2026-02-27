package io.github.fungrin.blackan.core.runtime;

import java.util.Comparator;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;

import io.github.fungrim.blackan.common.api.BootStage;
import io.github.fungrim.blackan.common.api.Stage;
import io.github.fungrim.blackan.injector.lookup.LimitedInstance;
import jakarta.annotation.Priority;

public class StageAndPriorityComparator implements Comparator<LimitedInstance> {

    @Override
    public int compare(LimitedInstance o1, LimitedInstance o2) {
        if(!o1.isResolvable() || !o2.isResolvable()) {
            return 0;
        }
        ClassInfo c1 = o1.getCandidate();
        ClassInfo c2 = o2.getCandidate();
        Stage s1 = extractStage(c1);
        Stage s2 = extractStage(c2);
        if(s1.order().compareTo(s2.order()) == 0) {
            return extractPriority(c1).compareTo(extractPriority(c2));
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
