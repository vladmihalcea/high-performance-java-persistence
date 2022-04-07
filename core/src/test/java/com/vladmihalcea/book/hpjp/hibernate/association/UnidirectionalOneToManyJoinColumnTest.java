package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class UnidirectionalOneToManyJoinColumnTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class,
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
                    .addComment(
                        new PostComment()
                            .setReview("Best book on JPA and Hibernate!")
                    )
                    .addComment(
                        new PostComment()
                            .setReview("A must-read for every Java developer!")
                    )
                    .addComment(
                        new PostComment()
                            .setReview("A great reference book")
                    )
            );
        });
    }

    @Test
    public void testRemoveTail() {

        doInJPA(entityManager -> {
            Post post = entityManager.createQuery("""
                select p 
                from Post p
                join fetch p.comments
                where p.id = :id
                """, Post.class)
                .setParameter("id", 1L)
                .getSingleResult();

            post.getComments().remove(post.getComments().size() - 1);
        });
    }

    @Test
    public void testRemoveHead() {

        doInJPA(entityManager -> {
            Post post = entityManager.createQuery("""
                select p 
                from Post p
                join fetch p.comments
                where p.id = :id
                """, Post.class)
                .setParameter("id", 1L)
                .getSingleResult();

            post.getComments().remove(0);
        });
        System.out.println("");
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
        @JoinColumn(name = "post_id")
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

        public List<PostComment> getComments() {
            return comments;
        }

        public Post addComment(PostComment comment) {
            comments.add(comment);
            return this;
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        @GeneratedValue
        private Long id;

        private String review;

        public Long getId() {
            return id;
        }

        public PostComment setId(Long id) {
            this.id = id;
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
