package io.github.fungrim.blackan.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.runtime.stubs.ApplicationService;
import io.github.fungrim.blackan.runtime.stubs.BootstrapHighPriority;
import io.github.fungrim.blackan.runtime.stubs.BootstrapLowPriority;
import io.github.fungrim.blackan.runtime.stubs.CoreService;
import io.github.fungrim.blackan.runtime.stubs.NoPriorityNoStage;
import io.github.fungrim.blackan.runtime.util.StageAndPriorityComparator;

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

    private static ClassInfo classInfoOf(Class<?> clazz) {
        return index.getClassByName(clazz);
    }

    @Test
    void bootstrapStageComesBeforeCoreStage() {
        assertTrue(comparator.compare(classInfoOf(BootstrapHighPriority.class), classInfoOf(CoreService.class)) < 0);
    }

    @Test
    void coreStageComesBeforeApplicationStage() {
        assertTrue(comparator.compare(classInfoOf(CoreService.class), classInfoOf(ApplicationService.class)) < 0);
    }

    @Test
    void bootstrapStageComesBeforeApplicationStage() {
        assertTrue(comparator.compare(classInfoOf(BootstrapHighPriority.class), classInfoOf(ApplicationService.class)) < 0);
    }

    @Test
    void sameStageOrderedByPriority() {
        assertTrue(comparator.compare(classInfoOf(BootstrapHighPriority.class), classInfoOf(BootstrapLowPriority.class)) < 0);
    }

    @Test
    void missingStageDefaultsToApplication() {
        assertTrue(comparator.compare(classInfoOf(CoreService.class), classInfoOf(ApplicationService.class)) < 0);
        assertTrue(comparator.compare(classInfoOf(CoreService.class), classInfoOf(NoPriorityNoStage.class)) < 0);
    }

    @Test
    void missingPriorityDefaultsToMaxValue() {
        assertTrue(comparator.compare(classInfoOf(ApplicationService.class), classInfoOf(NoPriorityNoStage.class)) < 0);
    }

    @Test
    void fullSortOrderIsCorrect() {
        List<ClassInfo> unsorted = List.of(
                classInfoOf(NoPriorityNoStage.class),
                classInfoOf(ApplicationService.class),
                classInfoOf(CoreService.class),
                classInfoOf(BootstrapLowPriority.class),
                classInfoOf(BootstrapHighPriority.class));

        List<String> sorted = unsorted.stream()
                .sorted(comparator)
                .map(c -> c.name().local())
                .toList();

        assertEquals(List.of(
                "BootstrapHighPriority",
                "BootstrapLowPriority",
                "CoreService",
                "ApplicationService",
                "NoPriorityNoStage"), sorted);
    }
}
