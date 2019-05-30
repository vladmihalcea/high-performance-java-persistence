package com.vladmihalcea.book.hpjp.hibernate.logging.inspector;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Session;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class SqlCommentTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Book.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.use_sql_comments", "true");
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Book()
                .setIsbn("978-9730228236")
                .setTitle("High-Performance Java Persistence")
                .setAuthor("Vlad Mihalcea")
            );
        });

        doInJPA(entityManager -> {
            Book book = entityManager.unwrap(Session.class)
                .bySimpleNaturalId(Book.class)
                .load("978-9730228236");

            assertEquals("High-Performance Java Persistence", book.getTitle());
        });
    }

}
