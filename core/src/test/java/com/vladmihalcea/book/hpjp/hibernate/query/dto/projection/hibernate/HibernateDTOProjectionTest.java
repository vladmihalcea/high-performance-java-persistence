package com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.hibernate;

import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.Post;
import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.PostComment;
import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.transformer.PostDTO;
import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.transformer.PostDTOResultTransformer;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import com.vladmihalcea.hibernate.query.ListResultTransformer;
import org.hibernate.jpa.QueryHints;
import org.hibernate.transform.Transformers;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
            PostComment.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
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
                    .addComment(
                        new PostComment()
                            .setId(1L)
                            .setReview("Best book on JPA and Hibernate!")
                    )
                    .addComment(
                        new PostComment()
                            .setId(2L)
                            .setReview("A must-read for every Java developer!")
                    )
            );


            entityManager.persist(
                new Post()
                    .setId(2L)
                    .setTitle("Hypersistence Optimizer")
                    .setCreatedBy("Vlad Mihalcea")
                    .setCreatedOn(
                        LocalDateTime.of(2019, 3, 19, 12, 0, 0)
                    )
                    .setUpdatedBy("Vlad Mihalcea")
                    .setUpdatedOn(
                        LocalDateTime.now()
                    )
                    .addComment(
                        new PostComment()
                            .setId(3L)
                            .setReview("It's like pair programming with Vlad!")
                    )
            );
        });
    }

    @Test
    public void testJPQLResultTransformer() {
        doInJPA( entityManager -> {
            List<PostDTO> postDTOs = entityManager.createQuery("""
                select p.id as id,
                       p.title as title
                from Post p
                order by p.id
                """)
            .unwrap(org.hibernate.query.Query.class)
            .setResultTransformer(Transformers.aliasToBean(PostDTO.class))
            .getResultList();

            assertEquals(2, postDTOs.size());

            PostDTO postDTO = postDTOs.get(0);
            assertEquals(1L, postDTO.getId().longValue());
            assertEquals("High-Performance Java Persistence", postDTO.getTitle());
        } );
    }

    @Test
    public void testNativeQueryResultTransformer() {
        doInJPA( entityManager -> {
            List<PostDTO> postDTOs = entityManager.createNativeQuery("""
                SELECT p.id AS id,
                       p.title AS title
                FROM post p
                ORDER BY p.id
                """)
            .unwrap(org.hibernate.query.Query.class)
            .setResultTransformer(Transformers.aliasToBean(PostDTO.class))
            .getResultList();

            assertEquals(2, postDTOs.size());

            PostDTO postDTO = postDTOs.get(0);
            assertEquals(1L, postDTO.getId().longValue());
            assertEquals("High-Performance Java Persistence", postDTO.getTitle());
        } );
    }

    @Test
    public void testRecord() {
        doInJPA(entityManager -> {
            List<PostRecord> postRecords = entityManager.createQuery("""
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
            .unwrap(org.hibernate.query.Query.class)
            .setResultTransformer(
                (ListResultTransformer) (tuple, aliases) -> {
                    int i =0;
                    return new PostRecord(
                        ((Number) tuple[i++]).longValue(),
                        (String) tuple[i++],
                        new AuditRecord(
                            (LocalDateTime) tuple[i++],
                            (String) tuple[i++],
                            (LocalDateTime) tuple[i++],
                            (String) tuple[i++]
                        )
                    );
                }
            )
            .getResultList();

            assertEquals(2, postRecords.size());

            PostRecord postRecord = postRecords.get(0);

            assertEquals(
                1L, postRecord.id().longValue()
            );

            assertEquals(
                "High-Performance Java Persistence", postRecord.title()
            );

            assertEquals(
                LocalDateTime.of(2016, 11, 2, 12, 0, 0), postRecord.audit().createdOn()
            );

            assertEquals(
                "Vlad Mihalcea", postRecord.audit().createdBy()
            );
        });
    }

    @Test
    public void testParentChildDTOProjectionNativeQueryResultTransformer() {
        doInJPA( entityManager -> {
            List<PostDTO> postDTOs = entityManager.createNativeQuery("""
                SELECT p.id AS p_id, 
                       p.title AS p_title,
                       pc.id AS pc_id, 
                       pc.review AS pc_review
                FROM post p
                JOIN post_comment pc ON p.id = pc.post_id
                ORDER BY pc.id
                """)
            .unwrap(org.hibernate.query.Query.class)
            .setResultTransformer(new PostDTOResultTransformer())
            .getResultList();

            assertEquals(2, postDTOs.size());
            assertEquals(2, postDTOs.get(0).getComments().size());
            assertEquals(1, postDTOs.get(1).getComments().size());

            PostDTO post1DTO = postDTOs.get(0);

            assertEquals(1L, post1DTO.getId().longValue());
            assertEquals(2, post1DTO.getComments().size());
            assertEquals(1L, post1DTO.getComments().get(0).getId().longValue());
            assertEquals(2L, post1DTO.getComments().get(1).getId().longValue());

            PostDTO post2DTO = postDTOs.get(1);

            assertEquals(2L, post2DTO.getId().longValue());
            assertEquals(1, post2DTO.getComments().size());
            assertEquals(3L, post2DTO.getComments().get(0).getId().longValue());
        } );
    }

    @Test
    public void testParentChildDTOProjectionJPQLResultTransformer() {
        doInJPA( entityManager -> {
            List<PostDTO> postDTOs = entityManager.createQuery("""
                select p.id as p_id, 
                       p.title as p_title,
                       pc.id as pc_id, 
                       pc.review as pc_review
                from PostComment pc
                join pc.post p
                order by pc.id
                """)
            .unwrap(org.hibernate.query.Query.class)
            .setResultTransformer(new PostDTOResultTransformer())
            .getResultList();

            assertEquals(2, postDTOs.size());
            assertEquals(2, postDTOs.get(0).getComments().size());
            assertEquals(1, postDTOs.get(1).getComments().size());

            PostDTO post1DTO = postDTOs.get(0);

            assertEquals(1L, post1DTO.getId().longValue());
            assertEquals(2, post1DTO.getComments().size());
            assertEquals(1L, post1DTO.getComments().get(0).getId().longValue());
            assertEquals(2L, post1DTO.getComments().get(1).getId().longValue());

            PostDTO post2DTO = postDTOs.get(1);

            assertEquals(2L, post2DTO.getId().longValue());
            assertEquals(1, post2DTO.getComments().size());
            assertEquals(3L, post2DTO.getComments().get(0).getId().longValue());
        } );
    }

    @Test
    public void testParentChildEntityProjectionJPQLResultTransformer() {
        doInJPA( entityManager -> {
            List<Post> posts = entityManager.createQuery("""
                select distinct p
                from Post p
                join fetch p.comments pc
                order by pc.id
                """)
            .getResultList();

            assertEquals(2, posts.size());
            assertEquals(2, posts.get(0).getComments().size());
            assertEquals(1, posts.get(1).getComments().size());

            Post post1 = posts.get(0);

            assertEquals(1L, post1.getId().longValue());
            assertEquals(2, post1.getComments().size());
            assertEquals(1L, post1.getComments().get(0).getId().longValue());
            assertEquals(2L, post1.getComments().get(1).getId().longValue());

            Post post2 = posts.get(1);

            assertEquals(2L, post2.getId().longValue());
            assertEquals(1, post2.getComments().size());
            assertEquals(3L, post2.getComments().get(0).getId().longValue());
        } );
    }

    public static record PostRecord(
        Long id,
        String title,
        AuditRecord audit
    ) {
    }

    public static record AuditRecord(
        LocalDateTime createdOn,
        String createdBy,
        LocalDateTime updatedOn,
        String updatedBy
    ) {
    }
}
