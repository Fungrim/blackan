package io.github.fungrim.blackan.injector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.injector.context.RootContext;
import io.github.fungrim.blackan.injector.creator.ConstructionException;
import io.github.fungrim.blackan.injector.util.stubs.AmbiguousInstanceBean;
import io.github.fungrim.blackan.injector.util.stubs.AppGreeting;
import io.github.fungrim.blackan.injector.util.stubs.AppService;
import io.github.fungrim.blackan.injector.util.stubs.AuditService;
import io.github.fungrim.blackan.injector.util.stubs.CircularConstructorA;
import io.github.fungrim.blackan.injector.util.stubs.CircularConstructorB;
import io.github.fungrim.blackan.injector.util.stubs.CircularFieldA;
import io.github.fungrim.blackan.injector.util.stubs.CircularFieldB;
import io.github.fungrim.blackan.injector.util.stubs.CircularProviderA;
import io.github.fungrim.blackan.injector.util.stubs.CircularProviderB;
import io.github.fungrim.blackan.injector.util.stubs.EmailNotificationService;
import io.github.fungrim.blackan.injector.util.stubs.GenericInjectionBean;
import io.github.fungrim.blackan.injector.util.stubs.Greeting;
import io.github.fungrim.blackan.injector.util.stubs.HighPriorityService;
import io.github.fungrim.blackan.injector.util.stubs.IllegalAppBean;
import io.github.fungrim.blackan.injector.util.stubs.ListProducer;
import io.github.fungrim.blackan.injector.util.stubs.LowPriorityService;
import io.github.fungrim.blackan.injector.util.stubs.NoPriorityService;
import io.github.fungrim.blackan.injector.util.stubs.NonResolvableBean;
import io.github.fungrim.blackan.injector.util.stubs.NonResolvableProviderBean;
import io.github.fungrim.blackan.injector.util.stubs.NotificationService;
import io.github.fungrim.blackan.injector.util.stubs.PriorityService;
import io.github.fungrim.blackan.injector.util.stubs.RequestHandler;
import io.github.fungrim.blackan.injector.util.stubs.RequestInfo;
import io.github.fungrim.blackan.injector.util.stubs.RequestInfoImpl;
import io.github.fungrim.blackan.injector.util.stubs.SessionData;
import io.github.fungrim.blackan.injector.util.stubs.SessionDataImpl;
import io.github.fungrim.blackan.injector.util.stubs.SessionService;
import io.github.fungrim.blackan.injector.util.stubs.SessionWithInstanceBean;
import io.github.fungrim.blackan.injector.util.stubs.SessionWithProviderBean;
import io.github.fungrim.blackan.injector.util.stubs.SmsNotificationService;
import io.github.fungrim.blackan.injector.util.stubs.UnsatisfiedInstanceBean;

class ContextTest {

    private final AtomicReference<Context> currentContext = new AtomicReference<>();

    private RootContext root;

    @BeforeEach
    void setup() throws IOException {
        SessionDataImpl.resetCounter();
        RequestInfoImpl.resetCounter();
        root = RootContext.builder()
                .withClasses(List.of(
                        Greeting.class,
                        AppGreeting.class,
                        SessionData.class,
                        SessionDataImpl.class,
                        RequestInfo.class,
                        RequestInfoImpl.class,
                        AppService.class,
                        SessionService.class,
                        RequestHandler.class,
                        IllegalAppBean.class,
                        SessionWithProviderBean.class,
                        SessionWithInstanceBean.class,
                        ListProducer.class,
                        GenericInjectionBean.class,
                        NotificationService.class,
                        EmailNotificationService.class,
                        SmsNotificationService.class,
                        AuditService.class,
                        NonResolvableBean.class,
                        NonResolvableProviderBean.class,
                        AmbiguousInstanceBean.class,
                        UnsatisfiedInstanceBean.class,
                        CircularFieldA.class,
                        CircularFieldB.class,
                        CircularConstructorA.class,
                        CircularConstructorB.class,
                        CircularProviderA.class,
                        CircularProviderB.class,
                        PriorityService.class,
                        HighPriorityService.class,
                        LowPriorityService.class,
                        NoPriorityService.class))
                .withScopeProvider(() -> currentContext.get())
                .build();
        currentContext.set(root);
    }

