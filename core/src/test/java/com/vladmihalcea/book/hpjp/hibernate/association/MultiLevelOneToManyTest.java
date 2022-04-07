package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MultiLevelOneToManyTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                PostComment.class,
                PostCommentTag.class,
        };
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            Post post = new Post("First post");

            PostComment comment1 = new PostComment("My first review");
            PostComment comment2 = new PostComment("My second review");
            PostComment comment3 = new PostComment("My third review");

            post.addComment(
                    comment1
            );
            post.addComment(
                    comment2
            );
            post.addComment(
                    comment3
            );

            comment1.addTag(new PostCommentTag("Java"));

            comment2.addTag(new PostCommentTag("Java"));
            comment2.addTag(new PostCommentTag("JPA"));

            comment3.addTag(new PostCommentTag("Java"));
            comment3.addTag(new PostCommentTag("JPA"));
            comment3.addTag(new PostCommentTag("Hibernate"));

            entityManager.persist(post);
        });
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            assertEquals(1, post.getComments().get(0).getTags().size());
            assertEquals(2, post.getComments().get(1).getTags().size());
            assertEquals(3, post.getComments().get(2).getTags().size());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<PostComment> comments = new ArrayList<>();

        public Post() {
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

        public List<PostComment> getComments() {
            return comments;
        }

        public void addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
        }

        public void removeComment(PostComment comment) {
            comments.remove(comment);
            comment.setPost(null);
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

        @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<PostCommentTag> tags = new ArrayList<>();

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

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public List<PostCommentTag> getTags() {
            return tags;
        }

        public void addTag(PostCommentTag tag) {
            tags.add(tag);
            tag.setComment(this);
        }

        public void removeComment(PostCommentTag tag) {
            tags.remove(tag);
            tag.setComment(null);
        }
    }

    @Entity(name = "PostCommentTag")
    @Table(name = "post_comment_tag")
    public static class PostCommentTag {

        @Id
        @GeneratedValue
        private Long id;

        private String tag;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "post_comment_id")
        private PostComment comment;

        public PostCommentTag() {
        }

        public PostCommentTag(String tag) {
            this.tag = tag;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public PostComment getComment() {
            return comment;
        }

        public void setComment(PostComment comment) {
            this.comment = comment;
        }
    }
}
