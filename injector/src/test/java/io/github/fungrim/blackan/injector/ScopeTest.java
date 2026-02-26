package io.github.fungrim.blackan.injector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.injector.creator.ConstructionException;
import io.github.fungrim.blackan.injector.stubs.ApplicationScopedBean;
import io.github.fungrim.blackan.injector.stubs.DependentBean;
import io.github.fungrim.blackan.injector.stubs.InjectionPointHolder;
import io.github.fungrim.blackan.injector.stubs.MultipleScopesBean;
import io.github.fungrim.blackan.injector.stubs.RequestScopedBean;
import io.github.fungrim.blackan.injector.stubs.SessionScopedBean;
import io.github.fungrim.blackan.injector.stubs.SingletonBean;
import io.github.fungrim.blackan.injector.stubs.UnannotatedBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Singleton;

class ScopeTest {

    @Nested
    class FromAnnotationType {

        @Test
        void resolvesApplicationScoped() {
            assertEquals(Optional.of(Scope.APPLICATION), Scope.from(ApplicationScoped.class));
        }

        @Test
        void resolvesSessionScoped() {
            assertEquals(Optional.of(Scope.SESSION), Scope.from(SessionScoped.class));
        }

        @Test
        void resolvesRequestScoped() {
            assertEquals(Optional.of(Scope.REQUEST), Scope.from(RequestScoped.class));
        }

        @Test
        void resolvesDependent() {
            assertEquals(Optional.of(Scope.DEPENDENT), Scope.from(Dependent.class));
        }

        @Test
        void resolvesSingleton() {
            assertEquals(Optional.of(Scope.SINGLETON), Scope.from(Singleton.class));
        }

        @Test
        void returnsEmptyForUnknownAnnotation() {
            assertEquals(Optional.empty(), Scope.from(Override.class));
        }
    }

    @Nested
    class BeanScope {

        @Test
        void resolvesApplicationScopedBean() {
            assertEquals(Optional.of(Scope.APPLICATION), Scope.of(ApplicationScopedBean.class));
        }

        @Test
        void resolvesSessionScopedBean() {
            assertEquals(Optional.of(Scope.SESSION), Scope.of(SessionScopedBean.class));
        }

        @Test
        void resolvesRequestScopedBean() {
            assertEquals(Optional.of(Scope.REQUEST), Scope.of(RequestScopedBean.class));
        }

        @Test
        void resolvesDependentBean() {
            assertEquals(Optional.of(Scope.DEPENDENT), Scope.of(DependentBean.class));
        }

        @Test
        void resolvesSingletonBean() {
            assertEquals(Optional.of(Scope.SINGLETON), Scope.of(SingletonBean.class));
        }

        @Test
        void defaultsToDependentForUnannotatedBean() {
            assertEquals(Optional.of(Scope.DEPENDENT), Scope.of(UnannotatedBean.class));
        }

        @Test
        void throwsOnMultipleScopes() {
            assertThrows(ConstructionException.class, () -> Scope.of(MultipleScopesBean.class));
        }
    }

    @Nested
    class FieldScope {

        @Test
        void resolvesScopedField() throws NoSuchFieldException {
            Field field = InjectionPointHolder.class.getField("requestScopedField");
            assertEquals(Optional.of(Scope.REQUEST), Scope.of(field));
        }

        @Test
        void returnsEmptyForUnscopedField() throws NoSuchFieldException {
            Field field = InjectionPointHolder.class.getField("unscopedField");
            assertEquals(Optional.empty(), Scope.of(field));
        }
    }

    @Nested
    class MethodScope {

        @Test
        void resolvesScopedMethod() throws NoSuchMethodException {
            Method method = InjectionPointHolder.class.getMethod("sessionScopedMethod", String.class);
            assertEquals(Optional.of(Scope.SESSION), Scope.of(method));
        }

        @Test
        void returnsEmptyForUnscopedMethod() throws NoSuchMethodException {
            Method method = InjectionPointHolder.class.getMethod("unscopedMethod", String.class);
            assertEquals(Optional.empty(), Scope.of(method));
        }
    }

    @Nested
    class ScopeProperties {

        @Test
        void applicationIsNormalScope() {
            assertTrue(Scope.APPLICATION.isNormalScope());
            assertFalse(Scope.APPLICATION.isPseudoScope());
            assertTrue(Scope.APPLICATION.isCachedInScope());
        }

        @Test
        void sessionIsNormalScope() {
            assertTrue(Scope.SESSION.isNormalScope());
            assertFalse(Scope.SESSION.isPseudoScope());
            assertTrue(Scope.SESSION.isCachedInScope());
        }

        @Test
        void requestIsNormalScope() {
            assertTrue(Scope.REQUEST.isNormalScope());
            assertFalse(Scope.REQUEST.isPseudoScope());
            assertTrue(Scope.REQUEST.isCachedInScope());
        }

        @Test
        void dependentIsPseudoScope() {
            assertFalse(Scope.DEPENDENT.isNormalScope());
            assertTrue(Scope.DEPENDENT.isPseudoScope());
            assertFalse(Scope.DEPENDENT.isCachedInScope());
        }

        @Test
        void singletonIsPseudoScope() {
            assertFalse(Scope.SINGLETON.isNormalScope());
            assertTrue(Scope.SINGLETON.isPseudoScope());
            assertTrue(Scope.SINGLETON.isCachedInScope());
        }
    }

    @Nested
    class ShouldYield {

        @Test
        void applicationYieldsToSession() {
            assertTrue(Scope.APPLICATION.shouldYield(Scope.SESSION));
        }

        @Test
        void sessionYieldsToRequest() {
            assertTrue(Scope.SESSION.shouldYield(Scope.REQUEST));
        }

        @Test
        void requestDoesNotYieldToApplication() {
            assertFalse(Scope.REQUEST.shouldYield(Scope.APPLICATION));
        }

        @Test
        void sameDoesNotYield() {
            assertFalse(Scope.SESSION.shouldYield(Scope.SESSION));
        }
    }
}
