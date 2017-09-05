package com.vladmihalcea.book.hpjp.hibernate.logging.validator;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.hibernate.logging.validator.sql.SQLStatementCountValidator;
import com.vladmihalcea.book.hpjp.util.AbstractTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class SQLStatementCountValidatorJoinFetchTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Post.class,
            PostComment.class,
        };
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

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
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne(
            fetch = FetchType.LAZY
        )
        private Post post;

        private String review;

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

    @Override
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            Post post1 = new Post();
            post1.setId( 1L );
            post1.setTitle("Post one");

            entityManager.persist(post1);

            PostComment comment1 = new PostComment();
            comment1.setId(1L);
            comment1.setReview("Good");
            comment1.setPost( post1 );

            entityManager.persist(comment1);

            Post post2 = new Post();
            post2.setId( 2L );
            post2.setTitle("Post two");

            entityManager.persist(post2);

            PostComment comment2 = new PostComment();
            comment2.setId(2L);
            comment2.setReview("Excellent");
            comment2.setPost( post2 );

            entityManager.persist(comment2);
        });
    }

    @Test
    public void testNPlusOne() {
        doInJPA( entityManager -> {
            LOGGER.info( "Detect N+1" );

            SQLStatementCountValidator.reset();

            List<PostComment> comments = entityManager.createQuery(
                "select pc " +
                "from PostComment pc", PostComment.class )
            .getResultList();
            assertEquals( 2, comments.size() );

            SQLStatementCountValidator.assertSelectCount( 1 );
        } );
    }
}
