package io.github.fungrim.blackan.test;

import java.io.IOException;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;

import io.github.fungrim.blackan.extension.jetty.ServletPath;
import io.github.fungrim.blackan.test.dto.HelloResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@ApplicationScoped
@ServletPath("/hello")
public class HelloWorldServlet extends HttpServlet {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    Provider<SessionFactory> sessionFactory;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        log.info("Will try to access database");
        try (StatelessSession session = sessionFactory.get().openStatelessSession()) {
            Transaction tx = session.beginTransaction();
            try {
                CountEntity countEntity = session.get(CountEntity.class, 1L);
                if (countEntity == null) {
                    countEntity = new CountEntity();
                    countEntity.setId(1L);
                    session.insert(countEntity);
                }
                countEntity.increment();
                long totalCount = session.createQuery("SELECT COUNT(c) FROM CountEntity c", Long.class).getSingleResult();
                log.info("Total count entities in database: {}", totalCount);
                resp.getWriter().write(objectMapper.writeValueAsString(new HelloResponse("Hello World - Request count: " + totalCount)));
                tx.commit();
            } catch (Exception e) {
                tx.rollback();
                log.error("Error processing request", e);
                throw e;
            }
        }
    }
}
