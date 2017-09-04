package com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.hibernate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.hibernate.transform.Transformers;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.Post;
import com.vladmihalcea.book.hpjp.util.AbstractTest;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class HibernateDTOProjectionTest extends AbstractTest {

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
    public void testJpqlResultTransformer() {
        doInJPA( entityManager -> {
            List<PostDTO> postDTOs = entityManager.createQuery(
                "select " +
                "       p.id as id, " +
                "       p.title as title " +
                "from Post p " +
                "where p.createdOn > :fromTimestamp")
            .setParameter( "fromTimestamp", Timestamp.from(
                LocalDateTime.of( 2016, 1, 1, 0, 0, 0 ).toInstant( ZoneOffset.UTC ) ))
            .unwrap( org.hibernate.query.Query.class )
            .setResultTransformer( Transformers.aliasToBean( PostDTO.class ) )
            .getResultList();

            assertEquals( 1, postDTOs.size() );
        } );
    }

    @Test
    public void testNativeQueryResultTransformer() {
        doInJPA( entityManager -> {
            List postDTOs = entityManager.createNativeQuery(
                "select " +
                "       p.id as \"id\", " +
                "       p.title as \"title\" " +
                "from Post p " +
                "where p.created_on > :fromTimestamp")
            .setParameter( "fromTimestamp", Timestamp.from(
                LocalDateTime.of( 2016, 1, 1, 0, 0, 0 ).toInstant( ZoneOffset.UTC ) ))
            .unwrap( org.hibernate.query.NativeQuery.class )
            .setResultTransformer( Transformers.aliasToBean( PostDTO.class ) )
            .getResultList();

            assertEquals( 1, postDTOs.size() );
        } );
    }

}