    @Nested
    class ApplicationScope {

        @Test
        void resolvesAppScopedBeanViaInterface() {
            currentContext.set(root);
            Greeting greeting = root.get(Greeting.class);
            assertNotNull(greeting);
            assertEquals("hello from app", greeting.greet());
        }

        @Test
        void appScopedBeanIsCachedAsSingleton() {
            currentContext.set(root);
            Greeting first = root.get(Greeting.class);
            Greeting second = root.get(Greeting.class);
            assertSame(first, second);
        }

        @Test
        void appServiceGetsGreetingInjected() {
            currentContext.set(root);
            AppService service = root.get(AppService.class);
            assertNotNull(service);
            assertNotNull(service.greeting);
            assertEquals("hello from app", service.hello());
        }
    }

    @Nested
    class SessionScope {

        @Test
        void sessionScopedBeanResolvedInSessionContext() {
            Context session = root.subcontext(Scope.SESSION);
            currentContext.set(session);
            SessionData data = session.get(SessionData.class);
            assertNotNull(data);
            assertNotNull(data.getSessionId());
        }

        @Test
        void sessionScopedBeanIsCachedWithinSameSession() {
            Context session = root.subcontext(Scope.SESSION);
            currentContext.set(session);
            SessionData first = session.get(SessionData.class);
            SessionData second = session.get(SessionData.class);
            assertSame(first, second);
        }

        @Test
        void differentSessionsGetDifferentBeans() {
            Context session1 = root.subcontext(Scope.SESSION);
            currentContext.set(session1);
            SessionData data1 = session1.get(SessionData.class);

            Context session2 = root.subcontext(Scope.SESSION);
            currentContext.set(session2);
            SessionData data2 = session2.get(SessionData.class);

            assertNotSame(data1, data2);
            assertNotSame(data1.getSessionId(), data2.getSessionId());
        }

        @Test
        void sessionServiceGetsAppScopedGreetingInjected() {
            Context session = root.subcontext(Scope.SESSION);
            currentContext.set(session);
            SessionService service = session.get(SessionService.class);
            assertNotNull(service);
            assertNotNull(service.greeting);
            assertEquals("hello from app", service.greeting.greet());
        }

        @Test
        void sessionServiceGetsSameSessionData() {
            Context session = root.subcontext(Scope.SESSION);
            currentContext.set(session);
            SessionService service = session.get(SessionService.class);
            assertNotNull(service.sessionData);
            SessionData directData = session.get(SessionData.class);
            assertSame(service.sessionData, directData);
        }
    }

    @Nested
    class RequestScope {

        @Test
        void requestScopedBeanResolvedInRequestContext() {
            Context session = root.subcontext(Scope.SESSION);
            Context request = session.subcontext(Scope.REQUEST);
            currentContext.set(request);
            RequestInfo info = request.get(RequestInfo.class);
            assertNotNull(info);
            assertNotNull(info.getRequestId());
        }

        @Test
        void requestScopedBeanIsCachedWithinSameRequest() {
            Context session = root.subcontext(Scope.SESSION);
            Context request = session.subcontext(Scope.REQUEST);
            currentContext.set(request);
            RequestInfo first = request.get(RequestInfo.class);
            RequestInfo second = request.get(RequestInfo.class);
            assertSame(first, second);
        }

        @Test
        void differentRequestsGetDifferentBeans() {
            Context session = root.subcontext(Scope.SESSION);

            Context request1 = session.subcontext(Scope.REQUEST);
            currentContext.set(request1);
            RequestInfo info1 = request1.get(RequestInfo.class);

            Context request2 = session.subcontext(Scope.REQUEST);
            currentContext.set(request2);
            RequestInfo info2 = request2.get(RequestInfo.class);

            assertNotSame(info1, info2);
        }

