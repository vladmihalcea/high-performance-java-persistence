package com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.record;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PostLegacyTest extends AbstractTest {

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
                1L, postInfo.getId().longValue()
            );

            assertEquals(
                "High-Performance Java Persistence", postInfo.getTitle()
            );

            assertEquals(
                LocalDateTime.of(2016, 11, 2, 12, 0, 0), postInfo.getAuditInfo().getCreatedOn()
            );

            assertEquals(
                "Vlad Mihalcea", postInfo.getAuditInfo().getCreatedBy()
            );

            LOGGER.info("Audit info:\n{}", postInfo.getAuditInfo());
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

    public static class AuditInfo {
    
        private final LocalDateTime createdOn;
    
        private final String createdBy;
    
        private final LocalDateTime updatedOn;
    
        private final String updatedBy;
    
        public AuditInfo(
                LocalDateTime createdOn,
                String createdBy,
                LocalDateTime updatedOn,
                String updatedBy) {
            this.createdOn = createdOn;
            this.createdBy = createdBy;
            this.updatedOn = updatedOn;
            this.updatedBy = updatedBy;
        }
    
        public LocalDateTime getCreatedOn() {
            return createdOn;
        }
    
        public String getCreatedBy() {
            return createdBy;
        }
    
        public LocalDateTime getUpdatedOn() {
            return updatedOn;
        }
    
        public String getUpdatedBy() {
            return updatedBy;
        }
    
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AuditInfo)) return false;
    
            AuditInfo auditInfo = (AuditInfo) o;
            return createdOn.equals(auditInfo.createdOn) &&
                   createdBy.equals(auditInfo.createdBy) &&
                   Objects.equals(updatedOn, auditInfo.updatedOn) &&
                   Objects.equals(updatedBy, auditInfo.updatedBy);
        }
    
        @Override
        public int hashCode() {
            return Objects.hash(createdOn, createdBy, updatedOn, updatedBy);
        }
    
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

    public static class PostInfo {
    
        private final Long id;
    
        private final String title;
    
        private final AuditInfo auditInfo;
    
        public PostInfo(
                Long id,
                String title,
                AuditInfo auditInfo) {
            this.id = id;
            this.title = title;
            this.auditInfo = auditInfo;
        }
    
        public Long getId() {
            return id;
        }
    
        public String getTitle() {
            return title;
        }
    
        public AuditInfo getAuditInfo() {
            return auditInfo;
        }
    
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PostInfo)) return false;
            PostInfo postInfo = (PostInfo) o;
            return id.equals(postInfo.id) &&
                   title.equals(postInfo.title) &&
                   auditInfo.equals(postInfo.auditInfo);
        }
    
        @Override
        public int hashCode() {
            return Objects.hash(id, title, auditInfo);
        }
    
        @Override
        public String toString() {
            return String.format("""
                PostInfo {
                    id : '%s',
                    title : '%s',
                    auditInfo : '%s'
                }
                """,
                id,
                title,
                auditInfo
            );
        }
    }
}
