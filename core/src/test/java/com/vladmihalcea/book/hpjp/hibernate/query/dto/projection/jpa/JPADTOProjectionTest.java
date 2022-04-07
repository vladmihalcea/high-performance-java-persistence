package com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.jpa;

import com.vladmihalcea.book.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.Post;
import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.PostComment;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.hibernate.type.util.ClassImportIntegrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.junit.Test;

import jakarta.persistence.Tuple;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("unchecked")
public class JPADTOProjectionTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(
            "hibernate.integrator_provider",
            (IntegratorProvider) () -> Collections.singletonList(
                new ClassImportIntegrator(
                    List.of(
                        PostDTO.class,
                        PostRecord.class
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
        });
    }

    @Test
    public void testDefaultProjection() {
        doInJPA(entityManager -> {
            List<Object[]> tuples = entityManager.createQuery("""
                select 
                    p.id,
                    p.title
                from Post p
                """)
            .getResultList();

            assertEquals(1, tuples.size());

            Object[] tuple = tuples.get(0);
            long id = ((Number) tuple[0]).longValue();
            String title = (String) tuple[1];

            assertEquals(1L, id);
            assertEquals("High-Performance Java Persistence", title);
        });
    }

    @Test
    public void testDefaultProjectionNativeQuery() {
        doInJPA(entityManager -> {
            List<Object[]> tuples = entityManager.createNativeQuery("""
                SELECT
                   p.id AS id,
                   p.title AS title
                FROM post p
                """
            )
            .getResultList();

            assertEquals(1, tuples.size());

            Object[] tuple = tuples.get(0);
            long id = ((Number) tuple[0]).longValue();
            String title = (String) tuple[1];

            assertEquals(1L, id);
            assertEquals("High-Performance Java Persistence", title);
        });
    }

    @Test
    public void testTuple() {
        doInJPA(entityManager -> {
            List<Tuple> tuples = entityManager.createQuery("""
                select
                   p.id as id,
                   p.title as title
                from Post p
                """, Tuple.class)
            .getResultList();

            assertEquals(1, tuples.size());

            Tuple tuple = tuples.get(0);
            long id = tuple.get("id", Number.class).longValue();
            String title = tuple.get("title", String.class);

            assertEquals(1L, id);
            assertEquals("High-Performance Java Persistence", title);
        });
    }

    @Test
    public void testTupleNativeQuery() {
        doInJPA(entityManager -> {
            List<Tuple> tuples = entityManager.createNativeQuery("""
                SELECT
                   p.id AS id,
                   p.title AS title
                FROM post p
                """, Tuple.class)
            .getResultList();

            assertEquals(1, tuples.size());

            Tuple tuple = tuples.get(0);
            long id = tuple.get("id", Number.class).longValue();
            String title = tuple.get("title", String.class);

            assertEquals(1L, id);
            assertEquals("High-Performance Java Persistence", title);
        });
    }

    @Test
    public void testConstructorExpression() {
        doInJPA(entityManager -> {
            List<PostDTO> postDTOs = entityManager.createQuery("""
                select new com.vladmihalcea.book.hpjp.hibernate.forum.dto.PostDTO(
                    p.id,
                    p.title
                )
                from Post p
                """, PostDTO.class)
            .getResultList();

            assertEquals(1, postDTOs.size());

            PostDTO postDTO = postDTOs.get(0);
            assertEquals(1L, postDTO.getId().longValue());
            assertEquals("High-Performance Java Persistence", postDTO.getTitle());
        });
    }

    @Test
    public void testConstructorExpressionSimpleClassName() {
        doInJPA(entityManager -> {
            List<PostDTO> postDTOs = entityManager.createQuery("""
                select new PostDTO(
                    p.id,
                    p.title
                )
                from Post p
                """, PostDTO.class)
            .getResultList();

            assertEquals(1, postDTOs.size());

            PostDTO postDTO = postDTOs.get(0);
            assertEquals(1L, postDTO.getId().longValue());
            assertEquals("High-Performance Java Persistence", postDTO.getTitle());
        });
    }

    @Test
    public void testNamedQuery() {
        doInJPA(entityManager -> {
            List<PostDTO> postDTOs = entityManager.createNamedQuery(
                "PostDTOEntityQuery", PostDTO.class)
            .getResultList();

            assertEquals(1, postDTOs.size());

            PostDTO postDTO = postDTOs.get(0);
            assertEquals(1L, postDTO.getId().longValue());
            assertEquals("High-Performance Java Persistence", postDTO.getTitle());
        });
    }

    @Test
    public void testNamedNativeQuery() {
        doInJPA(entityManager -> {
            List<PostDTO> postDTOs = entityManager.createNamedQuery(
                "PostDTONativeQuery")
            .getResultList();

            assertEquals(1, postDTOs.size());

            PostDTO postDTO = postDTOs.get(0);
            assertEquals(1L, postDTO.getId().longValue());
            assertEquals("High-Performance Java Persistence", postDTO.getTitle());
        });
    }

    @Test
    public void testRecord() {
        doInJPA(entityManager -> {
            List<PostRecord> postRecords = entityManager.createQuery("""
                select new PostRecord(
                    p.id,
                    p.title
                )
                from Post p
                """, PostRecord.class)
            .getResultList();

            assertEquals(1, postRecords.size());

            PostRecord postRecord = postRecords.get(0);
            assertEquals(1L, postRecord.id().longValue());
            assertEquals("High-Performance Java Persistence", postRecord.title());
        });
    }

    public static record PostRecord(
        Number id,
        String title
    ) {
    }
}
