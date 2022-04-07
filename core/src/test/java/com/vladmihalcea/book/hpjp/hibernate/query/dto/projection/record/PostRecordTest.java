package com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.record;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.hibernate.type.util.ClassImportIntegrator;
import com.vladmihalcea.hibernate.query.ListResultTransformer;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.query.Query;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PostRecordTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(
            "hibernate.integrator_provider",
            (IntegratorProvider) () -> Collections.singletonList(
                new ClassImportIntegrator(
                    List.of(
                        AuditInfo.class,
                        PostInfo.class
                    )
                )
            )
        );
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
                    .setCreatedBy("Vlad Mihalcea")
                    .setCreatedOn(
                        LocalDateTime.of(2016, 11, 2, 12, 0, 0)
                    )
                    .setUpdatedBy("Vlad Mihalcea")
                    .setUpdatedOn(
                        LocalDateTime.now()
                    )
            );
            entityManager.persist(
                new Post()
                    .setId(2L)
                    .setTitle("Hypersistence Optimizer")
                    .setCreatedBy("Vlad Mihalcea")
                    .setCreatedOn(
                        LocalDateTime.of(2020, 3, 19, 12, 0, 0)
                    )
                    .setUpdatedBy("Vlad Mihalcea")
                    .setUpdatedOn(
                        LocalDateTime.now()
                    )
            );
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

            LOGGER.info("Audit info:\n{}", postInfo.auditInfo());
            LOGGER.info("Post info:\n{}", postInfo);
        });
    }

    @Test
    public void testRecordDTO() {
        doInJPA(entityManager -> {
            AuditInfo auditInfo = entityManager.createQuery("""
                select 
                    new AuditInfo (
                        p.createdOn,
                        p.createdBy,
                        p.updatedOn,
                        p.updatedBy
                    )
                from Post p
                where p.id = :postId
                """, AuditInfo.class)
            .setParameter("postId", 1L)
            .getSingleResult();

            assertEquals(
                LocalDateTime.of(2016, 11, 2, 12, 0, 0), auditInfo.createdOn()
            );

            assertEquals(
                "Vlad Mihalcea", auditInfo.createdBy()
            );

            assertEquals(
                "Vlad Mihalcea", auditInfo.updatedBy()
            );
        });

        doInJPA(entityManager -> {
            List<PostInfo> postInfos = entityManager.createQuery("""
                select 
                    p.id,
                    p.title,
                    p.createdOn,
                    p.createdBy,
                    p.updatedOn,
                    p.updatedBy
                from Post p
                order by p.id
                """)
            .unwrap(Query.class)
            .setResultTransformer(
                (ListResultTransformer) (tuple, aliases) -> {
                    int i =0;
                    return new PostInfo(
                        ((Number) tuple[i++]).longValue(),
                        (String) tuple[i++],
                        new AuditInfo(
                            (LocalDateTime) tuple[i++],
                            (String) tuple[i++],
                            (LocalDateTime) tuple[i++],
                            (String) tuple[i++]
                        )
                    );
                }
            )
            .getResultList();

            assertEquals(2, postInfos.size());

            PostInfo postInfo = postInfos.get(0);

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

        public Integer getVersion() {
            return version;
        }

        public Post setVersion(Integer version) {
            this.version = version;
            return this;
        }
    }

    public static record AuditInfo(
        LocalDateTime createdOn,
        String createdBy,
        LocalDateTime updatedOn,
        String updatedBy
    ) {}

    public static record PostInfo(
        Long id,
        String title,
        AuditInfo auditInfo
    ) {}
}
