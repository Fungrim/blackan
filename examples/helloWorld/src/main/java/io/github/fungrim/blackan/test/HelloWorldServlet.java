package io.github.fungrim.blackan.test;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import jakarta.enterprise.context.ApplicationScoped;
import io.github.fungrim.blackan.extension.jetty.ServletPath;

@ApplicationScoped
@ServletPath("/hello")
public class HelloWorldServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.getWriter().write("Hello World");
    }
}
