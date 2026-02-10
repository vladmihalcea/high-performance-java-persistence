package com.vladmihalcea.hpjp.hibernate.association;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import io.hypersistence.utils.hibernate.type.util.ClassImportIntegrator;
import jakarta.persistence.*;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.stream.LongStream;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class OneToOneMapsIdIdentityTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostDetails.class,
        };
    }

    @Override
    protected Database database() {
        return Database.SQLSERVER;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(AvailableSettings.STATEMENT_BATCH_SIZE, "100");
        properties.put(AvailableSettings.ORDER_INSERTS, "100");
    }

    @Test
    public void testBatchInserts() {
        doInJPA(entityManager -> {
            List<Post> posts = LongStream
                .range(1, 10).boxed()
                .map(id -> new Post().setTitle(String.format("Post nr. %s", id)))
                .toList();
            posts.forEach(entityManager::persist);
            entityManager.flush();
            posts.stream()
                .map(post -> new PostDetails()
                    .setPost(post)
                    .setCreatedBy("Vlad Mihalcea"))
                .forEach(entityManager::persist);
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

    @Entity(name = "PostDetails")
    @Table(name = "PostDetails")
    public static class PostDetails {

        @Id
        private Long id;

        @Column(name = "CreatedOn")
        private LocalDateTime createdOn = LocalDateTime.now();

        @Column(name = "CreatedBy")
        private String createdBy;

        @OneToOne
        @MapsId
        @JoinColumn(name = "PostId")
        private Post post;

        public Long getId() {
            return id;
        }

        public PostDetails setId(Long id) {
            this.id = id;
            return this;
        }

        public LocalDateTime getCreatedOn() {
            return createdOn;
        }

        public PostDetails setCreatedOn(LocalDateTime createdOn) {
            this.createdOn = createdOn;
            return this;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public PostDetails setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostDetails setPost(Post post) {
            this.post = post;
            this.id = post.getId();
            return this;
        }
    }

    public record PostWithDetailsRecord(Post post, PostDetails details) {

    }
}
