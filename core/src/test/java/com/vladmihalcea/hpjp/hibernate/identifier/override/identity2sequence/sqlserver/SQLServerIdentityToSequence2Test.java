package com.vladmihalcea.hpjp.hibernate.identifier.override.identity2sequence.sqlserver;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import io.hypersistence.utils.hibernate.id.BatchSequence;
import jakarta.persistence.*;
import org.hibernate.cfg.BatchSettings;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.tool.schema.Action;
import org.junit.Test;

import java.util.Properties;

public class SQLServerIdentityToSequence2Test extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class
        };
    }

    @Override
    protected Database database() {
        return Database.SQLSERVER;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(SchemaToolingSettings.HBM2DDL_AUTO, Action.NONE);
        properties.put(BatchSettings.STATEMENT_BATCH_SIZE, "5");
        properties.put(BatchSettings.ORDER_INSERTS, Boolean.TRUE);
        properties.put(BatchSettings.ORDER_UPDATES, Boolean.TRUE);
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            for (int i = 1; i <= 5; i++) {
                Post post = new Post();
                post.setTitle(String.format("Post nr %d", i + 1));
                entityManager.persist(post);

                PostComment postComment = new PostComment();
                postComment.setReview(String.format("Post nr %d", i + 1));
                postComment.setPost(post);
                entityManager.persist(postComment);
            }
        });
    }

    @Entity(name = "Post")
    @Table(name = "Posts")
    public static class Post {

        @Id
        @BatchSequence(
            name = "Seq_Posts_PostId",
            fetchSize = 5
        )
        @Column(name = "PostId")
        private Long id;

        @Column(name = "Title")
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
    @Table(name = "PostComments")
    public class PostComment {

        @Id
        @BatchSequence(
            name = "Seq_PostComments_PostCommentId",
            fetchSize = 5
        )
        @Column(name = "PostCommentId")
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "PostId")
        private Post post;

        @Column(name = "Review")
        private String review;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getReview() {
            return review;
        }

        public void setReview(String title) {
            this.review = title;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }
    }
}
