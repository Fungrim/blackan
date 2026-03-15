package io.github.fungrim.blackan.test;

import java.io.IOException;

import io.github.fungrim.blackan.extension.jetty.ServletPath;
import io.github.fungrim.blackan.test.dto.HelloResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@ApplicationScoped
@ServletPath("/hello")
public class HelloWorldServlet extends HttpServlet {

    @Inject
    ObjectMapper objectMapper;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.getWriter().write(objectMapper.writeValueAsString(new HelloResponse("Hello World")));
    }
}
