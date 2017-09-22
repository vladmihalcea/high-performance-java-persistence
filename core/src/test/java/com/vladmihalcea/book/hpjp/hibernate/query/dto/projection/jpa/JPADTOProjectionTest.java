package com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.jpa;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import javax.persistence.Tuple;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.Post;
import com.vladmihalcea.book.hpjp.util.AbstractTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Vlad Mihalcea
 */
public class JPADTOProjectionTest extends AbstractTest {

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
        doInJPA( entityManager -> {
            List<PostDTO> postDTOs = entityManager.createQuery(
                "select new " +
                "   com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.jpa.PostDTO(" +
                "       p.id, " +
                "       p.title " +
                "   ) " +
                "from Post p " +
                "where p.createdOn > :fromTimestamp", PostDTO.class)
            .setParameter( "fromTimestamp", Timestamp.from(
                LocalDateTime.of( 2016, 1, 1, 0, 0, 0 ).toInstant( ZoneOffset.UTC ) ))
            .getResultList();

            assertEquals( 1, postDTOs.size() );
        } );
    }

    @Test
    public void testTuple() {
        doInJPA( entityManager -> {
            List<Tuple> postDTOs = entityManager.createQuery(
                "select " +
                "       p.id as id, " +
                "       p.title as title " +
                "from Post p " +
                "where p.createdOn > :fromTimestamp", Tuple.class)
            .setParameter( "fromTimestamp", Timestamp.from(
                LocalDateTime.of( 2016, 1, 1, 0, 0, 0 ).toInstant( ZoneOffset.UTC ) ))
            .getResultList();

            assertFalse( postDTOs.isEmpty() );

            Tuple postDTO = postDTOs.get( 0 );
            assertEquals( 1L, postDTO.get( "id" ) );
            assertEquals( "High-Performance Java Persistence", postDTO.get( "title" ) );
        } );
    }

    @Test
    public void testTupleNativeQuery() {
        doInJPA( entityManager -> {

            List<Tuple> postDTOs = entityManager.createNativeQuery(
                "SELECT " +
                "       p.id AS id, " +
                "       p.title AS title " +
                "FROM Post p " +
                "WHERE p.created_on > :fromTimestamp", Tuple.class)
            .setParameter( "fromTimestamp", Timestamp.from(
                LocalDateTime.of( 2016, 1, 1, 0, 0, 0 ).toInstant( ZoneOffset.UTC ) ))
            .getResultList();

            assertFalse( postDTOs.isEmpty() );

            Tuple postDTO = postDTOs.get( 0 );
            assertEquals( 1L, ((Number) postDTO.get( "id" )).longValue() );
            assertEquals( "High-Performance Java Persistence", postDTO.get( "title" ) );
        } );
    }

    @Test
    public void testConstructorResultNativeQuery() {
        doInJPA( entityManager -> {
            List<PostDTO> postDTOs = entityManager.createNamedQuery("PostDTO")
            .setParameter( "fromTimestamp", Timestamp.from(
                LocalDateTime.of( 2016, 1, 1, 0, 0, 0 ).toInstant( ZoneOffset.UTC ) ))
            .getResultList();

            assertEquals( 1, postDTOs.size() );
        } );
    }

}
