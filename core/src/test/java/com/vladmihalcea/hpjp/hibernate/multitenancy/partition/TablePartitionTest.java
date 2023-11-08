package com.vladmihalcea.hpjp.hibernate.multitenancy.partition;

import com.vladmihalcea.hpjp.hibernate.multitenancy.partition.model.Post;
import com.vladmihalcea.hpjp.hibernate.multitenancy.partition.model.User;
import com.vladmihalcea.hpjp.util.AbstractTest;
import org.hibernate.Session;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class TablePartitionTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            User.class,
            Post.class
        };
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
