package com.vladmihalcea.book.hpjp.hibernate.batch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.QueryHint;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.annotations.QueryHints;

import org.junit.Before;
import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractTest;

/**
 * @author Vlad Mihalcea
 */
public class BatchMergeVsUpdateTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.jdbc.batch_size", "5");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        properties.put("hibernate.jdbc.batch_versioned_data", "true");
        return properties;
    }

    @Before
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            for (int i = 0; i < 3; i++) {
                Post post = new Post(
                    String.format("High-Performance Java Persistence, Part no. %d", i)
                );
                post.addComment(
                    new PostComment("Excellent")
                );
                entityManager.persist(post);
            }
        });
    }

    @Test
    public void testMerge() {
        List<Post> posts = doInJPA(entityManager -> {
            return entityManager.createQuery(
                "select distinct p " +
                "from Post p " +
                "join fetch p.comments ", Post.class)
            .setHint( QueryHints.PASS_DISTINCT_THROUGH, false )
            .getResultList();
        });

        for ( Post post: posts ) {
            post.setTitle( "Vlad Mihalcea's " + post.getTitle() );
            for ( PostComment comment: post.getComments() ) {
                comment.setReview( comment.getReview() + " read!" );
            }
        }

        doInJPA(entityManager -> {
            LOGGER.info( "Merge" );
            for ( Post post: posts ) {
                entityManager.merge( post );
            }
        });
    }

    @Test
    public void testUpdate() {
        List<Post> posts = doInJPA(entityManager -> {
            return entityManager.createQuery(
                "select distinct p " +
                "from Post p " +
                "join fetch p.comments ", Post.class)
            .setHint( QueryHints.PASS_DISTINCT_THROUGH, false )
            .getResultList();
        });

        for ( Post post: posts ) {
            post.setTitle( "Vlad Mihalcea's " + post.getTitle() );
            for ( PostComment comment: post.getComments() ) {
                comment.setReview( comment.getReview() + " read!" );
            }
        }

        doInJPA(entityManager -> {
            LOGGER.info( "Update" );
            Session session = entityManager.unwrap( Session.class );
            for ( Post post: posts ) {
                session.update( post );
            }
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        public Post() {}

        public Post(Long id) {
            this.id = id;
        }

        public Post(String title) {
            this.title = title;
        }

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
