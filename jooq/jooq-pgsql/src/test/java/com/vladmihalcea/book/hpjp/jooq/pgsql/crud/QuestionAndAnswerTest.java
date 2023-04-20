package com.vladmihalcea.book.hpjp.jooq.pgsql.crud;

import com.vladmihalcea.book.hpjp.jooq.pgsql.schema.crud.routines.GetUpdatedQuestionsAndAnswers;
import com.vladmihalcea.book.hpjp.jooq.pgsql.schema.crud.tables.records.AnswerRecord;
import com.vladmihalcea.book.hpjp.jooq.pgsql.schema.crud.tables.records.QuestionRecord;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.Test;

import java.time.LocalDateTime;

import static com.vladmihalcea.book.hpjp.jooq.pgsql.schema.crud.Tables.ANSWER;
import static com.vladmihalcea.book.hpjp.jooq.pgsql.schema.crud.Tables.QUESTION;
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
            GetUpdatedQuestionsAndAnswers getUpdatedQuestionsAndAnswers = new GetUpdatedQuestionsAndAnswers();
            getUpdatedQuestionsAndAnswers.setUpdatedAfter(LocalDateTime.now().minusDays(1));
            getUpdatedQuestionsAndAnswers.execute(sql.configuration());

            Result<Record> records = getUpdatedQuestionsAndAnswers.getReturnValue();
            assertSame(2, records.size());
            for (Record record : records) {
                QuestionRecord question = record.into(QUESTION);
                AnswerRecord answerRecord = record.into(ANSWER);
            }
        });
    }
}
