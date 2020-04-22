package com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.hibernate;

import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.Post;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.transform.Transformers;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("unchecked")
public class HibernateDTOProjectionTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
        };
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
        });
    }

    @Test
    public void testJpqlResultTransformer() {
        doInJPA( entityManager -> {
            List<PostDTO> postDTOs = entityManager.createQuery("""
                select
                   p.id as id,
                   p.title as title
                from Post p
                """)
            .unwrap(org.hibernate.query.Query.class)
            .setResultTransformer(Transformers.aliasToBean(PostDTO.class))
            .getResultList();

            assertEquals(1, postDTOs.size());
        } );
    }

    @Test
    public void testNativeQueryResultTransformer() {
        doInJPA( entityManager -> {
            List<PostDTO> postDTOs = entityManager.createNativeQuery("""
                SELECT
                   p.id AS "id",
                   p.title AS "title"
                FROM Post p
                """)
            .unwrap(org.hibernate.query.NativeQuery.class)
            .setResultTransformer(Transformers.aliasToBean(PostDTO.class))
            .getResultList();

            assertEquals(1, postDTOs.size());
        } );
    }

    public static class PostDTO {

        private Long id;

        private String title;

        public Long getId() {
            return id;
        }

        public void setId(Number id) {
            this.id = id.longValue();
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}
