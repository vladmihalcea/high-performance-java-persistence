package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.jpa.QueryHints;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class DistinctTest extends AbstractPostgreSQLIntegrationTest {

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
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");

            post.addComment(new PostComment("Excellent!"));
            post.addComment(new PostComment("Great!"));

            entityManager.persist(post);
        });
    }

    @Test
    public void testWithoutDistinct() {
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "left join fetch p.comments " +
                "where p.title = :title", Post.class)
            .setParameter("title", "High-Performance Java Persistence")
            .getResultList();

            LOGGER.info("Fetched {} post entities: {}", posts.size(), posts);
        });
    }

    @Test
    public void testWithDistinctScalarQuery() {
        doInJPA(entityManager -> {
            List<String> posts = entityManager.createQuery(
                "select distinct p.title " +
                "from Post p ", String.class)
            .getResultList();

            LOGGER.info("Fetched {} post entities: {}", posts.size(), posts);
        });
    }

    @Test
    public void testWithDistinct() {
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery(
                "select distinct p " +
                "from Post p " +
                "left join fetch p.comments " +
                "where p.title = :title", Post.class)
            .setParameter("title", "High-Performance Java Persistence")
            .getResultList();

            LOGGER.info("Fetched {} post entities: {}", posts.size(), posts);
        });
    }

    @Test
    public void testWithDistinctAndQueryHint() {
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery(
                "select distinct p " +
                "from Post p " +
                "left join fetch p.comments " +
                "where p.title = :title", Post.class)
            .setParameter("title", "High-Performance Java Persistence")
            .setHint(QueryHints.HINT_PASS_DISTINCT_THROUGH, false)
            .getResultList();

            LOGGER.info("Fetched {} post entities: {}", posts.size(), posts);
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post",
                   orphanRemoval = true)
        private List<PostComment> comments = new ArrayList<>();

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

        public void addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
        }

        @Override
        public String toString() {
            return "Post{" +
                    "id=" + id +
                    ", title='" + title + '\'' +
                    '}';
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        private String review;

        public PostComment() {}

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
