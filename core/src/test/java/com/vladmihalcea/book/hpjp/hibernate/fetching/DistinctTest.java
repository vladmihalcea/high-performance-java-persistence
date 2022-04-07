package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.jpa.QueryHints;
import org.junit.Test;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence eBook has been released!")
                    .setCreatedOn(LocalDate.of(2016, 8, 30))
                    .addComment(new PostComment("Excellent!"))
                    .addComment(new PostComment("Great!"))
            );

            entityManager.persist(
                new Post()
                    .setId(2L)
                    .setTitle("High-Performance Java Persistence paperback has been released!")
                    .setCreatedOn(LocalDate.of(2016, 10, 12))
            );

            entityManager.persist(
                new Post()
                    .setId(3L)
                    .setTitle("High-Performance Java Persistence Mach 1 video course has been released!")
                    .setCreatedOn(LocalDate.of(2018, 1, 30))
            );

            entityManager.persist(
                new Post()
                    .setId(4L)
                    .setTitle("High-Performance Java Persistence Mach 2 video course has been released!")
                    .setCreatedOn(LocalDate.of(2018, 5, 8))
            );
        });
    }

    @Test
    public void testWithDistinctScalarQuery() {
        doInJPA(entityManager -> {
            List<Integer> publicationYears = entityManager.createQuery("""
                select distinct year(p.createdOn)
                from Post p
                order by year(p.createdOn)
                """, Integer.class)
            .getResultList();

            LOGGER.info("Publication years: {}", publicationYears);
        });
    }

    @Test
    public void testWithoutDistinct() {
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p
                left join fetch p.comments
                where p.title = :title
                """, Post.class)
            .setParameter("title", "High-Performance Java Persistence eBook has been released!")
            .getResultList();

            LOGGER.info("Fetched the following Post entity identifiers: {}", posts.stream().map(Post::getId).collect(Collectors.toList()));
        });
    }

    @Test
    public void testWithDistinct() {
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery("""
                select distinct p
                from Post p
                left join fetch p.comments
                where p.title = :title
                """, Post.class)
            .setParameter("title", "High-Performance Java Persistence eBook has been released!")
            .getResultList();

            LOGGER.info("Fetched the following Post entity identifiers: {}", posts.stream().map(Post::getId).collect(Collectors.toList()));
        });
    }

    @Test
    public void testWithDistinctAndQueryHint() {
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery("""
                select distinct p
                from Post p
                left join fetch p.comments
                where p.title = :title
                """, Post.class)
            .setParameter("title", "High-Performance Java Persistence eBook has been released!")
            .getResultList();

            LOGGER.info("Fetched the following Post entity identifiers: {}", posts.stream().map(Post::getId).collect(Collectors.toList()));
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Column(name = "created_on")
        private LocalDate createdOn;

        @OneToMany(
            mappedBy = "post",
            cascade = CascadeType.ALL,
            orphanRemoval = true
        )
        private List<PostComment> comments = new ArrayList<>();

        public Long getId() {
            return id;
        }

        public Post setId(Long id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Post setTitle(String title) {
            this.title = title;
            return this;
        }

        public LocalDate getCreatedOn() {
            return createdOn;
        }

        public Post setCreatedOn(LocalDate createdOn) {
            this.createdOn = createdOn;
            return this;
        }

        public Post addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
            return this;
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        @GeneratedValue
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

        public PostComment setId(Long id) {
            this.id = id;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostComment setPost(Post post) {
            this.post = post;
            return this;
        }

        public String getReview() {
            return review;
        }

        public PostComment setReview(String review) {
            this.review = review;
            return this;
        }
    }
}
