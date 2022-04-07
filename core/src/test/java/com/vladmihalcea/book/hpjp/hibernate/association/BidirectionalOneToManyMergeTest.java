package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
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
public class BidirectionalOneToManyMergeTest extends AbstractTest {

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
            .addComment(new PostComment().setReview("JDBC section is a must-read!"))
            .addComment(new PostComment().setReview("The book size is larger than usual."))
            .addComment(new PostComment().setReview("Just half-way through."))
            .addComment(new PostComment().setReview("The book has over 450 pages."));
        });
    }

    @Test
    @Ignore
    public void testCollectionOverwrite() {
        List<PostComment> comments = fetchPostComments(1L);

        modifyComments(comments);

        doInJPA(entityManager -> {
            Post post = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "join fetch p.comments " +
                "where p.id = :id", Post.class)
            .setParameter("id", 1L)
            .getSingleResult();

            entityManager.detach(post);
            post.setComments(comments);
            entityManager.merge(post);
        });

        verifyResults();
    }

    @Test
    public void testCollectionOverwriteFix() {
        List<PostComment> comments = fetchPostComments(1L);

        modifyComments(comments);

        doInJPA(entityManager -> {
            Post post = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "join fetch p.comments " +
                "where p.id = :id", Post.class)
            .setParameter("id", 1L)
            .getSingleResult();

            entityManager.detach(post);

            post.getComments().clear();
            for (PostComment comment : comments) {
                post.addComment(comment);
            }

            entityManager.merge(post);
        });

        verifyResults();
    }

    @Test
    public void testCollectionMerge() {

        List<PostComment> comments = fetchPostComments(1L);

        modifyComments(comments);

        doInJPA(entityManager -> {
            Post post = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "join fetch p.comments " +
                "where p.id = :id", Post.class)
            .setParameter("id", 1L)
            .getSingleResult();

            List<PostComment> removedComments = new ArrayList<>(post.getComments());
            removedComments.removeAll(comments);

            for(PostComment removedComment : removedComments) {
                post.removeComment(removedComment);
            }

            List<PostComment> newComments = new ArrayList<>(comments);
            newComments.removeAll(post.getComments());

            comments.removeAll(newComments);

            for(PostComment existingComment : comments) {
                existingComment.setPost(post);
                PostComment mergedComment = entityManager.merge(existingComment);
                post.getComments().set(post.getComments().indexOf(mergedComment), mergedComment);
            }

            for(PostComment newComment : newComments) {
                post.addComment(newComment);
            }
        });

        verifyResults();
    }

    @Test
    public void testPostMerge() {
        Post post = fetchPostWithComments(1L);

        modifyPostComments(post);

        doInJPA(entityManager -> {
            entityManager.merge(post);
        });

        verifyResults();
    }

    public List<PostComment> fetchPostComments(Long postId) {
        return doInJPA(entityManager -> {
            return entityManager.createQuery(
                "select pc " +
                "from PostComment pc " +
                "join pc.post p " +
                "where p.id = :postId " +
                "order by pc.id", PostComment.class)
            .setParameter("postId", postId)
            .getResultList();
        });
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

    private void modifyComments(List<PostComment> comments) {
        comments.get(0)
        .setReview("The JDBC part is a must-have!");

        comments.remove(2);

        comments.add(
            new PostComment()
            .setReview(
                "The last part is about jOOQ and " +
                "how to get the most of your relational database."
            )
        );
    }

    private void modifyPostComments(Post post) {
        post.getComments().get(0).setReview("The JDBC part is a must-have!");

        post.removeComment(post.getComments().get(2));

        post.addComment(
            new PostComment()
            .setReview(
                "The last part is about jOOQ and " +
                "how to get the most of your relational database."
            )
        );
    }

    private void verifyResults() {
        doInJPA(entityManager -> {
            Post post = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "join fetch p.comments c " +
                "where p.id = :id " +
                "order by c.id", Post.class)
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
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        @GeneratedValue
        private Long id;

        private String review;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "post_id")
        private Post post;

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
