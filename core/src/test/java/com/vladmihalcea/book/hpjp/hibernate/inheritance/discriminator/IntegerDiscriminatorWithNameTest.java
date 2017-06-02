package com.vladmihalcea.book.hpjp.hibernate.inheritance.discriminator;

import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.DiscriminatorValue;
import javax.persistence.EntityManager;

import org.hibernate.Session;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;

/**
 * @author Vlad Mihalcea
 */
public class IntegerDiscriminatorWithNameTest
    extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Board.class,
            TopicType.class,
            Topic.class,
            Post.class,
            Announcement.class,
            TopicStatistics.class
        };
    }

    @Test
    public void test() {
        Topic _topic = doInJPA(entityManager -> {

            addConsistecyTriggers( entityManager );

            for ( Class entityClass : entities() ) {
                if ( Topic.class.isAssignableFrom( entityClass ) && !Topic.class.equals( entityClass ) ) {
                    DiscriminatorValue discriminatorValue = (DiscriminatorValue) entityClass.getAnnotation( DiscriminatorValue.class );
                    Byte id = Byte.valueOf( discriminatorValue.value() );

                    TopicType topicType = new TopicType();
                    topicType.setId( id );
                    topicType.setName( entityClass.getName() );
                    topicType.setDescription( String.format( "%s is a subclass of the Topic base class", entityClass.getSimpleName()) );

                    entityManager.persist( topicType );
                }
            }

            Board board = new Board();
            board.setName("Hibernate");

            entityManager.persist(board);

            Post post = new Post();
            post.setOwner("John Doe");
            post.setTitle("Inheritance");
            post.setContent("Best practices");
            post.setBoard(board);

            entityManager.persist(post);

            Announcement announcement = new Announcement();
            announcement.setOwner("John Doe");
            announcement.setTitle("Release x.y.z.Final");
            announcement.setValidUntil(Timestamp.valueOf(LocalDateTime.now().plusMonths(1)));
            announcement.setBoard(board);

            entityManager.persist(announcement);

            TopicStatistics postStatistics = new TopicStatistics(post);
            postStatistics.incrementViews();
            entityManager.persist(postStatistics);

            TopicStatistics announcementStatistics = new TopicStatistics(announcement);
            announcementStatistics.incrementViews();
            entityManager.persist(announcementStatistics);

            return post;
        });

        doInJPA(entityManager -> {
            Board board = _topic.getBoard();
            LOGGER.info("Fetch Topics");
            List<Topic> topics = entityManager
                    .createQuery("select t from Topic t where t.board = :board", Topic.class)
                    .setParameter("board", board)
                    .getResultList();

            for ( Topic topic: topics ) {
                LOGGER.info( "Found topic: {}", topic.getType() );
            }
        });
    }

    private void addConsistecyTriggers(EntityManager entityManager) {
        entityManager.unwrap(Session.class).doWork( connection -> {
            try(Statement st = connection.createStatement()) {
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
