package io.github.fungrim.blackan.injector.creator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.creator.stubs.FieldInjectedBean;
import io.github.fungrim.blackan.injector.creator.stubs.FullInjectedBean;
import io.github.fungrim.blackan.injector.creator.stubs.MethodInjectedBean;
import io.github.fungrim.blackan.injector.creator.stubs.NoArgBean;
import io.github.fungrim.blackan.injector.lookup.LimitedInstance;

class DependentProviderTest {

    @SuppressWarnings("unchecked")
    private static Context mockContextForAny(Object... values) {
        Context context = mock(Context.class);
        when(context.destroyableTracker()).thenReturn(new DestroyableTracker());
        int[] index = {0};
        when(context.getInstance(any(DotName.class))).thenAnswer(invocation -> {
            LimitedInstance instance = mock(LimitedInstance.class);
            when(instance.get(any(Class.class))).thenReturn(values[index[0]++]);
            return instance;
        });
        when(context.loadClass(any(DotName.class))).thenAnswer(invocation -> {
            DotName name = invocation.getArgument(0);
            return Class.forName(name.toString());
        });
        return context;
    }

    @Nested
    class NoInjection {

        @Test
        void createsSimpleBean() {
            Context context = mock(Context.class);
            when(context.destroyableTracker()).thenReturn(new DestroyableTracker());
            var creator = new DependentProvider<>(context, mock(ClassInfo.class), NoArgBean.class);
            var result = creator.get();
            assertInstanceOf(NoArgBean.class, result);
        }
    }

    @Nested
    class FieldInjection {

        @Test
        void injectsField() {
            Context context = mockContextForAny("injected");
            var creator = new DependentProvider<>(context, mock(ClassInfo.class), FieldInjectedBean.class);
            FieldInjectedBean result = creator.get();
            assertEquals("injected", result.value);
        }
    }

    @Nested
    class MethodInjection {

        @Test
        void injectsViaMethod() {
            Context context = mockContextForAny("fromMethod");
            var creator = new DependentProvider<>(context, mock(ClassInfo.class), MethodInjectedBean.class);
            MethodInjectedBean result = creator.get();
            assertEquals("fromMethod", result.value);
        }
    }

    @Nested
    class CombinedInjection {

        @Test
        void injectsConstructorFieldAndMethod() {
            Context context = mockContextForAny("hello", 42, 99L);
            var creator = new DependentProvider<>(context, mock(ClassInfo.class), FullInjectedBean.class);
            FullInjectedBean result = creator.get();
            assertNotNull(result);
            assertEquals("hello", result.name);
            assertEquals(42, result.count);
            assertEquals(99L, result.id);
        }
    }
}
