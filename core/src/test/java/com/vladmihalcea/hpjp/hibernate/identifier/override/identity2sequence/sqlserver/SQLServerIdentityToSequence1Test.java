package com.vladmihalcea.hpjp.hibernate.identifier.override.identity2sequence.sqlserver;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.cfg.BatchSettings;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.tool.schema.Action;
import org.junit.jupiter.api.Test;

import java.util.Properties;

public class SQLServerIdentityToSequence1Test extends AbstractTest {

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
    }

    @Override
    protected void beforeInit() {
        executeStatement("drop table if exists PostComments");
        executeStatement("drop table if exists Posts");
        executeStatement("drop sequence if exists Seq_Posts_PostId");
        executeStatement("create table PostComments (PostCommentId bigint identity not null, PostId bigint, Review varchar(255), CONSTRAINT PK_PostComments_PostCommentId PRIMARY KEY CLUSTERED (PostCommentId))");
        executeStatement("create table Posts (PostId bigint identity not null, Title varchar(255), CONSTRAINT PK_Posts_PostId PRIMARY KEY CLUSTERED (PostId))");
        executeStatement("alter table PostComments add constraint FK_PostComments_PostId foreign key (PostId) references Posts");
    }

    @Override
    protected void afterDestroy() {
        //Change the PK in the Posts table from IDENTITY to SEQUENCE
        executeStatement("ALTER TABLE Posts ADD PostIdNew BIGINT NULL");
        executeStatement("UPDATE Posts SET PostIdNew = PostId;");
        executeStatement("ALTER TABLE PostComments DROP CONSTRAINT [FK_PostComments_PostId]");
        executeStatement("ALTER TABLE Posts DROP CONSTRAINT [PK_Posts_PostId]");
        executeStatement("ALTER TABLE Posts DROP COLUMN PostId");
        executeStatement("EXECUTE sp_rename 'Posts.PostIdNew', 'PostId', 'COLUMN'");
        executeStatement("ALTER TABLE Posts ALTER COLUMN PostId BIGINT NOT NULL");
        executeStatement("ALTER TABLE Posts ADD CONSTRAINT PK_Posts_PostId PRIMARY KEY CLUSTERED (PostId ASC)");
        executeStatement("ALTER TABLE PostComments ADD CONSTRAINT FK_PostComments_PostId foreign key (PostId) references Posts");
        executeStatement("""
            DECLARE @MaxId bigint;
            SELECT @MaxId = MAX(PostId) FROM Posts;
            SET @MaxId = @MaxId + 1;
            exec('CREATE SEQUENCE Seq_Posts_PostId START WITH ' + @MaxId + ' INCREMENT BY 1;');
            """);
        executeStatement("ALTER TABLE Posts ADD CONSTRAINT PK_Default_PostId DEFAULT (NEXT VALUE FOR Seq_Posts_PostId) FOR PostId");

        //Change the PK in the PostComments table from IDENTITY to SEQUENCE
        executeStatement("ALTER TABLE PostComments ADD PostCommentIdNew BIGINT NULL");
        executeStatement("UPDATE PostComments SET PostCommentIdNew = PostCommentId;");
        executeStatement("ALTER TABLE PostComments DROP CONSTRAINT [PK_PostComments_PostCommentId]");
        executeStatement("ALTER TABLE PostComments DROP COLUMN PostCommentId");
        executeStatement("EXECUTE sp_rename 'PostComments.PostCommentIdNew', 'PostCommentId', 'COLUMN'");
        executeStatement("ALTER TABLE PostComments ALTER COLUMN PostCommentId BIGINT NOT NULL");
        executeStatement("ALTER TABLE PostComments ADD CONSTRAINT PK_PostComments_PostCommentId PRIMARY KEY CLUSTERED (PostCommentId ASC)");
        executeStatement("""
            DECLARE @MaxId bigint;
            SELECT @MaxId = MAX(PostCommentId) FROM PostComments;
            SET @MaxId = @MaxId + 1;
            exec('CREATE SEQUENCE Seq_PostComments_PostCommentId START WITH ' + @MaxId + ' INCREMENT BY 1;');
            """);
        executeStatement("ALTER TABLE PostComments ADD CONSTRAINT PK_Default_PostCommentId DEFAULT (NEXT VALUE FOR Seq_PostComments_PostCommentId) FOR PostCommentId");
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
        @GeneratedValue(strategy = GenerationType.IDENTITY)
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
        @GeneratedValue(strategy = GenerationType.IDENTITY)
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
