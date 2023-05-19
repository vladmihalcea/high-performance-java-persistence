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

import static com.vladmihalcea.book.hpjp.jooq.pgsql.schema.crud.Tables.ANSWER;
import static com.vladmihalcea.book.hpjp.jooq.pgsql.schema.crud.Tables.QUESTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

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
            sql
            .insertInto(QUESTION)
            .columns(
                QUESTION.ID,
                QUESTION.TITLE,
                QUESTION.BODY,
                QUESTION.SCORE
            )
            .values(
                1L,
                "How to call jOOQ stored procedures?",
                "I have a PostgreSQL stored procedure and I'd like to call it from jOOQ.",
                1
            )
            .execute();

            sql
                .insertInto(ANSWER)
                .columns(
                    ANSWER.ID,
                    ANSWER.QUESTION_ID,
                    ANSWER.BODY,
                    ANSWER.SCORE,
                    ANSWER.ACCEPTED
                )
                .values(
                    1L,
                    1L,
                    "Checkout the [jOOQ docs](https://www.jooq.org/doc/latest/manual/sql-execution/stored-procedures/).",
                    10,
                    true
                )
                .values(
                    2L,
                    1L,
                    "Checkout [this article](https://vladmihalcea.com/jooq-facts-sql-functions-made-easy/).",
                    5,
                    false
                )
                .execute();
        });
    }

    @Test
    public void test() {
        doInJOOQ(sql -> {
            Result<GetUpdatedQuestionsAndAnswersRecord> records = sql
                .selectFrom(GetUpdatedQuestionsAndAnswers.GET_UPDATED_QUESTIONS_AND_ANSWERS.call(LocalDateTime.now().minusDays(1)))
                .fetch();

            assertSame(2, records.size());

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

            assertEquals(1, questionsMap.size());
            Question question = questionsMap.get(1L);
            assertEquals(2, question.answers().size());
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
