package com.vladmihalcea.book.hpjp.hibernate.criteria;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class CriteriaNestedQueryTest extends AbstractTest {

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

            DetachedCriteria innerCriteria = DetachedCriteria.forClass(PostComment.class, "inner")
                .add(Restrictions.eqProperty("inner.post.id","upper.id"))
                .setProjection(Projections.projectionList().add(Projections.max("inner.id")));

            DetachedCriteria outerCriteria= DetachedCriteria.forClass(Post.class, "upper");
            outerCriteria.createAlias("upper.comments", "comments");
            outerCriteria.add(Subqueries.propertyEq("comments.id", innerCriteria ));
            outerCriteria.add(Restrictions.eq("comments.review", "Excellent"));

            List<Post> posts = outerCriteria.getExecutableCriteria(session).list();
            assertEquals(1, posts.size());
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
