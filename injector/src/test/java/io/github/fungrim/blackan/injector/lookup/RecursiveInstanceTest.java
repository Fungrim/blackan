package io.github.fungrim.blackan.injector.lookup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.injector.creator.InjectionLocation;
import io.github.fungrim.blackan.injector.creator.ProviderFactory;
import io.github.fungrim.blackan.injector.lookup.stubs.AlternativeBean;
import io.github.fungrim.blackan.injector.lookup.stubs.DefaultBean;
import io.github.fungrim.blackan.injector.lookup.stubs.DisabledAlternativeBean;
import io.github.fungrim.blackan.injector.lookup.stubs.FastServiceBean;
import io.github.fungrim.blackan.injector.lookup.stubs.HighPriorityAlternativeBean;
import io.github.fungrim.blackan.injector.lookup.stubs.HighPriorityBean;
import io.github.fungrim.blackan.injector.lookup.stubs.PlainBean;
import io.github.fungrim.blackan.injector.lookup.stubs.PlainBeanB;
import io.github.fungrim.blackan.injector.lookup.stubs.PriorityBean;
import io.github.fungrim.blackan.injector.lookup.stubs.SamePriorityAlternativeA;
import io.github.fungrim.blackan.injector.lookup.stubs.SamePriorityAlternativeB;
import io.github.fungrim.blackan.injector.lookup.stubs.SamePriorityBeanA;
import io.github.fungrim.blackan.injector.lookup.stubs.SamePriorityBeanB;
import io.github.fungrim.blackan.injector.lookup.stubs.ServiceType;
import io.github.fungrim.blackan.injector.lookup.stubs.SlowServiceBean;
import io.github.fungrim.blackan.injector.lookup.stubs.TestService;
import io.github.fungrim.blackan.injector.lookup.stubs.Tracked;
import io.github.fungrim.blackan.injector.lookup.stubs.TrackedFastBean;
import io.github.fungrim.blackan.injector.lookup.stubs.TrackedSlowBean;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.inject.Provider;

class RecursiveInstanceTest {

    private static Index index;

