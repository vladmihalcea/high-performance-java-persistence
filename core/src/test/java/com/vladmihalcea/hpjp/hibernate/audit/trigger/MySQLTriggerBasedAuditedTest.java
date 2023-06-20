package com.vladmihalcea.hpjp.hibernate.audit.trigger;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import io.hypersistence.utils.hibernate.type.json.JsonNodeStringType;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.usertype.UserType;
import org.junit.Test;
import com.vladmihalcea.hpjp.util.ReflectionUtils;

import jakarta.persistence.*;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MySQLTriggerBasedAuditedTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Post.class
        };
    }

    @Override
    protected Database database() {
        return Database.MYSQL;
    }

    @Override
    protected List<UserType<?>> additionalTypes() {
        return List.of(JsonNodeStringType.INSTANCE);
    }

    @Override
    protected void afterInit() {
        executeStatement("DROP TABLE IF EXISTS post_AUD");
        executeStatement("""
            CREATE TABLE IF NOT EXISTS post_AUD (
            	id BIGINT NOT NULL,
            	title VARCHAR(255),
            	REV_TYPE ENUM('INSERT', 'UPDATE', 'DELETE') NOT NULL,
            	REV_TIMESTAMP DATETIME NOT NULL,
            	REV_CREATED_BY VARCHAR(255) NOT NULL,
            	PRIMARY KEY (id, REV_TYPE, REV_TIMESTAMP)
            )
            """
        );

        executeStatement("""          
            CREATE TRIGGER post_insert_audit_trigger
            AFTER INSERT ON post
            FOR EACH ROW BEGIN
            INSERT INTO post_AUD (
                id,
                title,
                REV_TYPE,
                REV_TIMESTAMP,
                REV_CREATED_BY
            )
            VALUES(
                NEW.id,
                NEW.title,
                'INSERT',
                CURRENT_TIMESTAMP,
                @logged_user
            );
            END
            """
        );

        executeStatement("""
            CREATE TRIGGER post_update_audit_trigger
            AFTER UPDATE ON post
            FOR EACH ROW BEGIN
            INSERT INTO post_AUD (
                id,
                title,
                REV_TYPE,
                REV_TIMESTAMP,
                REV_CREATED_BY
            )
            VALUES(
                NEW.id,
                NEW.title,
                'UPDATE',
                CURRENT_TIMESTAMP,
                @logged_user
            );
            END
            """
        );

        executeStatement("""
            CREATE TRIGGER post_delete_audit_trigger
            AFTER DELETE ON post
            FOR EACH ROW BEGIN
            INSERT INTO post_AUD (
                id,
                title,
                REV_TYPE,
                REV_TIMESTAMP,
                REV_CREATED_BY
            )
            VALUES(
                OLD.id,
                OLD.title,
                'DELETE',
                CURRENT_TIMESTAMP,
                @logged_user
            );
            END
            """
        );
    }

    @Test
    public void test() {
        LoggedUser.logIn("Vlad Mihalcea");

        doInJPA(entityManager -> {
            setCurrentLoggedUser(entityManager);

            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence 1st edition");
            entityManager.persist(post);
        });

        doInJPA(entityManager -> {
            List<Tuple> revisions = getPostRevisions(entityManager);

            assertEquals(1, revisions.size());
        });

        doInJPA(entityManager -> {
            setCurrentLoggedUser(entityManager);

            Post post = entityManager.find(Post.class, 1L);
            post.setTitle("High-Performance Java Persistence 2nd edition");
        });

        doInJPA(entityManager -> {
            List<Tuple> revisions = getPostRevisions(entityManager);

            assertEquals(2, revisions.size());
        });

        doInJPA(entityManager -> {
            setCurrentLoggedUser(entityManager);

            entityManager.remove(
                entityManager.getReference(Post.class, 1L)
            );
        });

        doInJPA(entityManager -> {
            List<Tuple> revisions = getPostRevisions(entityManager);

            assertEquals(3, revisions.size());
        });
    }

    private void setCurrentLoggedUser(EntityManager entityManager) {
        Session session = entityManager.unwrap(Session.class);
        Dialect dialect = session.getSessionFactory().unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect();
        String loggedUser = ReflectionUtils.invokeMethod(
            dialect,
            "inlineLiteral",
            LoggedUser.get()
        );

        session.doWork(connection -> {
            update(
                connection,
                String.format(
                    "SET @logged_user = %s", loggedUser
                )
            );
        });
    }

    private List<Tuple> getPostRevisions(EntityManager entityManager) {
        return entityManager.createNativeQuery("""
            SELECT *
            FROM post_AUD
            ORDER BY REV_TIMESTAMP 
            """, Tuple.class)
        .getResultList();
    }

    public static class LoggedUser {

        private static final ThreadLocal<String> userHolder = new ThreadLocal<>();

        public static void logIn(String user) {
            userHolder.set(user);
        }

        public static void logOut() {
            userHolder.remove();
        }

        public static String get() {
            return userHolder.get();
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return "Post{" +
                   "id=" + id +
                   ", title='" + title + '\'' +
                   '}';
        }
    }
}
