package com.vladmihalcea.hpjp.jdbc.fetching;

import com.vladmihalcea.hpjp.hibernate.forum.Post;
import com.vladmihalcea.hpjp.hibernate.forum.PostComment;
import com.vladmihalcea.hpjp.hibernate.forum.PostDetails;
import com.vladmihalcea.hpjp.hibernate.forum.Tag;
import com.vladmihalcea.hpjp.util.AbstractTest;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class JDBCVsJPATest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Post.class,
            PostDetails.class,
            PostComment.class,
            Tag.class
        };
    }

    @Test
    public void testWriteAndReadUsingJDBC() {
        doInJDBC(connection -> {
            int postCount = 100;
            int batchSize = 50;

            try (PreparedStatement postStatement = connection.prepareStatement("""
                INSERT INTO post (
                    id,
                    title 
                ) 
                VALUES (
                    ?, 
                    ?
                )
                """
            )) {
                for (int i = 1; i <= postCount; i++) {
                    if (i % batchSize == 0) {
                        postStatement.executeBatch();
                    }
                    int index = 0;
                    postStatement.setLong(++index, i);
                    postStatement.setString(++index, String.format("High-Performance Java Persistence, review no. %1$d", i));
                    postStatement.addBatch();
                }
                postStatement.executeBatch();
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });

        doInJDBC(connection -> {
            int maxResults = 10;

            List<Post> posts = new ArrayList<>();

            try (PreparedStatement preparedStatement = connection.prepareStatement("""
                    SELECT 
                        p.id AS id, 
                        p.title AS title
                    FROM post p 
                    ORDER BY p.id
                    LIMIT ?
                    """
            )) {
                preparedStatement.setInt(1, maxResults);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        int index = 0;
                        posts.add(
                            new Post()
                                .setId(resultSet.getLong(++index))
                                .setTitle(resultSet.getString(++index))
                        );
                    }
                }

            } catch (SQLException e) {
                fail(e.getMessage());
            }
            assertEquals(maxResults, posts.size());
        });
    }

    @Test
    public void testWriteAndReadUsingJPA() {
        doInJPA(entityManager -> {
            int postCount = 100;

            for (long i = 1; i <= postCount; i++) {
                entityManager.persist(
                    new Post()
                        .setId(i)
                        .setTitle(
                            String.format(
                                "High-Performance Java Persistence, review no. %1$d",
                                i
                            )
                        )
                );
            }
        });

        doInJPA(entityManager -> {
            int maxResults = 10;

            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p 
                order by p.id
                """, Post.class)
            .setMaxResults(maxResults)
            .getResultList();

            assertEquals(maxResults, posts.size());
        });
    }

    @Override
    protected boolean proxyDataSource() {
        return false;
    }
}
