package io.github.fungrin.blackan.core.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.injector.lookup.LimitedInstance;
import io.github.fungrin.blackan.core.runtime.stubs.ApplicationService;
import io.github.fungrin.blackan.core.runtime.stubs.BootstrapHighPriority;
import io.github.fungrin.blackan.core.runtime.stubs.BootstrapLowPriority;
import io.github.fungrin.blackan.core.runtime.stubs.CoreService;
import io.github.fungrin.blackan.core.runtime.stubs.NoPriorityNoStage;

class StageAndPriorityComparatorTest {

    private static Index index;
    private final StageAndPriorityComparator comparator = new StageAndPriorityComparator();

    @BeforeAll
    static void buildIndex() throws IOException {
        Indexer indexer = new Indexer();
        for (Class<?> clazz : List.of(
                BootstrapHighPriority.class,
                BootstrapLowPriority.class,
                CoreService.class,
                ApplicationService.class,
                NoPriorityNoStage.class)) {
            indexer.indexClass(clazz);
        }
        index = indexer.complete();
    }

    private static LimitedInstance instanceOf(Class<?> clazz) {
        LimitedInstance instance = mock(LimitedInstance.class);
        ClassInfo classInfo = index.getClassByName(clazz);
        when(instance.isResolvable()).thenReturn(true);
        when(instance.getCandidate()).thenReturn(classInfo);
        return instance;
    }

    @Test
    void bootstrapStageComesBeforeCoreStage() {
        LimitedInstance bootstrap = instanceOf(BootstrapHighPriority.class);
        LimitedInstance core = instanceOf(CoreService.class);
        assertTrue(comparator.compare(bootstrap, core) < 0);
    }

    @Test
    void coreStageComesBeforeApplicationStage() {
        LimitedInstance core = instanceOf(CoreService.class);
        LimitedInstance app = instanceOf(ApplicationService.class);
        assertTrue(comparator.compare(core, app) < 0);
    }

    @Test
    void bootstrapStageComesBeforeApplicationStage() {
        LimitedInstance bootstrap = instanceOf(BootstrapHighPriority.class);
        LimitedInstance app = instanceOf(ApplicationService.class);
        assertTrue(comparator.compare(bootstrap, app) < 0);
    }

    @Test
    void sameStageOrderedByPriority() {
        LimitedInstance high = instanceOf(BootstrapHighPriority.class);
        LimitedInstance low = instanceOf(BootstrapLowPriority.class);
        assertTrue(comparator.compare(high, low) < 0);
    }

    @Test
    void missingStageDefaultsToApplication() {
        LimitedInstance noStage = instanceOf(ApplicationService.class);
        LimitedInstance noPriorityNoStage = instanceOf(NoPriorityNoStage.class);
        LimitedInstance core = instanceOf(CoreService.class);
        assertTrue(comparator.compare(core, noStage) < 0);
        assertTrue(comparator.compare(core, noPriorityNoStage) < 0);
    }

    @Test
    void missingPriorityDefaultsToMaxValue() {
        LimitedInstance withPriority = instanceOf(ApplicationService.class);
        LimitedInstance noPriority = instanceOf(NoPriorityNoStage.class);
        assertTrue(comparator.compare(withPriority, noPriority) < 0);
    }

    @Test
    void nonResolvableInstancesAreEqual() {
        LimitedInstance unresolvable1 = mock(LimitedInstance.class);
        LimitedInstance unresolvable2 = mock(LimitedInstance.class);
        when(unresolvable1.isResolvable()).thenReturn(false);
        when(unresolvable2.isResolvable()).thenReturn(false);
        assertEquals(0, comparator.compare(unresolvable1, unresolvable2));
    }

    @Test
    void fullSortOrderIsCorrect() {
        List<LimitedInstance> unsorted = List.of(
                instanceOf(NoPriorityNoStage.class),
                instanceOf(ApplicationService.class),
                instanceOf(CoreService.class),
                instanceOf(BootstrapLowPriority.class),
                instanceOf(BootstrapHighPriority.class));

        List<String> sorted = unsorted.stream()
                .sorted(comparator)
                .map(i -> i.getCandidate().name().local())
                .toList();

        assertEquals(List.of(
                "BootstrapHighPriority",
                "BootstrapLowPriority",
                "CoreService",
                "ApplicationService",
                "NoPriorityNoStage"), sorted);
    }
}
