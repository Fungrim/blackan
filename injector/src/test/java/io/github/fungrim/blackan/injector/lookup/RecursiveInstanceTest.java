package io.github.fungrim.blackan.injector.lookup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.injector.context.DecoratedInstance;
import io.github.fungrim.blackan.injector.creator.InjectionLocation;
import io.github.fungrim.blackan.injector.creator.ProviderFactory;
import io.github.fungrim.blackan.injector.lookup.stubs.AbstractTestService;
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
import io.github.fungrim.blackan.injector.lookup.stubs.TestServiceSubclass;
import io.github.fungrim.blackan.injector.lookup.stubs.Tracked;
import io.github.fungrim.blackan.injector.lookup.stubs.TrackedFastBean;
import io.github.fungrim.blackan.injector.lookup.stubs.TrackedSlowBean;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.inject.Provider;

class RecursiveInstanceTest {

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
        public <T> DecoratedInstance<T> decorate(T instance) {
            return null;
        }

        @Override
        public void evict(DotName type) {}

        @Override
        public void close() {}
    };

    private static RecursiveInstance instance(Class<?>... stubClasses) {
        return createInstance(TestService.class, stubClasses);
    }

    private static RecursiveInstance abstractInstance(Class<?>... stubClasses) {
        return createInstance(AbstractTestService.class, stubClasses);
    }

    private static RecursiveInstance createInstance(Class<?> keyClass, Class<?>... stubClasses) {
        Indexer indexer = new Indexer();
        for (Class<?> clazz : stubClasses) {
            try {
                indexer.indexClass(clazz);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Index localIndex = indexer.complete();
        InjectionPointLookupKey key = InjectionPointLookupKey.of(keyClass);
        InstanceFactory localFactory = new InstanceFactory() {
            @Override
            public LimitedInstance create(InjectionPointLookupKey k) {
                return new RecursiveInstance(k, CREATOR_FACTORY, this, localIndex, Set.of());
            }
            @Override public void evict(DotName t) {}
            @Override public void close() {}
            @Override public <T> DecoratedInstance<T> decorate(T instance) { return null; }
        };
        return new RecursiveInstance(key, CREATOR_FACTORY, localFactory, localIndex, Set.of());
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
            var inst = instance(PlainBean.class);
            assertFalse(inst.isUnsatisfied());
            assertFalse(inst.isAmbiguous());
            assertTrue(inst.isResolvable());
            assertTrue(inst.get(TestService.class) instanceof PlainBean);
        }

        @Test
        void singleDefaultBean() {
            var inst = instance(DefaultBean.class);
            assertTrue(inst.isResolvable());
            assertTrue(inst.get(TestService.class) instanceof DefaultBean);
        }

        @Test
        void singleAlternativeBean() {
            var inst = instance(AlternativeBean.class);
            assertTrue(inst.isResolvable());
            assertTrue(inst.get(TestService.class) instanceof AlternativeBean);
        }
    }

    @Nested
    class DefaultBeanResolution {

        @Test
        void defaultBeanWinsOverPlainBeans() {
            var inst = instance(PlainBean.class, DefaultBean.class);
            assertTrue(inst.isResolvable());
            assertTrue(inst.get(TestService.class) instanceof DefaultBean);
        }

        @Test
        void defaultBeanWinsOverDisabledAlternative() {
            var inst = instance(DefaultBean.class, DisabledAlternativeBean.class);
            assertTrue(inst.isResolvable());
            assertTrue(inst.get(TestService.class) instanceof DefaultBean);
        }
    }

    @Nested
    class AlternativeResolution {

        @Test
        void enabledAlternativeOverridesDefaultBean() {
            var inst = instance(DefaultBean.class, AlternativeBean.class);
            assertTrue(inst.isResolvable());
            assertTrue(inst.get(TestService.class) instanceof AlternativeBean);
        }

        @Test
        void enabledAlternativeOverridesPlainBean() {
            var inst = instance(PlainBean.class, AlternativeBean.class);
            assertTrue(inst.isResolvable());
            assertTrue(inst.get(TestService.class) instanceof AlternativeBean);
        }

        @Test
        void highestPriorityAlternativeWins() {
            var inst = instance(
                    AlternativeBean.class,
                    HighPriorityAlternativeBean.class);
            assertTrue(inst.isResolvable());
            assertTrue(inst.get(TestService.class) instanceof HighPriorityAlternativeBean);
        }

        @Test
        void highestPriorityAlternativeWinsOverDefaultAndPlain() {
            var inst = instance(
                    DefaultBean.class,
                    PlainBean.class,
                    AlternativeBean.class,
                    HighPriorityAlternativeBean.class);
            assertTrue(inst.isResolvable());
            assertTrue(inst.get(TestService.class) instanceof HighPriorityAlternativeBean);
        }

        @Test
        void disabledAlternativeDoesNotParticipate() {
            var inst = instance(
                    PlainBean.class,
                    DisabledAlternativeBean.class);
            // DisabledAlternative has no @Priority so doesn't count as enabled
            // Two candidates, no disambiguation possible
            assertTrue(inst.isAmbiguous());
            assertThrows(AmbiguousResolutionException.class, () -> inst.get(TestService.class));
        }

        @Test
        void samePriorityAlternativesAreAmbiguous() {
            var inst = instance(
                    SamePriorityAlternativeA.class,
                    SamePriorityAlternativeB.class);
            assertTrue(inst.isAmbiguous());
            assertThrows(AmbiguousResolutionException.class, () -> inst.get(TestService.class));
        }
    }

    @Nested
    class PriorityResolution {

        @Test
        void highestPriorityWinsAmongPlainBeans() {
            var inst = instance(
                    PriorityBean.class,
                    HighPriorityBean.class);
            assertTrue(inst.isResolvable());
            assertTrue(inst.get(TestService.class) instanceof HighPriorityBean);
        }

        @Test
        void priorityFallbackWhenNoDefaultOrAlternative() {
            var inst = instance(
                    PlainBean.class,
                    PriorityBean.class,
                    HighPriorityBean.class);
            assertTrue(inst.isResolvable());
            assertTrue(inst.get(TestService.class) instanceof HighPriorityBean);
        }

        @Test
        void samePriorityBeansAreAmbiguous() {
            var inst = instance(
                    SamePriorityBeanA.class,
                    SamePriorityBeanB.class);
            assertTrue(inst.isAmbiguous());
            assertThrows(AmbiguousResolutionException.class, () -> inst.get(TestService.class));
        }
    }

    @Nested
    class AmbiguousCases {

        @Test
        void multiplePlainBeansAreAmbiguous() {
            var inst = instance(PlainBean.class, PlainBeanB.class);
            assertTrue(inst.isAmbiguous());
            assertFalse(inst.isResolvable());
            assertThrows(AmbiguousResolutionException.class, () -> inst.get(TestService.class));
        }

        @Test
        void plainBeanAndDisabledAlternativeAreAmbiguous() {
            var inst = instance(
                    PlainBean.class,
                    DisabledAlternativeBean.class);
            assertTrue(inst.isAmbiguous());
        }
    }

    @Nested
    class CustomQualifierResolution {

        @Test
        void selectByCustomQualifierValue() {
            var inst = instance(
                    FastServiceBean.class,
                    SlowServiceBean.class);
            LimitedInstance selected = inst.select(ServiceType.Literal.of("fast"));
            assertTrue(selected.isResolvable());
            assertTrue(selected.get(TestService.class) instanceof FastServiceBean);
        }

        @Test
        void selectByCustomQualifierDifferentValue() {
            var inst = instance(
                    FastServiceBean.class,
                    SlowServiceBean.class);
            LimitedInstance selected = inst.select(ServiceType.Literal.of("slow"));
            assertTrue(selected.isResolvable());
            assertTrue(selected.get(TestService.class) instanceof SlowServiceBean);
        }

        @Test
        void selectByCustomQualifierNoMatch() {
            var inst = instance(
                    FastServiceBean.class,
                    SlowServiceBean.class);
            LimitedInstance selected = inst.select(ServiceType.Literal.of("unknown"));
            assertTrue(selected.isUnsatisfied());
        }

        @Test
        void nonbindingMemberIsIgnoredDuringMatch() {
            var inst = instance(
                    TrackedFastBean.class,
                    TrackedSlowBean.class);
            // TrackedFastBean has description="a fast tracked bean" but @Nonbinding should ignore it
            LimitedInstance selected = inst.select(Tracked.Literal.of("fast", "different description"));
            assertTrue(selected.isResolvable());
            assertTrue(selected.get(TestService.class) instanceof TrackedFastBean);
        }

        @Test
        void bindingMemberStillMatchedWithNonbindingPresent() {
            var inst = instance(
                    TrackedFastBean.class,
                    TrackedSlowBean.class);
            LimitedInstance selected = inst.select(Tracked.Literal.of("slow"));
            assertTrue(selected.isResolvable());
            assertTrue(selected.get(TestService.class) instanceof TrackedSlowBean);
        }

        @Test
        void nonMatchingBindingMemberIsUnsatisfied() {
            var inst = instance(
                    TrackedFastBean.class,
                    TrackedSlowBean.class);
            LimitedInstance selected = inst.select(Tracked.Literal.of("unknown"));
            assertTrue(selected.isUnsatisfied());
        }
    }

    @Nested
    class SelectMethod {

        @Test
        void selectNarrowsCandidates() {
            var inst = instance(
                    DefaultBean.class,
                    PlainBean.class);
            LimitedInstance selected = inst.select(Default.Literal.INSTANCE);
            assertTrue(selected.isResolvable());
            assertFalse(selected.isAmbiguous());
            assertTrue(selected.get(TestService.class) instanceof DefaultBean);
        }

        @Test
        void selectWithNoMatchingQualifierIsUnsatisfied() {
            var inst = instance(PlainBean.class);
            LimitedInstance selected = inst.select(Default.Literal.INSTANCE);
            assertTrue(selected.isUnsatisfied());
        }

        @Test
        void selectWithNoQualifiersReturnsSameResolution() {
            var inst = instance(DefaultBean.class);
            LimitedInstance selected = inst.select();
            assertTrue(selected.isResolvable());
            assertTrue(selected.get(TestService.class) instanceof DefaultBean);
        }
    }

    @Nested
    class AbstractBeanSelection {
     
        @Test
        void selectAbstractBeanReturnsConcreteImplementation() {
            var inst = abstractInstance(TestServiceSubclass.class);
            LimitedInstance selected = inst.select();
            assertTrue(selected.isResolvable());
            assertTrue(selected.get(AbstractTestService.class) instanceof TestServiceSubclass);
        }
    }
}
