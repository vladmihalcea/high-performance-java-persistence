package com.vladmihalcea.book.hpjp.hibernate.logging;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class StatementInspectorLoggingTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(
            "hibernate.session_factory.statement_inspector",
            new LoggingStatementInspector(getClass().getPackage().getName())
        );
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId( 1L );
            post.setTitle( "Post it!" );

            entityManager.persist(post);
        });
    }

    @Test
    public void testBatch() {
        doInJPA(entityManager -> {
            for ( long id = 1; id <= 5; id++ ) {
                Post post = new Post();
                post.setId( id );
                post.setTitle(
                    String.format(
                        "High-Performance Java Persistence, part %d",
                        id
                    )
                );

                entityManager.persist(post);
            }
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Version
        private int version;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

}
