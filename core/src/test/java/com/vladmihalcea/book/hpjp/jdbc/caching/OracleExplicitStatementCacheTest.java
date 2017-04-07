package com.vladmihalcea.book.hpjp.jdbc.caching;

import com.vladmihalcea.book.hpjp.util.AbstractOracleXEIntegrationTest;
import com.vladmihalcea.book.hpjp.util.ReflectionUtils;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.Assert.fail;

/**
 * OracleImplicitStatementCacheTest - Test Oracle implicit Statement cache
 *
 * @author Vlad Mihalcea
 */
public class OracleExplicitStatementCacheTest extends AbstractOracleXEIntegrationTest {

    public static final String INSERT_POST = "insert into post (title, version, id) values (?, ?, ?)";

    public static final String INSERT_POST_COMMENT = "insert into post_comment (post_id, review, version, id) values (?, ?, ?, ?)";

    public static final String SELECT_POST_REVIEWS =
            "select p.title, pc.review " +
                    "from post p left join post_comment pc on p.id = pc.post_id " +
                    "where EXISTS ( " +
                    "   select 1 from post_comment where version = ? and id > p.id " +
                    ")";

    public static final String SELECT_POST_REVIEWS_KEY = "post_reviews";

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

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
            ) {
                int postCount = getPostCount();
                int postCommentCount = getPostCommentCount();

                int index;

                for (int i = 0; i < postCount; i++) {
                    index = 0;
                    postStatement.setString(++index, String.format("Post no. %1$d", i));
                    postStatement.setInt(++index, 0);
                    postStatement.setLong(++index, i);
                    postStatement.executeUpdate();
                }

                for (int i = 0; i < postCount; i++) {
                    for (int j = 0; j < postCommentCount; j++) {
                        index = 0;
                        postCommentStatement.setLong(++index, i);
                        postCommentStatement.setString(++index, String.format("Post comment %1$d", j));
                        postCommentStatement.setInt(++index, (int) (Math.random() * 1000));
                        postCommentStatement.setLong(++index, (postCommentCount * i) + j);
                        postCommentStatement.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void testStatementCaching() {
        doInJDBC(connection -> {
            for (int i = 0; i < 5; i++) {
                ReflectionUtils.invokeSetter(connection,"explicitCachingEnabled", true);
                ReflectionUtils.invokeSetter(connection,"statementCacheSize", 1);
                PreparedStatement statement = ReflectionUtils.invoke(connection, ReflectionUtils.getMethod(connection, "getStatementWithKey", String.class), SELECT_POST_REVIEWS_KEY);
                if (statement == null)
                    statement = connection.prepareStatement(SELECT_POST_REVIEWS);
                try {
                    statement.setInt(1, 10);
                    statement.execute();
                } finally {
                    ReflectionUtils.invoke(statement, ReflectionUtils.getMethod(statement, "closeWithKey", String.class), SELECT_POST_REVIEWS_KEY);
                }
            }
        });
    }

    protected int getPostCount() {
        return 1000;
    }

    protected int getPostCommentCount() {
        return 5;
    }

    @Override
    protected boolean proxyDataSource() {
        return false;
    }
}
