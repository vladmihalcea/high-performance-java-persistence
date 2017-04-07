package com.vladmihalcea.book.hpjp.jdbc.caching;

import com.vladmihalcea.book.hpjp.util.AbstractOracleXEIntegrationTest;
import com.vladmihalcea.book.hpjp.util.ReflectionUtils;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.OracleDataSourceProvider;

import org.junit.Test;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * OracleImplicitStatementCacheTest - Test Oracle implicit Statement cache
 *
 * @author Vlad Mihalcea
 */
public class OracleImplicitStatementCacheTest extends AbstractOracleXEIntegrationTest {

    public static final String INSERT_POST = "insert into post (title, version, id) values (?, ?, ?)";

    public static final String INSERT_POST_COMMENT = "insert into post_comment (post_id, review, version, id) values (?, ?, ?, ?)";

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new OracleDataSourceProvider() {
            @Override
            public DataSource dataSource() {
                DataSource dataSource = super.dataSource();
                try {
                    Properties connectionProperties = ReflectionUtils.invokeGetter(dataSource, "connectionProperties");
                    if(connectionProperties == null) {
                        connectionProperties = new Properties();
                    }
                    connectionProperties.put("oracle.jdbc.implicitStatementCacheSize", "5");
                    ReflectionUtils.invokeSetter(dataSource, "connectionProperties", connectionProperties);
                } catch (Exception e) {
                    fail(e.getMessage());
                }
                return dataSource;
            }
        };
    }

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
        selectWhenCaching(true);
    }

    @Test
    public void testStatementWithoutCaching() {
        selectWhenCaching(false);
    }

    private void selectWhenCaching(boolean caching) {
        long startNanos = System.nanoTime();
        doInJDBC(connection -> {
            ReflectionUtils.invokeSetter(connection, "implicitCachingEnabled", false);
            assertFalse(ReflectionUtils.invokeGetter(connection, "implicitCachingEnabled"));
            assertEquals(Integer.valueOf(5), ReflectionUtils.invokeGetter(connection, "statementCacheSize"));

            for (int i = 0; i < 1; i++) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "select p.title, pc.review " +
                                "from post p left join post_comment pc on p.id = pc.post_id " +
                                "where EXISTS ( " +
                                "   select 1 from post_comment where version = ? and id > p.id " +
                                ")"
                )) {
                    if (statement.isPoolable()) {
                        statement.setPoolable(caching);
                    }
                    statement.setInt(1, i);
                    statement.execute();
                } catch (SQLException e) {
                    fail(e.getMessage());
                }
            }
        });
        LOGGER.info("{} when caching Statements is {} took {} millis",
                getClass().getSimpleName(),
                caching,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
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
