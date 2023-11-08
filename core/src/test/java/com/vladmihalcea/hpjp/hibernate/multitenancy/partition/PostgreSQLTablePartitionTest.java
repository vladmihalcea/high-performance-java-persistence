package com.vladmihalcea.hpjp.hibernate.multitenancy.partition;

import com.vladmihalcea.hpjp.hibernate.multitenancy.partition.model.Post;
import com.vladmihalcea.hpjp.hibernate.multitenancy.partition.model.User;
import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLTablePartitionTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            User.class,
            Post.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.setProperty(AvailableSettings.HBM2DDL_AUTO, "none");
    }

    @Override
    protected void beforeInit() {
        executeStatement("DROP TABLE IF EXISTS posts cascade");
        executeStatement("DROP TABLE IF EXISTS users cascade");
        executeStatement("DROP SEQUENCE IF EXISTS posts_SEQ");
        executeStatement("DROP SEQUENCE IF EXISTS users_SEQ");

        executeStatement("CREATE SEQUENCE posts_SEQ START WITH 1 INCREMENT BY 50");
        executeStatement("CREATE SEQUENCE users_SEQ START WITH 1 INCREMENT BY 50");

        executeStatement("""
            CREATE TABLE users (
                id bigint NOT NULL,
                first_name varchar(255),
                last_name varchar(255),
                registered_on timestamp(6),
                partition_key varchar(255),
                PRIMARY KEY (id, partition_key)
            ) PARTITION BY LIST (partition_key)
            """);
        executeStatement("CREATE TABLE users_asia PARTITION OF users FOR VALUES IN ('Asia')");
        executeStatement("CREATE TABLE users_africa PARTITION OF users FOR VALUES IN ('Africa')");
        executeStatement("CREATE TABLE users_north_america PARTITION OF users FOR VALUES IN ('North America')");
        executeStatement("CREATE TABLE users_south_america PARTITION OF users FOR VALUES IN ('South America')");
        executeStatement("CREATE TABLE users_europe PARTITION OF users FOR VALUES IN ('Europe')");
        executeStatement("CREATE TABLE users_australia PARTITION OF users FOR VALUES IN ('Australia')");

        executeStatement("""
            CREATE TABLE posts (
                id bigint NOT NULL,
                title varchar(255),
                created_on timestamp(6),
                user_id bigint,
                partition_key varchar(255),
                PRIMARY KEY (id, partition_key)
            ) PARTITION BY LIST (partition_key)
            """);
        executeStatement("CREATE TABLE posts_asia PARTITION OF posts FOR VALUES IN ('Asia')");
        executeStatement("CREATE TABLE posts_africa PARTITION OF posts FOR VALUES IN ('Africa')");
        executeStatement("CREATE TABLE posts_north_america PARTITION OF posts FOR VALUES IN ('North America')");
        executeStatement("CREATE TABLE posts_south_america PARTITION OF posts FOR VALUES IN ('South America')");
        executeStatement("CREATE TABLE posts_europe PARTITION OF posts FOR VALUES IN ('Europe')");
        executeStatement("CREATE TABLE posts_australia PARTITION OF posts FOR VALUES IN ('Australia')");

        executeStatement("""
            ALTER TABLE IF EXISTS posts
            ADD CONSTRAINT fk_posts_user_id FOREIGN KEY (user_id, partition_key) REFERENCES users
            """);
    }

    @Test
    public void test() {
        PartitionContext.set("Europe");

        User vlad = doInJPA(entityManager -> {
            User user = new User()
                .setFirstName("Vlad")
                .setLastName("Mihalcea");

            entityManager.persist(user);
            return user;
        });

        PartitionContext.set("North America");

        doInJPA(entityManager -> {
            entityManager.persist(
                new User()
                    .setFirstName("John")
                    .setLastName("Doe")
            );

            entityManager.persist(
                new User()
                    .setFirstName("Jane")
                    .setLastName("Doe")
            );
        });

        PartitionContext.set("Europe");

        Post _post = doInJPA(entityManager -> {
            Post post = new Post()
                .setTitle("High-Performance Java Persistence")
                .setUser(vlad);

            entityManager.persist(post);

            return post;
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, _post.getId());

            entityManager.remove(post);
        });

        PartitionContext.set("North America");

        doInJPA(entityManager -> {
            List<User> allUsers = entityManager.createQuery("""
                select u
                from User u
                order by u.id
                """, User.class)
            .getResultList();

            assertEquals(3, allUsers.size());

            entityManager
                .unwrap(Session.class)
                .enableFilter("partitionKey")
                .setParameter("partitionKey", PartitionContext.get());

            List<User> northAmericanUsers = entityManager.createQuery("""
                select u
                from User u
                order by u.id
                """, User.class)
            .getResultList();

            assertEquals(2, northAmericanUsers.size());
        });
    }
}
