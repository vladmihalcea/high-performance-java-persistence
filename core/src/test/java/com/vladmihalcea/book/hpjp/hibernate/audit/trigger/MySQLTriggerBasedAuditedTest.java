package com.vladmihalcea.book.hpjp.hibernate.audit.trigger;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditEntity;
import org.junit.Test;

import javax.persistence.*;
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
    protected void afterInit() {
        ddl("DROP TABLE IF EXISTS post_AUD");
        ddl("""
            CREATE TABLE IF NOT EXISTS post_AUD (
            	id BIGINT NOT NULL,
            	title VARCHAR(255),
            	REV_TYPE ENUM('INSERT', 'UPDATE', 'DELETE') NOT NULL,
            	REV_TIMESTAMP TIMESTAMP NOT NULL,
            	PRIMARY KEY (id, REV_TYPE, REV_TIMESTAMP)
            ) 
            """
        );

        ddl("""          
            CREATE TRIGGER post_insert_audit_trigger
            AFTER INSERT ON post
            FOR EACH ROW BEGIN
            INSERT INTO post_AUD (
                id,
                title,
                REV_TYPE,
                REV_TIMESTAMP
            )
            VALUES(
                NEW.id,
                NEW.title,
                'INSERT',
                CURRENT_TIMESTAMP
            );
            END
            """
        );

        ddl("""
            CREATE TRIGGER post_update_audit_trigger
            AFTER UPDATE ON post
            FOR EACH ROW BEGIN
            INSERT INTO post_AUD (
                id,
                title,
                REV_TYPE,
                REV_TIMESTAMP
            )
            VALUES(
                NEW.id,
                NEW.title,
                'UPDATE',
                CURRENT_TIMESTAMP
            );
            END
            """
        );

        ddl("""
            CREATE TRIGGER post_delete_audit_trigger
            AFTER DELETE ON post
            FOR EACH ROW BEGIN
            INSERT INTO post_AUD (
                id,
                title,
                REV_TYPE,
                REV_TIMESTAMP
            )
            VALUES(
                OLD.id,
                OLD.title,
                'DELETE',
                CURRENT_TIMESTAMP
            );
            END
            """
        );
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
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
            Post post = entityManager.find(Post.class, 1L);
            post.setTitle("High-Performance Java Persistence 2nd edition");
        });

        doInJPA(entityManager -> {
            List<Tuple> revisions = getPostRevisions(entityManager);

            assertEquals(2, revisions.size());
        });

        doInJPA(entityManager -> {
            entityManager.remove(
                entityManager.getReference(Post.class, 1L)
            );
        });

        doInJPA(entityManager -> {
            List<Tuple> revisions = getPostRevisions(entityManager);

            assertEquals(3, revisions.size());
        });
    }

    private List<Tuple> getPostRevisions(EntityManager entityManager) {
        return entityManager.createNativeQuery("""
            SELECT *
            FROM post_AUD 
            """, Tuple.class)
        .getResultList();
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
