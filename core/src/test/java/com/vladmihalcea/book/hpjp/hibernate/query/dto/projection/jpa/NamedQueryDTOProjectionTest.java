package com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.jpa;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class NamedQueryDTOProjectionTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
        };
    }

    @Override
    public void init() {
        super.init();

        doInJPA( entityManager -> {
            Post post = new Post();
            post.setId( 1L );
            post.setTitle( "High-Performance Java Persistence" );
            post.setCreatedBy( "Vlad Mihalcea" );
            post.setCreatedOn( Timestamp.from(
                    LocalDateTime.of( 2016, 11, 2, 12, 0, 0 ).toInstant( ZoneOffset.UTC)
            ) );
            post.setUpdatedBy( "Vlad Mihalcea" );
            post.setUpdatedOn( Timestamp.from(
                    LocalDateTime.now().toInstant( ZoneOffset.UTC)
            ) );

            entityManager.persist( post );
        } );
    }

    @Test
    public void testConstructorExpression() {
        doInJPA(entityManager -> {
            List<PostDTO> postDTOs = entityManager.createNamedQuery(
                "PostDTO", PostDTO.class)
            .setParameter("fromTimestamp", Timestamp.from(
                LocalDateTime.of(2016, 1, 1, 0, 0, 0).toInstant(ZoneOffset.UTC)))
            .getResultList();

            assertEquals(1, postDTOs.size());
        });
    }

    @NamedQuery(
        name = "PostDTO",
        query =
            "select new " +
            "   com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.jpa.PostDTO(" +
            "       p.id, " +
            "       p.title " +
            "   ) " +
            "from Post p " +
            "where p.createdOn > :fromTimestamp"
    )
    @Entity(name = "Post")
    public class Post {

        @Id
        private Long id;

        private String title;

        @Column(name = "created_on")
        private Timestamp createdOn;

        @Column(name = "created_by")
        private String createdBy;

        @Column(name = "updated_on")
        private Timestamp updatedOn;

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

        public Timestamp getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(Timestamp createdOn) {
            this.createdOn = createdOn;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }

        public Timestamp getUpdatedOn() {
            return updatedOn;
        }

        public void setUpdatedOn(Timestamp updatedOn) {
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


}
