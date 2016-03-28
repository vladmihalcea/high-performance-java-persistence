package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.junit.Test;

import javax.persistence.*;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * <code>FindEntityTest</code> - Find entity Test
 *
 * @author Vlad Mihalcea
 */
public class NaturalIdTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class
        };
    }


    @Override
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setTitle(String.format("Post nr. %d", 1));
            post.setSlug("high-performance-java-persistence");
            entityManager.persist(post);
        });
    }

    @Test
    public void testFindByNaturalId() {
        doInJPA(entityManager -> {
            String slug = "high-performance-java-persistence";
            Session session = entityManager.unwrap(Session.class);
            Post post = session.bySimpleNaturalId(Post.class).load(slug);
            assertNotNull(post);
        });
    }

    @Test
    public void testFindWithQuery() {
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "where p.slug is not null", Post.class)
            .getResultList();
            assertFalse(posts.isEmpty());
        });
    }

    @Test
    public void testGetReferenceByNaturalId() {
        doInJPA(entityManager -> {
            String slug = "high-performance-java-persistence";
            Session session = entityManager.unwrap(Session.class);
            LOGGER.info("Loading a post by natural identifier");
            Post post = session.bySimpleNaturalId(Post.class).getReference(slug);
            LOGGER.info("Proxy is loaded");
            LOGGER.info("Post title is {}", post.getTitle());
            assertNotNull(post);
        });
    }

    @Entity(name = "Post") @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        @NaturalId
        @Column(nullable = false, unique = true)
        private String slug;

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

        public String getSlug() {
            return slug;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }
    }
}
