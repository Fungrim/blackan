package io.github.fungrim.blackan.extension.jetty;

import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.Scope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ContextRequestListener implements ServletRequestListener{

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        var parentScope = (Context) sre.getServletContext().getAttribute(Constants.APPLICATION_SCOPE_KEY.toString());
        if(sre.getServletRequest() instanceof HttpServletRequest req && req.getSession(false) != null) {
            log.debug("Creating request context scope with parent session context");
            parentScope = (Context) req.getSession().getAttribute(Constants.SESSION_SCOPE_KEY.toString());
        } else {
            log.debug("Creating request context scope with parent application context");
        }
        sre.getServletRequest().setAttribute(Constants.REQUEST_SCOPE_KEY.toString(), parentScope.subcontext(Scope.REQUEST));
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        log.debug("Destroying request context scope");
        var requestScope = (Context) sre.getServletRequest().getAttribute(Constants.REQUEST_SCOPE_KEY.toString());
        sre.getServletRequest().removeAttribute(Constants.REQUEST_SCOPE_KEY.toString());
        if(requestScope != null) {
            requestScope.close();
        }
    }
}
