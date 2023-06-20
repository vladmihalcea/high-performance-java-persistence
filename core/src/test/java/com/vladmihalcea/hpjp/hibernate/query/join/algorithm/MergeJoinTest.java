package com.vladmihalcea.hpjp.hibernate.query.join.algorithm;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MergeJoinTest {

    private List<Post> posts = new ArrayList<>();

    private List<PostComment> postComments = new ArrayList<>();

    /**
     * post
     * ----
     *
     * | id | title     |
     * |----|-----------|
     * | 1  | Java      |
     * | 2  | Hibernate |
     * | 3  | JPA       |
     *
     * post_comment
     * -------------
     *
     * | id | review    | post_id |
     * |----|-----------|---------|
     * | 1  | Good      | 1       |
     * | 2  | Excellent | 1       |
     * | 3  | Awesome   | 2       |
     */
    public MergeJoinTest() {
        posts.add(
            new Post()
                .setId(1L)
                .setTitle("Java")
        );

        posts.add(
            new Post()
                .setId(2L)
                .setTitle("Hibernate")
        );

        posts.add(
            new Post()
                .setId(3L)
                .setTitle("JPA")
        );

        postComments.add(
            new PostComment()
                .setId(1L)
                .setReview("Good")
                .setPost(1L)
        );

        postComments.add(
            new PostComment()
                .setId(2L)
                .setReview("Excellent")
                .setPost(1L)
        );

        postComments.add(
            new PostComment()
                .setId(3L)
                .setReview("Awesome")
                .setPost(2L)
        );
    }

    /**
     * Get all posts with their associated post_comments.
     *
     *
     * | post_id | post_title | review    |
     * |---------|------------|-----------|
     * | 1       | Java       | Good      |
     * | 1       | Java       | Excellent |
     * | 2       | Hibernate  | Awesome   |
     */
    @Test
    public void testInnerJoin() {
        posts.sort(Comparator.comparing(Post::getId));

        postComments.sort((pc1, pc2) -> {
            int result = Comparator.comparing(PostComment::getPostId).compare(pc1, pc2);
            return result != 0 ? result :
                Comparator.comparing(PostComment::getId).compare(pc1, pc2);
        });

        List<Tuple> tuples = new ArrayList<>();
        int postCount = posts.size(), postCommentCount = postComments.size();
        int i = 0, j = 0;

        while(i < postCount && j < postCommentCount) {
            Post post = posts.get(i);
            PostComment postComment = postComments.get(j);
            
            if(post.getId().equals(postComment.getPostId())) {
                tuples.add(
                    new Tuple()
                        .add("post_id", postComment.getPostId())
                        .add("post_title", post.getTitle())
                        .add("review", postComment.getReview())
                );
                j++;
            } else {
                i++;
            }
        }

        Tuple tuple1 = tuples.get(0);
        assertEquals(1L, tuple1.getLong("post_id"));
        assertEquals("Java", tuple1.get("post_title"));
        assertEquals("Good", tuple1.get("review"));

        Tuple tuple2 = tuples.get(1);
        assertEquals(1L, tuple2.getLong("post_id"));
        assertEquals("Java", tuple2.get("post_title"));
        assertEquals("Excellent", tuple2.get("review"));

        Tuple tuple3 = tuples.get(2);
        assertEquals(2L, tuple3.getLong("post_id"));
        assertEquals("Hibernate", tuple3.get("post_title"));
        assertEquals("Awesome", tuple3.get("review"));
    }

    public static class Post {

        private Long id;

        private String title;

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
    }

    public static class PostComment {

        private Long id;

        private String review;

        private Long postId;


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

        public Long getPostId() {
            return postId;
        }

        public PostComment setPost(Long postId) {
            this.postId = postId;
            return this;
        }
    }
}
