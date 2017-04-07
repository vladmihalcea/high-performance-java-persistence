package com.vladmihalcea.book.hpjp.hibernate.binding;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.exception.DataAccessException;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider.Post;
import static com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider.PostComment;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * EntityGraphMapperTest - Test mapping to entity
 *
 * @author Vlad Mihalcea
 */
public class EntityGraphMapperTest extends AbstractTest {

    public static final String INSERT_POST = "insert into post (title, version, id) values (?, ?, ?)";

    public static final String INSERT_POST_COMMENT = "insert into post_comment (post_id, review, version, id) values (?, ?, ?, ?)";

    public static final String INSERT_POST_DETAILS= "insert into post_details (id, created_on, version) values (?, ?, ?)";

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    private Long id = 1L;
    private long expectedCount = 2;

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
    public void testJdbcOneToManyMapping() {
        doInJDBC(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * " +
                    "FROM post AS p " +
                    "JOIN post_comment AS pc ON p.id = pc.post_id " +
                    "WHERE " +
                    "   p.id BETWEEN ? AND ? + 1"
            )) {
                statement.setLong(1, id);
                statement.setLong(2, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<Post> posts = toPosts(resultSet);
                    assertEquals(expectedCount, posts.size());
                }
            } catch (SQLException e) {
                throw new DataAccessException( e);
            }
        });
    }

    private List<Post> toPosts(ResultSet resultSet) throws SQLException {
        Map<Long, Post> postMap = new LinkedHashMap<>();
        while (resultSet.next()) {
            Long postId = resultSet.getLong(1);
            Post post = postMap.get(postId);
            if(post == null) {
                post = new Post(postId);
                postMap.put(postId, post);
                post.setTitle(resultSet.getString(2));
                post.setVersion(resultSet.getInt(3));
            }
            PostComment comment = new PostComment();
            comment.setId(resultSet.getLong(4));
            comment.setReview(resultSet.getString(5));
            comment.setVersion(resultSet.getInt(6));
            post.addComment(comment);
        }
        return new ArrayList<>(postMap.values());
    }

    @Test
    public void testJPAParameterBinding() {
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery(
                "select distinct p " +
                "from Post p " +
                "join fetch p.comments " +
                "where " +
                "   p.id BETWEEN :id AND :id + 1",
                Post.class)
                .setParameter("id", id)
                .getResultList();
            assertEquals(expectedCount, posts.size());
        });
    }

    protected int getPostCount() {
        return 10;
    }

    protected int getPostCommentCount() {
        return 10;
    }
}
