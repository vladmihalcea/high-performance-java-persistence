package com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.jpa;

import com.vladmihalcea.book.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.Post;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.hibernate.type.util.ClassImportIntegrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.junit.Test;

import javax.persistence.Tuple;
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
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(
            "hibernate.integrator_provider",
            (IntegratorProvider) () -> Collections.singletonList(
                new ClassImportIntegrator(
                    Collections.singletonList(PostDTO.class)
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
            assertEquals(1L, ((Number) tuple[0]).longValue());
            assertEquals("High-Performance Java Persistence", tuple[1]);
        });
    }

    @Test
    public void testDefaultProjectionNativeQuery() {
        doInJPA(entityManager -> {
            List<Object[]> tuples = entityManager.createNativeQuery("""
                SELECT
                   p.id AS id,
                   p.title AS title
                FROM Post p
                """
            )
            .getResultList();

            assertEquals(1, tuples.size());

            Object[] tuple = tuples.get(0);
            assertEquals(1L, ((Number) tuple[0]).longValue());
            assertEquals("High-Performance Java Persistence", tuple[1]);
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
            assertEquals(1L, tuple.get("id"));
            assertEquals("High-Performance Java Persistence", tuple.get("title"));
        });
    }

    @Test
    public void testTupleNativeQuery() {
        doInJPA(entityManager -> {
            List<Tuple> tuples = entityManager.createNativeQuery("""
                SELECT
                   p.id AS id,
                   p.title AS title
                FROM Post p
                """, Tuple.class)
            .getResultList();

            assertEquals(1, tuples.size());

            Tuple tuple = tuples.get(0);
            assertEquals(1L, ((Number) tuple.get("id")).longValue());
            assertEquals("High-Performance Java Persistence", tuple.get("title"));
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
}
