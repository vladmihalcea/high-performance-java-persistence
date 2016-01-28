package com.vladmihalcea.book.hpjp.hibernate.identifier;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.junit.Ignore;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Ignore
public class GlobalIdentifierGeneratorScopeTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            Announcement.class
        };
    }

    @Test
    public void testHiloIdentifierGenerator() {
        doInJPA(entityManager -> {
            for(int i = 0; i < 4; i++) {
                Post post = new Post();
                entityManager.persist(post);
                Announcement announcement = new Announcement();
                entityManager.persist(announcement);
            }
        });
    }

    @Entity(name = "Post")
    public static class Post {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pooled")
        @GenericGenerator(
            name = "pooled",
            strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
            parameters = {
                @Parameter(name = "sequence_name", value = "sequence"),
                @Parameter(name = "initial_value", value = "1"),
                @Parameter(name = "increment_size", value = "5"),
            }
        )
        private Long id;
    }

    @Entity(name = "Announcement")
    public static class Announcement {

        @Id
        /*@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pooled")
        @GenericGenerator(
                name = "pooled",
                strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
                parameters = {
                    @Parameter(name = "sequence_name", value = "sequence"),
                    @Parameter(name = "initial_value", value = "1"),
                    @Parameter(name = "increment_size", value = "10"),
                }
        )*/
        private Long id;
    }


}
