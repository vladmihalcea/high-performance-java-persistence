package com.vladmihalcea.book.hpjp.hibernate.cascade;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Session;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * @author Vlad Mihalcea
 */
public class CascadeTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class
        };
    }

    private Long postId;

    public void afterInit() {
        doInJPA(entityManager -> {
            Post post = new Post()
                .setTitle("High-Performance Java Persistence")
                .addComment(
                    new PostComment()
                        .setReview("Best book on JPA and Hibernate!")
                )
                .addComment(
                    new PostComment()
                        .setReview("A must-read for every Java developer!")
                );


            entityManager.persist(post);

            postId = post.getId();
        });
    }

    @Test
    public void testMerge() {
        Post post = doInJPA(entityManager -> {
            return entityManager.createQuery("""
                select p
                from Post p
                join fetch p.comments
                where p.id = :id
                """, Post.class)
            .setParameter("id", postId)
            .getSingleResult();
        });

        post.setTitle(post.getTitle() + " - 2nd edition");

        PostComment comment = post.getComments()
            .stream()
            .filter(c -> c.getReview().startsWith("Best book"))
            .findAny()
            .orElseGet(null);
        comment.setReview(comment.getReview().replace("Best", "The best"));

        post.addComment(
            new PostComment()
                .setReview("A great reference book")
        );

        doInJPA(entityManager -> {
            entityManager.merge(post);
        });
    }

    @Test
    public void testRemove() {
        doInJPA(entityManager -> {
            Post post = entityManager.createQuery("""
                select p
                from Post p
                join fetch p.comments
                where p.id = :id
                """, Post.class)
            .setParameter("id", postId)
            .getSingleResult();

            entityManager.remove(post);
        });
    }

    @Test
    public void testDetach() {
        doInJPA(entityManager -> {
            Post post = entityManager.createQuery("""
                select p
                from Post p
                join fetch p.comments
                where p.id = :id
                """, Post.class)
            .setParameter("id", postId)
            .getSingleResult();

            assertTrue(entityManager.contains(post));
            for (PostComment comment : post.getComments()) {
                assertTrue(entityManager.contains(comment));
            }

            entityManager.detach(post);

            assertFalse(entityManager.contains(post));
            for (PostComment comment : post.getComments()) {
                assertFalse(entityManager.contains(comment));
            }
        });
    }

    @Test
    public void testUpdate() {
        Post post = doInJPA(entityManager -> {
            return entityManager.createQuery("""
                select p
                from Post p
                join fetch p.comments
                where p.id = :id
                """, Post.class)
            .setParameter("id", postId)
            .getSingleResult();
        });

        post.setTitle(post.getTitle() + " - 2nd edition");

        PostComment comment = post.getComments()
            .stream()
            .filter(c -> c.getReview().startsWith("Best book"))
            .findAny()
            .orElseGet(null);
        comment.setReview(comment.getReview().replace("Best", "The best"));

        post.addComment(
            new PostComment()
                .setReview("A great reference book")
        );

        doInJPA(entityManager -> {
            entityManager
                .unwrap(Session.class)
                .update(post);
        });
    }

    @Test
    public void testOrphanRemoval() {
        doInJPA(entityManager -> {
            Post post = entityManager.createQuery("""
                select p
                from Post p
                join fetch p.comments c
                where p.id = :id
                order by p.id, c.id
                """, Post.class)
                .setParameter("id", postId)
                .getSingleResult();

            post.removeComment(post.getComments().get(0));
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

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

        public List<PostComment> getComments() {
            return comments;
        }

        public Post addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
            return this;
        }

        public Post removeComment(PostComment comment) {
            comments.remove(comment);
            comment.setPost(null);
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
