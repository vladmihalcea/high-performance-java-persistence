package com.vladmihalcea.hpjp.hibernate.sp;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import jakarta.persistence.*;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLStoredProcedureQATest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Question.class,
            Answer.class
        };
    }

    public void afterInit() {
        doInJDBC(connection -> {
            try(Statement statement = connection.createStatement()) {
                statement.executeUpdate("DROP FUNCTION get_updated_questions_and_answers(timestamp)");
            }
            catch (SQLException ignore) {
            }
        });
        doInJDBC(connection -> {
            try(Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                    CREATE OR REPLACE FUNCTION get_updated_questions_and_answers(updated_after timestamp)
                       RETURNS REFCURSOR AS
                    $BODY$
                        DECLARE
                            qa REFCURSOR;
                        BEGIN
                            OPEN qa FOR 
                                SELECT * 
                                FROM question 
                                JOIN answer on question.id = answer.question_id
                                WHERE 
                                    question.updated_on >= updated_after OR 
                                    answer.updated_on >= updated_after
                                ; 
                            RETURN qa;
                        END;
                    $BODY$
                    LANGUAGE plpgsql
                    """
                );
            }
        });
        doInJPA(entityManager -> {
            Question question = new Question()
                .setId(1L)
                .setTitle("How to call jOOQ stored procedures?")
                .setBody("I have a PostgreSQL stored procedure and I'd like to call it from jOOQ.")
                .setScore(1);

            entityManager.persist(question);

            entityManager.persist(
                new Answer()
                    .setQuestion(question)
                    .setBody("""
                        Checkout the 
                        [jOOQ docs](https://www.jooq.org/doc/latest/manual/sql-execution/stored-procedures/).
                        """)
                    .setScore(10)
                    .setAccepted(true)
            );

            entityManager.persist(
                new Answer()
                    .setQuestion(question)
                    .setBody("""
                        Checkout 
                        [this article](https://vladmihalcea.com/jooq-facts-sql-functions-made-easy/).
                        """)
                    .setScore(5)
            );
        });
    }

    @Test
    public void testStoredProcedureRefCursor() {
        doInJPA(entityManager -> {
            StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("get_updated_questions_and_answers")
                .registerStoredProcedureParameter(1, void.class, ParameterMode.REF_CURSOR)
                .registerStoredProcedureParameter(2, LocalDateTime.class, ParameterMode.IN)
                .setParameter(2, LocalDateTime.now().minusDays(1));

            List<Object[]> qas = query.getResultList();
            assertEquals(2, qas.size());
        });
    }

    @Entity(name = "Question")
    @Table(name = "question")
    public static class Question {

        @Id
        private Long id;

        private String title;

        private String body;

        @Column(name = "created_on")
        private LocalDateTime createdOn = LocalDateTime.now();

        @Column(name = "updated_on")
        private LocalDateTime updatedOn = LocalDateTime.now();

        private int score;

        public Long getId() {
            return id;
        }

        public Question setId(Long id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Question setTitle(String title) {
            this.title = title;
            return this;
        }

        public String getBody() {
            return body;
        }

        public Question setBody(String body) {
            this.body = body;
            return this;
        }

        public LocalDateTime getCreatedOn() {
            return createdOn;
        }

        public Question setCreatedOn(LocalDateTime createdOn) {
            this.createdOn = createdOn;
            return this;
        }

        public LocalDateTime getUpdatedOn() {
            return updatedOn;
        }

        public Question setUpdatedOn(LocalDateTime updatedOn) {
            this.updatedOn = updatedOn;
            return this;
        }

        public int getScore() {
            return score;
        }

        public Question setScore(int score) {
            this.score = score;
            return this;
        }
    }

    @Entity(name = "Answer")
    @Table(name = "answer")
    public static class Answer {

        @Id
        @GeneratedValue
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Question question;

        private String body;

        @Column(name = "created_on")
        private LocalDateTime createdOn = LocalDateTime.now();

        @Column(name = "updated_on")
        private LocalDateTime updatedOn = LocalDateTime.now();

        private int score;

        private boolean accepted;

        public Long getId() {
            return id;
        }

        public Answer setId(Long id) {
            this.id = id;
            return this;
        }

        public Question getQuestion() {
            return question;
        }

        public Answer setQuestion(Question question) {
            this.question = question;
            return this;
        }

        public String getBody() {
            return body;
        }

        public Answer setBody(String body) {
            this.body = body;
            return this;
        }

        public LocalDateTime getCreatedOn() {
            return createdOn;
        }

        public Answer setCreatedOn(LocalDateTime createdOn) {
            this.createdOn = createdOn;
            return this;
        }

        public LocalDateTime getUpdatedOn() {
            return updatedOn;
        }

        public Answer setUpdatedOn(LocalDateTime updatedOn) {
            this.updatedOn = updatedOn;
            return this;
        }

        public int getScore() {
            return score;
        }

        public Answer setScore(int score) {
            this.score = score;
            return this;
        }

        public boolean isAccepted() {
            return accepted;
        }

        public Answer setAccepted(boolean accepted) {
            this.accepted = accepted;
            return this;
        }
    }
}