    private static final ProviderFactory CREATOR_FACTORY = new ProviderFactory() {
        @Override
        @SuppressWarnings("unchecked")
        public <T> Provider<T> create(ClassInfo type, Optional<InjectionLocation<T>> location, Class<T> clazzType) {
            return () -> {
                try {
                    return (T) Class.forName(type.name().toString()).getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        }

        @Override
        public void close() {}
    };

    private static final InstanceFactory INSTANCE_FACTORY = new InstanceFactory() {
        @Override
        public LimitedInstance create(RecursionKey key, Collection<ClassInfo> filteredCandidates) {
            return new RecursiveInstance(key, filteredCandidates, CREATOR_FACTORY, this);
        }
        @Override
        public void close() {}
    };

    @BeforeAll
    static void buildIndex() throws IOException {
        Indexer indexer = new Indexer();
        Class<?>[] classes = {
            PlainBean.class, PlainBeanB.class, DefaultBean.class,
            AlternativeBean.class, HighPriorityAlternativeBean.class,
            PriorityBean.class, HighPriorityBean.class,
            DisabledAlternativeBean.class,
            SamePriorityBeanA.class, SamePriorityBeanB.class,
            SamePriorityAlternativeA.class, SamePriorityAlternativeB.class,
            FastServiceBean.class, SlowServiceBean.class,
            TrackedFastBean.class, TrackedSlowBean.class,
        };
        for (Class<?> clazz : classes) {
            indexer.indexClass(clazz);
        }
        index = indexer.complete();
    }

    private static ClassInfo classInfo(Class<?> clazz) {
        return index.getClassByName(clazz);
    }

    private static RecursiveInstance instance(ClassInfo... candidates) {
        RecursionKey key = RecursionKey.of(TestService.class);
        return new RecursiveInstance(key, List.of(candidates), CREATOR_FACTORY, INSTANCE_FACTORY);
    }

    @Nested
    class NoCandidates {

        @Test
        void isUnsatisfied() {
            var inst = instance();
            assertTrue(inst.isUnsatisfied());
            assertFalse(inst.isAmbiguous());
            assertFalse(inst.isResolvable());
        }

        @Test
        void getThrowsUnsatisfied() {
            var inst = instance();
            assertThrows(UnsatisfiedResolutionException.class, () -> inst.get(TestService.class));
        }
    }

    @Nested
    class SingleCandidate {

        @Test
        void resolvesToOnlyCandidate() {
            var inst = instance(classInfo(PlainBean.class));
            assertFalse(inst.isUnsatisfied());
            assertFalse(inst.isAmbiguous());
            assertTrue(inst.isResolvable());
            assertTrue(inst.get(TestService.class) instanceof PlainBean);
        }

        @Test
        void singleDefaultBean() {
            var inst = instance(classInfo(DefaultBean.class));
            assertTrue(inst.isResolvable());
            assertTrue(inst.get(TestService.class) instanceof DefaultBean);
        }

        @Test
        void singleAlternativeBean() {
            var inst = instance(classInfo(AlternativeBean.class));
            assertTrue(inst.isResolvable());
            assertTrue(inst.get(TestService.class) instanceof AlternativeBean);
        }
    }

    @Nested
    class DefaultBeanResolution {

        @Test
        void defaultBeanWinsOverPlainBeans() {
            var inst = instance(classInfo(PlainBean.class), classInfo(DefaultBean.class));
            assertTrue(inst.isResolvable());
            assertTrue(inst.get(TestService.class) instanceof DefaultBean);
        }

        @Test
        void defaultBeanWinsOverDisabledAlternative() {
            var inst = instance(classInfo(DefaultBean.class), classInfo(DisabledAlternativeBean.class));
            assertTrue(inst.isResolvable());
            assertTrue(inst.get(TestService.class) instanceof DefaultBean);
        }
    }

    @Nested
    class AlternativeResolution {

        @Test
        void enabledAlternativeOverridesDefaultBean() {
            var inst = instance(classInfo(DefaultBean.class), classInfo(AlternativeBean.class));
            assertTrue(inst.isResolvable());
            assertTrue(inst.get(TestService.class) instanceof AlternativeBean);
        }

        @Test
        void enabledAlternativeOverridesPlainBean() {
            var inst = instance(classInfo(PlainBean.class), classInfo(AlternativeBean.class));
            assertTrue(inst.isResolvable());
            assertTrue(inst.get(TestService.class) instanceof AlternativeBean);
        }

        @Test
        void highestPriorityAlternativeWins() {
            var inst = instance(
                    classInfo(AlternativeBean.class),
                    classInfo(HighPriorityAlternativeBean.class));
            assertTrue(inst.isResolvable());
            assertTrue(inst.get(TestService.class) instanceof HighPriorityAlternativeBean);
        }

        @Test
        void highestPriorityAlternativeWinsOverDefaultAndPlain() {
            var inst = instance(
                    classInfo(DefaultBean.class),
                    classInfo(PlainBean.class),
                    classInfo(AlternativeBean.class),
                    classInfo(HighPriorityAlternativeBean.class));
            assertTrue(inst.isResolvable());
            assertTrue(inst.get(TestService.class) instanceof HighPriorityAlternativeBean);
        }

        @Test
        void disabledAlternativeDoesNotParticipate() {
            var inst = instance(
                    classInfo(PlainBean.class),
                    classInfo(DisabledAlternativeBean.class));
            // DisabledAlternative has no @Priority so doesn't count as enabled
            // Two candidates, no disambiguation possible
            assertTrue(inst.isAmbiguous());
            assertThrows(AmbiguousResolutionException.class, () -> inst.get(TestService.class));
        }

        @Test
        void samePriorityAlternativesAreAmbiguous() {
            var inst = instance(
                    classInfo(SamePriorityAlternativeA.class),
                    classInfo(SamePriorityAlternativeB.class));
            assertTrue(inst.isAmbiguous());
            assertThrows(AmbiguousResolutionException.class, () -> inst.get(TestService.class));
        }
    }

    @Nested
    class PriorityResolution {

        @Test
        void highestPriorityWinsAmongPlainBeans() {
            var inst = instance(
                    classInfo(PriorityBean.class),
                    classInfo(HighPriorityBean.class));
            assertTrue(inst.isResolvable());
            assertTrue(inst.get(TestService.class) instanceof HighPriorityBean);
        }

        @Test
        void priorityFallbackWhenNoDefaultOrAlternative() {
            var inst = instance(
                    classInfo(PlainBean.class),
                    classInfo(PriorityBean.class),
                    classInfo(HighPriorityBean.class));
            assertTrue(inst.isResolvable());
            assertTrue(inst.get(TestService.class) instanceof HighPriorityBean);
        }

        @Test
        void samePriorityBeansAreAmbiguous() {
            var inst = instance(
                    classInfo(SamePriorityBeanA.class),
                    classInfo(SamePriorityBeanB.class));
            assertTrue(inst.isAmbiguous());
            assertThrows(AmbiguousResolutionException.class, () -> inst.get(TestService.class));
        }
    }

    @Nested
    class AmbiguousCases {

        @Test
        void multiplePlainBeansAreAmbiguous() {
            var inst = instance(classInfo(PlainBean.class), classInfo(PlainBeanB.class));
            assertTrue(inst.isAmbiguous());
            assertFalse(inst.isResolvable());
            assertThrows(AmbiguousResolutionException.class, () -> inst.get(TestService.class));
        }

        @Test
        void plainBeanAndDisabledAlternativeAreAmbiguous() {
            var inst = instance(
                    classInfo(PlainBean.class),
                    classInfo(DisabledAlternativeBean.class));
            assertTrue(inst.isAmbiguous());
        }
    }

    @Nested
    class CustomQualifierResolution {

        @Test
        void selectByCustomQualifierValue() {
            var inst = instance(
                    classInfo(FastServiceBean.class),
                    classInfo(SlowServiceBean.class));
            LimitedInstance selected = inst.select(ServiceType.Literal.of("fast"));
            assertTrue(selected.isResolvable());
            assertTrue(selected.get(TestService.class) instanceof FastServiceBean);
        }

        @Test
        void selectByCustomQualifierDifferentValue() {
            var inst = instance(
                    classInfo(FastServiceBean.class),
                    classInfo(SlowServiceBean.class));
            LimitedInstance selected = inst.select(ServiceType.Literal.of("slow"));
            assertTrue(selected.isResolvable());
            assertTrue(selected.get(TestService.class) instanceof SlowServiceBean);
        }

        @Test
        void selectByCustomQualifierNoMatch() {
            var inst = instance(
                    classInfo(FastServiceBean.class),
                    classInfo(SlowServiceBean.class));
            LimitedInstance selected = inst.select(ServiceType.Literal.of("unknown"));
            assertTrue(selected.isUnsatisfied());
        }

        @Test
        void nonbindingMemberIsIgnoredDuringMatch() {
            var inst = instance(
                    classInfo(TrackedFastBean.class),
                    classInfo(TrackedSlowBean.class));
            // TrackedFastBean has description="a fast tracked bean" but @Nonbinding should ignore it
            LimitedInstance selected = inst.select(Tracked.Literal.of("fast", "different description"));
            assertTrue(selected.isResolvable());
            assertTrue(selected.get(TestService.class) instanceof TrackedFastBean);
        }

        @Test
        void bindingMemberStillMatchedWithNonbindingPresent() {
            var inst = instance(
                    classInfo(TrackedFastBean.class),
                    classInfo(TrackedSlowBean.class));
            LimitedInstance selected = inst.select(Tracked.Literal.of("slow"));
            assertTrue(selected.isResolvable());
            assertTrue(selected.get(TestService.class) instanceof TrackedSlowBean);
        }

        @Test
        void nonMatchingBindingMemberIsUnsatisfied() {
            var inst = instance(
                    classInfo(TrackedFastBean.class),
                    classInfo(TrackedSlowBean.class));
            LimitedInstance selected = inst.select(Tracked.Literal.of("unknown"));
            assertTrue(selected.isUnsatisfied());
        }
    }

    @Nested
    class SelectMethod {

        @Test
        void selectNarrowsCandidates() {
            var inst = instance(
                    classInfo(DefaultBean.class),
                    classInfo(PlainBean.class));
            LimitedInstance selected = inst.select(Default.Literal.INSTANCE);
            assertTrue(selected.isResolvable());
            assertFalse(selected.isAmbiguous());
            assertTrue(selected.get(TestService.class) instanceof DefaultBean);
        }

        @Test
        void selectWithNoMatchingQualifierIsUnsatisfied() {
            var inst = instance(classInfo(PlainBean.class));
            LimitedInstance selected = inst.select(Default.Literal.INSTANCE);
            assertTrue(selected.isUnsatisfied());
        }

        @Test
        void selectWithNoQualifiersReturnsSameResolution() {
            var inst = instance(classInfo(DefaultBean.class));
            LimitedInstance selected = inst.select();
            assertTrue(selected.isResolvable());
            assertTrue(selected.get(TestService.class) instanceof DefaultBean);
        }
    }
}