        @Test
        void requestHandlerGetsAllDependenciesInjected() {
            Context session = root.subcontext(Scope.SESSION);
            Context request = session.subcontext(Scope.REQUEST);
            currentContext.set(request);
            RequestHandler handler = request.get(RequestHandler.class);
            assertNotNull(handler);
            assertNotNull(handler.requestInfo);
            assertNotNull(handler.sessionData);
            assertNotNull(handler.greeting);
            assertEquals("hello from app", handler.greeting.greet());
        }

        @Test
        void requestsInSameSessionShareSessionData() {
            Context session = root.subcontext(Scope.SESSION);

            Context request1 = session.subcontext(Scope.REQUEST);
            currentContext.set(request1);
            RequestHandler handler1 = request1.get(RequestHandler.class);

            Context request2 = session.subcontext(Scope.REQUEST);
            currentContext.set(request2);
            RequestHandler handler2 = request2.get(RequestHandler.class);

            assertSame(handler1.sessionData, handler2.sessionData);
            assertNotSame(handler1.requestInfo, handler2.requestInfo);
        }
    }

    @Nested
    class CrossScopeInjection {

        @Test
        void cannotInjectRequestScopedIntoAppScope() {
            currentContext.set(root);
            assertThrows(ConstructionException.class,
                    () -> root.get(IllegalAppBean.class));
        }

        @Test
        void canInjectAppScopedIntoSessionScope() {
            Context session = root.subcontext(Scope.SESSION);
            currentContext.set(session);
            SessionService service = session.get(SessionService.class);
            assertNotNull(service.greeting);
        }

        @Test
        void canInjectAppScopedIntoRequestScope() {
            Context session = root.subcontext(Scope.SESSION);
            Context request = session.subcontext(Scope.REQUEST);
            currentContext.set(request);
            RequestHandler handler = request.get(RequestHandler.class);
            assertNotNull(handler.greeting);
            assertEquals("hello from app", handler.greeting.greet());
        }

        @Test
        void canInjectSessionScopedIntoRequestScope() {
            Context session = root.subcontext(Scope.SESSION);
            Context request = session.subcontext(Scope.REQUEST);
            currentContext.set(request);
            RequestHandler handler = request.get(RequestHandler.class);
            assertNotNull(handler.sessionData);
        }

        @Test
        void appScopedBeanIsSameAcrossAllContexts() {
            Context session = root.subcontext(Scope.SESSION);
            Context request = session.subcontext(Scope.REQUEST);

            currentContext.set(root);
            Greeting fromApp = root.get(Greeting.class);

            currentContext.set(session);
            SessionService sessionService = session.get(SessionService.class);

            currentContext.set(request);
            RequestHandler handler = request.get(RequestHandler.class);

            assertSame(fromApp, sessionService.greeting);
            assertSame(fromApp, handler.greeting);
        }
    }

    @Nested
    class ProviderAndInstanceInjection {

        @Test
        void canGetProviderForRequestScopedFromSessionContext() {
            Context session = root.subcontext(Scope.SESSION);
            Context request = session.subcontext(Scope.REQUEST);
            currentContext.set(request);
            var provider = request.getInstance(RequestInfo.class).toProvider(RequestInfo.class);
            assertNotNull(provider);
            RequestInfo info = provider.get();
            assertNotNull(info);
        }

        @Test
        void canGetInstanceForRequestScopedFromSessionContext() {
            Context session = root.subcontext(Scope.SESSION);
            Context request = session.subcontext(Scope.REQUEST);
            currentContext.set(request);
            var instance = request.getInstance(RequestInfo.class).toInstance(RequestInfo.class);
            assertNotNull(instance);
            assertNotNull(instance.get());
        }

