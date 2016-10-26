package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.Session;
import org.junit.Test;

import javax.persistence.*;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Vlad Mihalcea
 */
public class FindEntityTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                PostComment.class,
        };
    }


    @Override
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setTitle(String.format("Post nr. %d", 1));
            entityManager.persist(post);
        });
    }

    @Test
    public void testFind() {
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertNotNull(post);
        });
    }

    @Test
    public void testGet() {
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            Post post = session.get(Post.class, 1L);
            assertNotNull(post);
        });
    }

    @Test
    public void testFindWithQuery() {
        doInJPA(entityManager -> {
            Long postId =  1L;
            Post post = entityManager.createQuery(
                "select p from Post p where p.id = :id", Post.class)
            .setParameter("id", postId)
            .getSingleResult();
            assertNotNull(post);
        });
    }

    @Test
    public void testGetReference() {
        doInJPA(entityManager -> {
            Post post = entityManager.getReference(Post.class, 1L);
            LOGGER.info("Loaded post entity");
            LOGGER.info("The post title is '{}'", post.getTitle());
        });
    }

    @Test
    public void testByIdGetReference() {
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            Post post = session.byId(Post.class).getReference(1L);
            LOGGER.info("Loaded post entity");
            LOGGER.info("The post title is '{}'", post.getTitle());
        });
    }

    @Test
    public void testLoad() {
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            Post post = session.load(Post.class, 1L);
            LOGGER.info("Loaded post entity");
            LOGGER.info("The post title is '{}'", post.getTitle());
        });
    }

    @Test
    public void testGetReferenceAndPersist() {
        doInJPA(entityManager -> {
            LOGGER.info("Persisting a post comment");
            Post post = entityManager.getReference(Post.class, 1L);
            PostComment postComment = new PostComment("Excellent reading!");
            postComment.setPost(post);
            entityManager.persist(postComment);
        });
    }

    @Test
    public void testTransientAndPersist() {
        doInJPA(entityManager -> {
            LOGGER.info("Persisting a post comment");
            Post post = new Post();
            post.setId(1L);
            PostComment postComment = new PostComment("Excellent reading!");
            postComment.setPost(post);
            entityManager.persist(postComment);
            assertNull(postComment.getPost().getTitle());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        private String title;

        public Post() {
        }

        public Post(Long id) {
            this.id = id;
        }

        public Post(String title) {
            this.title = title;
        }

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

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        @ManyToOne
        private Post post;

        private String review;

        public PostComment() {
        }

        public PostComment(String review) {
            this.review = review;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
        }
    }
}
