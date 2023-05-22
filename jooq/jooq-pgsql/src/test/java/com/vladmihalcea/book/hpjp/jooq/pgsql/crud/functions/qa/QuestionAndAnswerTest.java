package com.vladmihalcea.book.hpjp.jooq.pgsql.crud.functions.qa;

import com.vladmihalcea.book.hpjp.jooq.pgsql.crud.AbstractJOOQPostgreSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.jooq.pgsql.schema.crud.tables.GetUpdatedQuestionsAndAnswers;
import com.vladmihalcea.book.hpjp.jooq.pgsql.schema.crud.tables.records.GetUpdatedQuestionsAndAnswersRecord;
import org.jooq.Result;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.vladmihalcea.book.hpjp.jooq.pgsql.schema.crud.Tables.ANSWER;
import static com.vladmihalcea.book.hpjp.jooq.pgsql.schema.crud.Tables.QUESTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class QuestionAndAnswerTest extends AbstractJOOQPostgreSQLIntegrationTest {

    @Override
    protected String ddlScript() {
        return "clean_schema.sql";
    }

    public void afterInit() {
        doInJOOQ(sql -> {
            LocalDateTime timestamp = LocalDateTime.now().minusSeconds(1);

            sql
            .insertInto(QUESTION)
            .columns(
                QUESTION.ID,
                QUESTION.TITLE,
                QUESTION.BODY,
                QUESTION.SCORE,
                QUESTION.CREATED_ON,
                QUESTION.CREATED_ON
            )
            .values(
                1L,
                "How to call jOOQ stored procedures?",
                "I have a PostgreSQL stored procedure and I'd like to call it from jOOQ.",
                1,
                timestamp,
                timestamp
            )
            .execute();

            sql
                .insertInto(ANSWER)
                .columns(
                    ANSWER.ID,
                    ANSWER.QUESTION_ID,
                    ANSWER.BODY,
                    ANSWER.SCORE,
                    ANSWER.ACCEPTED,
                    ANSWER.CREATED_ON,
                    ANSWER.CREATED_ON
                )
                .values(
                    1L,
                    1L,
                    "Checkout the [jOOQ docs](https://www.jooq.org/doc/latest/manual/sql-execution/stored-procedures/).",
                    10,
                    true,
                    timestamp,
                    timestamp
                )
                .values(
                    2L,
                    1L,
                    "Checkout [this article](https://vladmihalcea.com/jooq-facts-sql-functions-made-easy/).",
                    5,
                    false,
                    timestamp,
                    timestamp
                )
                .execute();
        });
    }

    @Test
    public void test() {
        List<Question> questions = getQuestionsAndAnswersUpdatedAfter();

        assertEquals(1, questions.size());
        Question question = questions.get(0);
        assertEquals(2, question.answers().size());
        
        doInJOOQ(sql -> {
            sql
                .insertInto(ANSWER)
                .columns(
                    ANSWER.ID,
                    ANSWER.QUESTION_ID,
                    ANSWER.BODY
                )
                .values(
                    3L,
                    1L,
                    "Checkout this [video from Toon Koppelaars](https://www.youtube.com/watch?v=8jiJDflpw4Y)."
                )
                .execute();
        });

        questions = getQuestionsAndAnswersUpdatedAfter();

        assertEquals(1, questions.size());
        question = questions.get(0);
        assertEquals(3, question.answers().size());

        sleep(TimeUnit.SECONDS.toMillis(1));

        doInJOOQ(sql -> {
            sql
                .update(ANSWER)
                .set(ANSWER.BODY, "Checkout this [YouTube video from Toon Koppelaars](https://www.youtube.com/watch?v=8jiJDflpw4Y).")
                .where(ANSWER.ID.eq(3L))
                .execute();
        });

        questions = getQuestionsAndAnswersUpdatedAfter();

        assertEquals(1, questions.size());
        question = questions.get(0);
        assertEquals(3, question.answers().size());

        doInJOOQ(sql -> {
            sql
                .insertInto(QUESTION)
                .columns(
                    QUESTION.ID,
                    QUESTION.TITLE,
                    QUESTION.BODY
                )
                .values(
                    2L,
                    "How to use the jOOQ MULTISET operator?",
                    "I want to know how I can use the jOOQ MULTISET operator."
                )
                .execute();
        });

        questions = getQuestionsAndAnswersUpdatedAfter();

        assertEquals(1, questions.size());
        question = questions.get(0);
        assertEquals(2L, question.id.longValue());
        assertTrue(question.answers().isEmpty());
    }

    private List<Question> getQuestionsAndAnswersUpdatedAfter() {
        return doInJOOQ(sql -> {
            Result<GetUpdatedQuestionsAndAnswersRecord> records = sql
                .selectFrom(
                    GetUpdatedQuestionsAndAnswers.GET_UPDATED_QUESTIONS_AND_ANSWERS.call()
                )
                .fetch();

            Map<Long, Question> questionsMap = new LinkedHashMap<>();

            for (GetUpdatedQuestionsAndAnswersRecord record : records) {
                Long questionId = record.getQuestionId();

                Question question = questionsMap.computeIfAbsent(
                    questionId,
                    id -> new Question(
                        questionId,
                        record.getQuestionTitle(),
                        record.getQuestionBody(),
                        record.getQuestionScore(),
                        record.getQuestionCreatedOn(),
                        record.getQuestionUpdatedOn(),
                        new ArrayList<>()
                    )
                );
                Long answerId = record.getAnswerId();
                if (answerId != null) {
                    question.answers().add(
                        new Answer(
                            answerId,
                            record.getAnswerBody(),
                            record.getAnswerScore(),
                            record.getAnswerAccepted(),
                            record.getAnswerCreatedOn(),
                            record.getAnswerUpdatedOn()
                        )
                    );
                }
            }

            return new ArrayList<>(questionsMap.values());
        });
    }

    public static record Question(
        Long id,
        String title,
        String body,
        int score,
        LocalDateTime createdOn,
        LocalDateTime updateOn,
        List<Answer> answers) {
    }

    public static record Answer(
        Long id,
        String body,
        int score,
        boolean accepted,
        LocalDateTime createdOn,
        LocalDateTime updateOn) {
    }
}
