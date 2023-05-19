package com.vladmihalcea.book.hpjp.jooq.pgsql.crud;

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
        RecentQuestionSnapshot recentQuestionSnapshot = getQuestionsAndAnswersUpdatedAfter(LocalDateTime.now().minusMinutes(1));

        assertEquals(1, recentQuestionSnapshot.list().size());
        Question question = recentQuestionSnapshot.list().get(0);
        assertEquals(2, question.answers().size());

        sleep(TimeUnit.SECONDS.toMillis(1));

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

        recentQuestionSnapshot = getQuestionsAndAnswersUpdatedAfter(recentQuestionSnapshot.timestamp);

        assertEquals(1, recentQuestionSnapshot.list().size());
        question = recentQuestionSnapshot.list().get(0);
        assertEquals(3, question.answers().size());
    }

    private RecentQuestionSnapshot getQuestionsAndAnswersUpdatedAfter(LocalDateTime fromTimestamp) {
        LocalDateTime toTimestamp = LocalDateTime.now();

        List<Question> questions = doInJOOQ(sql -> {
            Result<GetUpdatedQuestionsAndAnswersRecord> records = sql
                .selectFrom(
                    GetUpdatedQuestionsAndAnswers.GET_UPDATED_QUESTIONS_AND_ANSWERS
                        .call(fromTimestamp, toTimestamp)
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
                question.answers().add(
                    new Answer(
                        record.getAnswerId(),
                        record.getAnswerBody(),
                        record.getAnswerScore(),
                        record.getAnswerAccepted(),
                        record.getAnswerCreatedOn(),
                        record.getAnswerUpdatedOn()
                    )
                );
            }

            return new ArrayList<>(questionsMap.values());
        });

        return new RecentQuestionSnapshot(
            toTimestamp,
            questions
        );
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

    public static record RecentQuestionSnapshot(
        LocalDateTime timestamp,
        List<Question> list) {
    }
}
