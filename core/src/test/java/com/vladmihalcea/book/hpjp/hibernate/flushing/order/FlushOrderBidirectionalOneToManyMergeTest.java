package com.vladmihalcea.book.hpjp.hibernate.flushing.order;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.NaturalId;
import org.hibernate.jpa.QueryHints;
import org.junit.Ignore;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class FlushOrderBidirectionalOneToManyMergeTest extends AbstractTest {

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
            );
        });

        doInJPA(entityManager -> {
            entityManager
            .find(Post.class, 1L)
            .addComment(new PostComment().setReview("JDBC section is a must-read!").setSlug("/1"))
            .addComment(new PostComment().setReview("The book size is larger than usual.").setSlug("/2"))
            .addComment(new PostComment().setReview("Just half-way through.").setSlug("/3"))
            .addComment(new PostComment().setReview("The book has over 450 pages.").setSlug("/4"));
        });
    }

    @Test
    @Ignore
    public void testPostMergeFlushOrderFail() {
        Post post = fetchPostWithComments(1L);

        modifyPostComments(post);

        doInJPA(entityManager -> {
            entityManager.merge(post);
        });

        verifyResults();
    }

    public Post fetchPostWithComments(Long postId) {
        return doInJPA(entityManager -> {
            return entityManager.createQuery(
                "select distinct p " +
                "from Post p " +
                "join fetch p.comments " +
                "where p.id = :postId ", Post.class)
            .setHint(QueryHints.HINT_READONLY, true)
            .setParameter("postId", postId)
            .getSingleResult();
        });
    }

    private void modifyPostComments(Post post) {
        post.getComments().get(0).setReview("The JDBC part is a must-have!");

        PostComment removedComment = post.getComments().get(2);
        post.removeComment(removedComment);

        post.addComment(
            new PostComment()
            .setReview(
                "The last part is about jOOQ and " +
                "how to get the most of your relational database."
            )
            .setSlug(removedComment.getSlug())
        );
    }

    private void verifyResults() {
        doInJPA(entityManager -> {
            Post post = entityManager.createQuery("""
                select p
                from Post p
                join fetch p.comments c
                where p.id = :idvorder by c.id
                """, Post.class)
            .setParameter("id", 1L)
            .getSingleResult();

            assertEquals(4, post.getComments().size());

            assertEquals(
                "The JDBC part is a must-have!",
                post.getComments().get(0).getReview()
            );

            assertEquals(
                "The book size is larger than usual.",
                post.getComments().get(1).getReview()
            );

            assertEquals(
                "The book has over 450 pages.",
                post.getComments().get(2).getReview()
            );

            assertEquals(
                "The last part is about jOOQ and how to get the most of your relational database.",
                post.getComments().get(3).getReview()
            );
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

        private Post setComments(List<PostComment> comments) {
            this.comments = comments;
            return this;
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
    @Table(
        name = "post_comment",
        uniqueConstraints = @UniqueConstraint(
            name = "slug_uq",
            columnNames = "slug"
        )
    )
    public static class PostComment {

        @Id
        @GeneratedValue
        private Long id;

        private String review;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "post_id")
        private Post post;

        @NaturalId
        private String slug;

        public PostComment() {
        }

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

        public String getSlug() {
            return slug;
        }

        public PostComment setSlug(String slug) {
            this.slug = slug;
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
