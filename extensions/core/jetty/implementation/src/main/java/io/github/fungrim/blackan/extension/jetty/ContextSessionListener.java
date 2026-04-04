package io.github.fungrim.blackan.extension.jetty;

import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.Scope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ContextSessionListener implements HttpSessionListener {

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        log.info("Creating session context scope");
        var appScope = (Context) se.getSession().getServletContext().getAttribute(Constants.APPLICATION_SCOPE_KEY.toString());
        se.getSession().setAttribute(Constants.SESSION_SCOPE_KEY.toString(), appScope.subcontext(Scope.SESSION));
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        log.info("Destroying session context scope");
        var sessionScope = (Context) se.getSession().getAttribute(Constants.SESSION_SCOPE_KEY.toString());
        se.getSession().removeAttribute(Constants.SESSION_SCOPE_KEY.toString());
        if(sessionScope != null) {
            log.info("Closing session context scope");
            sessionScope.close();
        }
    }
}
