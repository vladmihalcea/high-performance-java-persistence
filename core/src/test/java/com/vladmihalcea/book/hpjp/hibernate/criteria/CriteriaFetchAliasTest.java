package com.vladmihalcea.book.hpjp.hibernate.criteria;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class CriteriaFetchAliasTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class,
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Post post = new Post(1L);
            post.title = "Postit";

            PostComment comment1 = new PostComment();
            comment1.id = 1L;
            comment1.review = "Good";

            PostComment comment2 = new PostComment();
            comment2.id = 2L;
            comment2.review = "Excellent";

            post.addComment(comment1);
            post.addComment(comment2);
            entityManager.persist(post);

            Session session = entityManager.unwrap(Session.class);
            Criteria criteria = session.createCriteria(Post.class)
                .add(Restrictions.eq("title", "post"));

            LOGGER.info("Criteria: {}", criteria);
        });

        doInJPA(entityManager -> {
            LOGGER.info("No alias");
            Session session = entityManager.unwrap(Session.class);
            List<Post> posts = session
                    .createCriteria(Post.class)
                    .setFetchMode("comments", FetchMode.JOIN)
                    .add(Restrictions.eq("title", "Postit"))
                    .list();
            assertEquals(2, posts.size());
        });

        try {
            doInJPA(entityManager -> {
                LOGGER.info("With alias");
                Session session = entityManager.unwrap(Session.class);
                List<Post> posts = session
                    .createCriteria(Post.class, "post")
                    .setFetchMode("post.comments", FetchMode.JOIN)
                    .add(Restrictions.eq("post.title", "Postit"))
                    .list();
                assertEquals(2, posts.size());
            });
        } catch (Throwable e) {
            LOGGER.error("Failure", e);
        }

        doInJPA(entityManager -> {
            Post newPost = new Post(2L);
            entityManager.persist(newPost);
        });
        doInJPA(entityManager -> {
            LOGGER.info("In query");
            Session session = entityManager.unwrap(Session.class);


            List<PostComment> postComments = new ArrayList<>();
            postComments.add(new PostComment());
            postComments.get(0).setId(2L);
            postComments.add(new PostComment());
            postComments.get(1).setId(3L);


            List<Long> ids = postComments.stream().map(PostComment::getId).collect(Collectors.toList());
            List<Post> filtered = session.createCriteria(Post.class)
                    .createAlias("comments", "c")
                    .add( Restrictions.in( "c.id", ids ) )
                    .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
                    .list();
            assertEquals(1, filtered.size());

            ids = new ArrayList<>();
            ids.add(3L);
            ids.add(4L);

            filtered = session.createCriteria(Post.class)
                    .createAlias("comments", "c")
                    .add( Restrictions.in( "c.id", ids ) )
                    .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
                    .list();
            assertEquals(0, filtered.size());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        public Post() {}

        public Post(Long id) {
            this.id = id;
        }

        public Post(String title) {
            this.title = title;
        }

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

        public List<PostComment> getComments() {
            return comments;
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
        private Long id;

        @ManyToOne
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
