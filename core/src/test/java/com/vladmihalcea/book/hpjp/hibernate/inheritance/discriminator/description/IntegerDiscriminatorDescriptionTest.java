package com.vladmihalcea.book.hpjp.hibernate.inheritance.discriminator.description;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import org.hibernate.Session;
import org.junit.Test;

import javax.persistence.DiscriminatorValue;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class IntegerDiscriminatorDescriptionTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Topic.class,
            Post.class,
            Announcement.class,
            TopicType.class,
        };
    }

    @Override
    protected void afterInit() {
        doInJPA(this::addConsistencyTriggers);

        doInJPA(entityManager -> {
            for (Class entityClass : entities()) {
                if (Topic.class.isAssignableFrom(entityClass)) {

                    DiscriminatorValue discriminatorValue = (DiscriminatorValue)
                        entityClass.getAnnotation(DiscriminatorValue.class);

                    TopicType topicType = new TopicType();
                    topicType.setId(Byte.valueOf(discriminatorValue.value()));
                    topicType.setName(entityClass.getName());
                    topicType.setDescription(
                        Topic.class.equals(entityClass) ?
                            "Topic is the base class of the Topic entity hierarchy" :
                            String.format(
                                "%s is a subclass of the Topic base class",
                                entityClass.getSimpleName()
                            )
                    );

                    entityManager.persist(topicType);
                }
            }
        });
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setOwner("John Doe");
            post.setTitle("Inheritance");
            post.setContent("Best practices");

            entityManager.persist(post);

            Announcement announcement = new Announcement();
            announcement.setOwner("John Doe");
            announcement.setTitle("Release x.y.z.Final");
            announcement.setValidUntil(Timestamp.valueOf(LocalDateTime.now().plusMonths(1)));

            entityManager.persist(announcement);
        });

        doInJPA(entityManager -> {
            List<Post> posts = entityManager
            .createQuery(
                "select p " +
                "from Post p ", Post.class)
            .getResultList();

            assertEquals(1, posts.size());

            List<Tuple> results = entityManager
            .createNativeQuery(
                "SELECT " +
                "    t.*, " +
                "    CAST(tt.id AS SIGNED) AS \"discriminator\", " +
                "    tt.name AS \"type_name\", " +
                "    tt.description AS \"type_description\" " +
                "FROM topic t " +
                "INNER JOIN topic_type tt ON t.topic_type_id = tt.id " +
                "WHERE t.content IS NOT NULL", Tuple.class)
            .getResultList();

            assertEquals(1, results.size());

            Tuple postTuple = results.get(0);

            assertEquals(
                "Best practices",
                postTuple.get("content")
            );
            assertEquals(
                1,
                ((Number) postTuple.get("discriminator")).intValue()
            );
            assertEquals(
                Post.class.getName(),
                postTuple.get("type_name")
            );
            assertEquals(
                "Post is a subclass of the Topic base class",
                postTuple.get("type_description")
            );
        });
    }

    private void addConsistencyTriggers(EntityManager entityManager) {
        entityManager.unwrap(Session.class).doWork(connection -> {
            try (Statement st = connection.createStatement()) {
                st.executeUpdate(
                    "CREATE " +
                    "TRIGGER post_content_check BEFORE INSERT " +
                    "ON Topic " +
                    "FOR EACH ROW " +
                    "BEGIN " +
                    "   IF NEW.topic_type_id = 1 " +
                    "   THEN " +
                    "       IF NEW.content IS NULL " +
                    "       THEN " +
                    "           signal sqlstate '45000' " +
                    "           set message_text = 'Post content cannot be NULL'; " +
                    "       END IF; " +
                    "   END IF; " +
                    "END;"
                );
                st.executeUpdate(
                    "CREATE " +
                    "TRIGGER announcement_validUntil_check BEFORE INSERT " +
                    "ON Topic " +
                    "FOR EACH ROW " +
                    "BEGIN " +
                    "   IF NEW.topic_type_id = 2 " +
                    "   THEN " +
                    "       IF NEW.validUntil IS NULL " +
                    "       THEN " +
                    "           signal sqlstate '45000' " +
                    "           set message_text = 'Announcement validUntil cannot be NULL'; " +
                    "       END IF; " +
                    "   END IF; " +
                    "END;"
                );
            }
        });
    }

}
