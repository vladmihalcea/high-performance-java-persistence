package com.vladmihalcea.hpjp.hibernate.query.dto.projection.record;

import com.vladmihalcea.hpjp.util.AbstractTest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class EntityToRecordTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
        };
    }

    @Override
    public void afterInit() {
        doInStatelessSession(session -> {
            session.insert(
                new PostRecord(
                    1L,
                    "High-Performance Java Persistence",
                    LocalDateTime.of(2016, 11, 2, 12, 0, 0),
                    "Vlad Mihalcea",
                    LocalDateTime.now(),
                    "Vlad Mihalcea",
                    null
                ).toPost()
            );
        });
    }

    @Test
    public void testRecordEntity() {
        PostRecord postRecord = doInJPA(entityManager -> {
            return entityManager.find(Post.class, 1L).toRecord();
        });

        assertEquals(
            "High-Performance Java Persistence", postRecord.title()
        );

        PostRecord updatedPostRecord = new PostRecord(
            postRecord.id,
            postRecord.title,
            postRecord.createdOn,
            postRecord.createdBy,
            LocalDateTime.now(),
            "Vlad",
            postRecord.version
        );

        doInStatelessSession(session -> {
            session.update(
                updatedPostRecord.toPost()
            );
        });
    }

    @Entity(name = "Post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Column(name = "created_on")
        private LocalDateTime createdOn;

        @Column(name = "created_by")
        private String createdBy;

        @Column(name = "updated_on")
        private LocalDateTime updatedOn;

        @Column(name = "updated_by")
        private String updatedBy;

        @Version
        private Short version;

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

        public LocalDateTime getCreatedOn() {
            return createdOn;
        }

        public Post setCreatedOn(LocalDateTime createdOn) {
            this.createdOn = createdOn;
            return this;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public Post setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public LocalDateTime getUpdatedOn() {
            return updatedOn;
        }

        public Post setUpdatedOn(LocalDateTime updatedOn) {
            this.updatedOn = updatedOn;
            return this;
        }

        public String getUpdatedBy() {
            return updatedBy;
        }

        public Post setUpdatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }

        public Short getVersion() {
            return version;
        }

        public Post setVersion(Short version) {
            this.version = version;
            return this;
        }

        public PostRecord toRecord() {
            return new PostRecord(
                id, title, createdOn, createdBy, updatedOn, updatedBy, version
            );
        }
    }

    public static record PostRecord (
            Long id,
            String title,
            LocalDateTime createdOn,
            String createdBy,
            LocalDateTime updatedOn,
            String updatedBy,
            Short version) {

        public Post toPost() {
            return new Post()
                .setId(id)
                .setTitle(title)
                .setCreatedOn(createdOn)
                .setCreatedBy(createdBy)
                .setUpdatedOn(updatedOn)
                .setUpdatedBy(updatedBy)
                .setVersion(version);
        }
    }
}
