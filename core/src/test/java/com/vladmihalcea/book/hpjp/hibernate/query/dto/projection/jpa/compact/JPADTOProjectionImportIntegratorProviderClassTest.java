package com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.jpa.compact;

import com.vladmihalcea.book.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.Post;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class JPADTOProjectionImportIntegratorProviderClassTest extends AbstractTest {

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
            ClassImportIntegratorIntegratorProvider.class
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
                        Timestamp.from(
                            LocalDateTime.of(2016, 11, 2, 12, 0, 0).toInstant(ZoneOffset.UTC)
                        )
                    )
                    .setUpdatedBy("Vlad Mihalcea")
                    .setUpdatedOn(
                        Timestamp.from(
                            LocalDateTime.now().toInstant(ZoneOffset.UTC)
                        )
                    )
            );
        });
    }

    @Test
    public void testConstructorExpression() {
        doInJPA(entityManager -> {
            List<PostDTO> postDTOs = entityManager.createQuery("""
                select new PostDTO(
                    p.id,
                    p.title
                )
                from Post p
                where p.createdOn > :fromTimestamp
                """, PostDTO.class)
            .setParameter(
                "fromTimestamp",
                Timestamp.from(
                    LocalDate.of(2016, 1, 1)
                        .atStartOfDay()
                        .toInstant(ZoneOffset.UTC)
                )
            )
            .getResultList();

            assertEquals(1, postDTOs.size());

            PostDTO postDTO = postDTOs.get(0);
            assertEquals(1L, postDTO.getId().longValue());
            assertEquals("High-Performance Java Persistence", postDTO.getTitle());
        });
    }
}