        @Test
        void providerCreatesNewDependentEachCall() {
            Context session = root.subcontext(Scope.SESSION);
            Context request = session.subcontext(Scope.REQUEST);
            currentContext.set(request);
            var provider = request.getInstance(RequestInfo.class).toProvider(RequestInfo.class);
            RequestInfo first = provider.get();
            RequestInfo second = provider.get();
            assertSame(first, second);
        }

        @Test
        void sessionBeanCanInjectProviderOfRequestScoped() {
            Context session = root.subcontext(Scope.SESSION);
            currentContext.set(session);
            SessionWithProviderBean bean = session.get(SessionWithProviderBean.class);
            assertNotNull(bean.requestInfoProvider);

            Context request = session.subcontext(Scope.REQUEST);
            currentContext.set(request);
            RequestInfo info = bean.requestInfoProvider.get();
            assertNotNull(info);
            assertNotNull(info.getRequestId());
        }

        @Test
        void providerResolvesInCurrentScopeEachCall() {
            Context session = root.subcontext(Scope.SESSION);
            currentContext.set(session);
            SessionWithProviderBean bean = session.get(SessionWithProviderBean.class);

            Context request1 = session.subcontext(Scope.REQUEST);
            currentContext.set(request1);
            RequestInfo info1 = bean.requestInfoProvider.get();

            Context request2 = session.subcontext(Scope.REQUEST);
            currentContext.set(request2);
            RequestInfo info2 = bean.requestInfoProvider.get();

            assertNotSame(info1, info2);
        }

        @Test
        void providerReturnsCachedBeanWithinSameRequest() {
            Context session = root.subcontext(Scope.SESSION);
            currentContext.set(session);
            SessionWithProviderBean bean = session.get(SessionWithProviderBean.class);

            Context request = session.subcontext(Scope.REQUEST);
            currentContext.set(request);
            RequestInfo first = bean.requestInfoProvider.get();
            RequestInfo second = bean.requestInfoProvider.get();
            assertSame(first, second);
        }

        @Test
        void sessionBeanCanInjectInstanceOfRequestScoped() {
            Context session = root.subcontext(Scope.SESSION);
            currentContext.set(session);
            SessionWithInstanceBean bean = session.get(SessionWithInstanceBean.class);
            assertNotNull(bean.requestInfoInstance);

            Context request = session.subcontext(Scope.REQUEST);
            currentContext.set(request);
            RequestInfo info = bean.requestInfoInstance.get();
            assertNotNull(info);
            assertNotNull(info.getRequestId());
        }
    }

    @Nested
    class GenericTypeInjection {

        @Test
        void injectsListOfStrings() {
            currentContext.set(root);
            GenericInjectionBean bean = root.get(GenericInjectionBean.class);
            assertNotNull(bean.stringList);
            assertEquals(List.of("alpha", "beta", "gamma"), bean.stringList);
        }

        @Test
        void injectsListOfIntegers() {
            currentContext.set(root);
            GenericInjectionBean bean = root.get(GenericInjectionBean.class);
            assertNotNull(bean.integerList);
            assertEquals(List.of(1, 2, 3), bean.integerList);
        }

        @Test
        void listOfStringIsNotSameAsListOfInteger() {
            currentContext.set(root);
            GenericInjectionBean bean = root.get(GenericInjectionBean.class);
            assertNotSame(bean.stringList, bean.integerList);
        }

        @Test
        void namedListOfIntegerIsDifferentFromUnqualifiedListOfInteger() {
            currentContext.set(root);
            GenericInjectionBean bean = root.get(GenericInjectionBean.class);
            assertNotNull(bean.productionIntegerList);
            assertEquals(List.of(100, 200, 300), bean.productionIntegerList);
            assertNotSame(bean.integerList, bean.productionIntegerList);
        }

        @Test
        void unqualifiedListOfIntegerHasExpectedValues() {
            currentContext.set(root);
            GenericInjectionBean bean = root.get(GenericInjectionBean.class);
            assertEquals(List.of(1, 2, 3), bean.integerList);
        }

