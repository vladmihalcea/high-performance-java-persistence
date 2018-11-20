package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.jpa.QueryHints;
import org.junit.Test;

import javax.persistence.*;
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
    public void afterInit() {
        doInJPA(entityManager -> {
            Post post1 = new Post();
            post1.setTitle("High-Performance Java Persistence eBook has been released!");
            post1.setCreatedOn(LocalDate.of(2016, 8, 30));
            entityManager.persist(post1);

            post1.addComment(new PostComment("Excellent!"));
            post1.addComment(new PostComment("Great!"));

            Post post2 = new Post();
            post2.setTitle("High-Performance Java Persistence paperback has been released!");
            post2.setCreatedOn(LocalDate.of(2016, 10, 12));

            entityManager.persist(post2);

            Post post3 = new Post();
            post3.setTitle("High-Performance Java Persistence Mach 1 video course has been released!");
            post3.setCreatedOn(LocalDate.of(2018, 1, 30));
            entityManager.persist(post3);

            Post post4 = new Post();
            post4.setTitle("High-Performance Java Persistence Mach 2 video course has been released!");
            post4.setCreatedOn(LocalDate.of(2018, 5, 8));
            entityManager.persist(post4);
        });
    }

    @Test
    public void testWithDistinctScalarQuery() {
        doInJPA(entityManager -> {
            List<Integer> publicationYears = entityManager.createQuery(
                "select distinct year(p.createdOn) " +
                "from Post p " +
                "order by year(p.createdOn)", Integer.class)
            .getResultList();

            LOGGER.info("Publication years: {}", publicationYears);
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
            .setParameter("title", "High-Performance Java Persistence eBook has been released!")
            .getResultList();

            LOGGER.info("Fetched the following Post entity identifiers: {}", posts.stream().map(Post::getId).collect(Collectors.toList()));
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
            .setParameter("title", "High-Performance Java Persistence eBook has been released!")
            .getResultList();

            LOGGER.info("Fetched the following Post entity identifiers: {}", posts.stream().map(Post::getId).collect(Collectors.toList()));
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
            .setParameter("title", "High-Performance Java Persistence eBook has been released!")
            .setHint(QueryHints.HINT_PASS_DISTINCT_THROUGH, false)
            .getResultList();

            LOGGER.info("Fetched the following Post entity identifiers: {}", posts.stream().map(Post::getId).collect(Collectors.toList()));
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
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

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public LocalDate getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(LocalDate createdOn) {
            this.createdOn = createdOn;
        }

        public void addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
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
