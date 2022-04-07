package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class BidirectionalOneToManyTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class,
        };
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
            );
        });
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            Post post = entityManager.createQuery("""
                select p 
                from Post p
                join fetch p.comments
                where p.id = :id
                """, Post.class)
            .setParameter("id", 1L)
            .getSingleResult();
            assertEquals(2, post.getComments().size());
        });
    }

    @Test
    public void testRemove() {
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            PostComment comment = post.getComments().get(0);

            post.removeComment(comment);
        });
    }

    @Test
    public void testOrphanRemoval() {
        QueryCountHolder.clear();

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(1, QueryCountHolder.getGrandTotal().getSelect());

            post.getComments().remove(0);
            assertEquals(2, QueryCountHolder.getGrandTotal().getSelect());
        });

        assertEquals(1, QueryCountHolder.getGrandTotal().getDelete());

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            assertEquals(1, post.getComments().size());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
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

        private String review;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "post_id")
        private Post post;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getReview() {
            return review;
        }

        public PostComment setReview(String review) {
            this.review = review;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostComment setPost(Post post) {
            this.post = post;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PostComment)) return false;
            return id != null && id.equals(((PostComment) o).getId());
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }
}
