package com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.record;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PostRecordOverrideTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
        };
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");
            post.setCreatedBy("Vlad Mihalcea");
            post.setCreatedOn(
                LocalDateTime.of(2020, 11, 2, 12, 0, 0)
            );
            post.setUpdatedBy("Vlad Mihalcea");
            post.setUpdatedOn(
                LocalDateTime.now()
            );

            entityManager.persist(post);
        });
    }

    @Test
    public void testRecord() {
        doInJPA(entityManager -> {
            PostInfo postInfo = new PostInfo(
                1L,
                "High-Performance Java Persistence",
                new AuditInfo(
                    LocalDateTime.of(2016, 11, 2, 12, 0, 0),
                    "Vlad Mihalcea",
                    LocalDateTime.now(),
                    "Vlad Mihalcea"
                )
            );

            assertEquals(
                1L, postInfo.id().longValue()
            );

            assertEquals(
                "High-Performance Java Persistence", postInfo.title()
            );

            assertEquals(
                LocalDateTime.of(2016, 11, 2, 12, 0, 0), postInfo.auditInfo().createdOn()
            );

            assertEquals(
                "Vlad Mihalcea", postInfo.auditInfo().createdBy()
            );

            LOGGER.info("Audit info:\nenable-preview{}", postInfo.auditInfo());
            LOGGER.info("Post info:\n{}", postInfo);
        });
    }

    @Entity(name = "Post")
    public class Post {

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
        private Integer version;

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

        public LocalDateTime getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(LocalDateTime createdOn) {
            this.createdOn = createdOn;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }

        public LocalDateTime getUpdatedOn() {
            return updatedOn;
        }

        public void setUpdatedOn(LocalDateTime updatedOn) {
            this.updatedOn = updatedOn;
        }

        public String getUpdatedBy() {
            return updatedBy;
        }

        public void setUpdatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }
    }

    public static record AuditInfo(
        LocalDateTime createdOn,
        String createdBy,
        LocalDateTime updatedOn,
        String updatedBy
    ) {
        @Override
        public String toString() {
            return String.format("""
               AuditInfo {
                    createdOn : '%s',
                    createdBy : '%s',
                    updatedOn : '%s',
                    updatedBy : '%s'
                }
                """,
                createdOn,
                createdBy,
                updatedOn,
                updatedBy
            );
        }
    }
    
    public static record PostInfo(
        Long id,
        String title,
        AuditInfo auditInfo
    ) {
        @Override
        public String toString() {
            return String.format("""
                PostInfo {
                    id : '%s',
                    title : '%s',
                    auditInfo : {
                        createdOn : '%s',
                        createdBy : '%s',
                        updatedOn : '%s',
                        updatedBy : '%s'
                    }
                }
                """,
                id,
                title,
                auditInfo.createdOn,
                auditInfo.createdBy,
                auditInfo.updatedOn,
                auditInfo.updatedBy
            );
        }
    }
}
