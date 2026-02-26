package io.github.fungrim.blackan.injector.creator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.creator.stubs.InjectConstructorBean;
import io.github.fungrim.blackan.injector.creator.stubs.MultiParamBean;
import io.github.fungrim.blackan.injector.creator.stubs.MultipleInjectBean;
import io.github.fungrim.blackan.injector.creator.stubs.NoArgBean;
import io.github.fungrim.blackan.injector.creator.stubs.NoConstructorMatchBean;
import io.github.fungrim.blackan.injector.lookup.LimitedInstance;

class ConstructorInvocationTest {

    @SuppressWarnings("unchecked")
    private static Context mockContextForAny(Object... values) {
        Context context = mock(Context.class);
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
    class DefaultConstructor {

        @Test
        void createsInstanceWithNoArgConstructor() {
            Context context = mock(Context.class);
            var invocation = ConstructorInvocation.of(context, NoArgBean.class);
            var result = invocation.create();
            assertInstanceOf(NoArgBean.class, result);
        }
    }

    @Nested
    class InjectConstructor {

        @Test
        void createsInstanceWithSingleInjectedParam() {
            Context context = mockContextForAny("hello");
            var invocation = ConstructorInvocation.of(context, InjectConstructorBean.class);
            InjectConstructorBean result = invocation.create();
            assertEquals("hello", result.value);
        }

        @Test
        void createsInstanceWithMultipleInjectedParams() {
            Context context = mockContextForAny("world", 42);
            var invocation = ConstructorInvocation.of(context, MultiParamBean.class);
            MultiParamBean result = invocation.create();
            assertEquals("world", result.name);
            assertEquals(42, result.count);
        }
    }

    @Nested
    class ErrorCases {

        @Test
        void throwsOnMultipleInjectConstructors() {
            Context context = mock(Context.class);
            assertThrows(ConstructionException.class,
                    () -> ConstructorInvocation.of(context, MultipleInjectBean.class));
        }

        @Test
        void throwsWhenNoSuitableConstructor() {
            Context context = mock(Context.class);
            assertThrows(ConstructionException.class,
                    () -> ConstructorInvocation.of(context, NoConstructorMatchBean.class));
        }
    }
}
