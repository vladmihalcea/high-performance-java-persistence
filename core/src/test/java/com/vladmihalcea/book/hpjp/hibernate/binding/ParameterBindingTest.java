package com.vladmihalcea.book.hpjp.hibernate.binding;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.exception.DataAccessException;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * ParameterBindingTest - Test parameter binding in PreparedStatement
 *
 * @author Vlad Mihalcea
 */
public class ParameterBindingTest extends AbstractTest {

    public static final String INSERT_POST = "insert into post (title, version, id) values (?, ?, ?)";

    public static final String INSERT_POST_COMMENT = "insert into post_comment (post_id, review, version, id) values (?, ?, ?, ?)";

    public static final String INSERT_POST_DETAILS= "insert into post_details (id, created_on, version) values (?, ?, ?)";

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    private int version = 5;
    private long expectedCount = 40;

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Override
    public void init() {
        super.init();
        doInJDBC(connection -> {
            try (
                    PreparedStatement postStatement = connection.prepareStatement(INSERT_POST);
                    PreparedStatement postCommentStatement = connection.prepareStatement(INSERT_POST_COMMENT);
                    PreparedStatement postDetailsStatement = connection.prepareStatement(INSERT_POST_DETAILS);
            ) {

                int postCount = getPostCount();
                int postCommentCount = getPostCommentCount();

                int index;

                for (int i = 0; i < postCount; i++) {
                    if (i > 0 && i % 100 == 0) {
                        postStatement.executeBatch();
                        postDetailsStatement.executeBatch();
                    }

                    index = 0;
                    postStatement.setString(++index, String.format("Post no. %1$d", i));
                    postStatement.setInt(++index, i);
                    postStatement.setLong(++index, i);
                    postStatement.addBatch();

                    index = 0;
                    postDetailsStatement.setInt(++index, i);
                    postDetailsStatement.setTimestamp(++index, new Timestamp(System.currentTimeMillis()));
                    postDetailsStatement.setInt(++index, i);
                    postDetailsStatement.addBatch();
                }
                postStatement.executeBatch();
                postDetailsStatement.executeBatch();

                for (int i = 0; i < postCount; i++) {
                    for (int j = 0; j < postCommentCount; j++) {
                        index = 0;
                        postCommentStatement.setLong(++index, i);
                        postCommentStatement.setString(++index, String.format("Post comment %1$d", j));
                        postCommentStatement.setInt(++index, i);
                        postCommentStatement.setLong(++index, (postCommentCount * i) + j);
                        postCommentStatement.addBatch();
                        if (j % 100 == 0) {
                            postCommentStatement.executeBatch();
                        }
                    }
                }
                postCommentStatement.executeBatch();
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void testJdbcParameterBinding() {
        doInJDBC(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*)  " +
                    "FROM post_comment pc " +
                    "JOIN post p ON p.id = pc.post_id " +
                    "JOIN post_details pd ON p.id = pd.id " +
                    "WHERE " +
                    "   pc.version > ? AND " +
                    "   p.version > ? AND " +
                    "   pd.version > ? "
            )) {
                statement.setInt(1, version);
                statement.setInt(2, version);
                statement.setInt(3, version);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    assertEquals(expectedCount, resultSet.getLong(1));
                }
            } catch (SQLException e) {
                throw new DataAccessException( e);
            }
        });
    }

    @Test
    public void testJPAParameterBinding() {
        doInJPA(entityManager -> {
            long actualCount = entityManager.createQuery(
                "select count(*) " +
                "from PostComment pc " +
                "join pc.post p " +
                "join p.details pd " +
                "where " +
                "   pc.version > :version and " +
                "   p.version > :version and " +
                "   pd.version > :version ",
                Long.class)
                .setParameter("version", version)
                .getSingleResult();
            assertEquals(expectedCount, actualCount);
        });
    }

    protected int getPostCount() {
        return 10;
    }

    protected int getPostCommentCount() {
        return 10;
    }
}