        @Test
        void namedListOfIntegerHasExpectedValues() {
            currentContext.set(root);
            GenericInjectionBean bean = root.get(GenericInjectionBean.class);
            assertEquals(List.of(100, 200, 300), bean.productionIntegerList);
        }

        @Test
        void producerMethodResolvesArgumentsFromContext() {
            currentContext.set(root);
            GenericInjectionBean bean = root.get(GenericInjectionBean.class);
            assertNotNull(bean.greetingList);
            assertEquals(List.of("hello from app", "HELLO FROM APP"), bean.greetingList);
        }
    }

    @Nested
    class UnsatisfiedAndAmbiguousResolution {

        @Test
        void cannotInjectNonResolvableBean() {
            currentContext.set(root);
            assertThrows(ConstructionException.class,
                    () -> root.get(NonResolvableBean.class));
        }

        @Test
        void cannotInjectProviderOfNonResolvableBean() {
            currentContext.set(root);
            NonResolvableProviderBean bean = root.get(NonResolvableProviderBean.class);
            assertNotNull(bean.auditServiceProvider);
            assertThrows(Exception.class,
                    () -> bean.auditServiceProvider.get());
        }

        @Test
        void canInjectAmbiguousInstanceAndIterateAllCandidates() {
            currentContext.set(root);
            AmbiguousInstanceBean bean = root.get(AmbiguousInstanceBean.class);
            assertNotNull(bean.notificationServices);
            assertTrue(bean.notificationServices.isAmbiguous());
            assertFalse(bean.notificationServices.isUnsatisfied());

            List<String> results = new ArrayList<>();
            for (NotificationService service : bean.notificationServices) {
                results.add(service.notify("hello"));
            }
            assertEquals(2, results.size());
            assertTrue(results.contains("email: hello"));
            assertTrue(results.contains("sms: hello"));
        }

        @Test
        void canInjectUnsatisfiedInstanceAndCheckIsUnsatisfied() {
            currentContext.set(root);
            UnsatisfiedInstanceBean bean = root.get(UnsatisfiedInstanceBean.class);
            assertNotNull(bean.auditServiceInstance);
            assertTrue(bean.auditServiceInstance.isUnsatisfied());
            assertFalse(bean.auditServiceInstance.isAmbiguous());
        }
    }

    @Nested
    class CircularDependencyDetection {

        @Test
        void circularFieldInjectionIsDetected() {
            currentContext.set(root);
            assertThrows(ConstructionException.class,
                    () -> root.get(CircularFieldA.class));
        }

        @Test
        void circularConstructorInjectionIsDetected() {
            currentContext.set(root);
            assertThrows(ConstructionException.class,
                    () -> root.get(CircularConstructorA.class));
        }

        @Test
        void circularDependencyViaProviderIsAllowed() {
            currentContext.set(root);
            CircularProviderA a = root.get(CircularProviderA.class);
            assertNotNull(a);
            assertNotNull(a.providerB);

            CircularProviderB b = a.providerB.get();
            assertNotNull(b);
            assertEquals("B", b.value());

            CircularProviderA backToA = b.providerA.get();
            assertNotNull(backToA);
            assertEquals("A", backToA.value());
            assertSame(a, backToA);
        }
    }

    @Nested
    class CandidatePriorityOrdering {

        @Test
        void candidatesAreOrderedByPriorityWithMissingPriorityLast() {
            currentContext.set(root);
            var candidates = root.getInstance(PriorityService.class).candidates();
            assertEquals(3, candidates.size());
            assertEquals(HighPriorityService.class.getName(), candidates.get(0).name().toString());
            assertEquals(LowPriorityService.class.getName(), candidates.get(1).name().toString());
            assertEquals(NoPriorityService.class.getName(), candidates.get(2).name().toString());
        }
    }
}
