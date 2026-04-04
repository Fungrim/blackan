package io.github.fungrim.blackan.extension.jetty;

import java.io.IOException;

import io.github.fungrim.blackan.injector.Context;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ServletRequestFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        var requestScope = (Context) request.getAttribute(Constants.REQUEST_SCOPE_KEY.toString());
        ServletScopedContext.enter(requestScope, () -> {
            log.debug("Request context scope entered");
            chain.doFilter(request, response);
        });
        log.debug("Request context scope exited");
    }
}
