package com.vladmihalcea.book.hpjp.jooq.mssql.crud;

import com.vladmihalcea.book.hpjp.jooq.mssql.schema.crud.high_performance_java_persistence.dbo.routines.CleanUpAuditLogTable;
import com.vladmihalcea.book.hpjp.jooq.mssql.schema.crud.high_performance_java_persistence.dbo.routines.CleanUpAuditLogTables;
import org.jooq.DSLContext;
import org.junit.Test;

import java.time.LocalDateTime;

import static com.vladmihalcea.book.hpjp.jooq.mssql.schema.crud.high_performance_java_persistence.dbo.Tables.*;
import static org.junit.Assert.assertSame;

/**
 * @author Vlad Mihalcea
 */
public class AuditLogTest extends AbstractJOOQSQLServerSQLIntegrationTest {

    @Override
    protected String ddlScript() {
        return "clean_schema.sql";
    }

    @Override
    protected void afterInit() {
        String[] DML_TYPES = new String[] {
            "insert",
            "update",
            "delete"
        };

        String[] TABLES = new String[] {
            "post",
            "post_details",
            "post_comment",
            "tag"
        };

        for (String table : TABLES) {
            for (String dmlType : DML_TYPES) {
                executeStatement(String.format("""
                    IF EXISTS (SELECT * FROM sys.triggers WHERE name='tr_%1$s_%2$s_audit_log' and type = 'TR')
                    DROP TRIGGER tr_%1$s_%2$s_audit_log;
                    """,
                    dmlType,
                    table)
                );
            }
            executeStatement(String.format("""
                CREATE TRIGGER tr_insert_%1$s_audit_log ON %1$s FOR INSERT AS
                BEGIN
                    DECLARE @loggedUser varchar(255)
                    SELECT @loggedUser = cast(SESSION_CONTEXT(N'loggedUser') as varchar(255))
                    
                    DECLARE @transactionTimestamp datetime = SYSUTCDATETIME()
                    
                    INSERT INTO %1$s_audit_log (
                        id,
                        old_row_data,
                        new_row_data,
                        dml_type,
                        dml_timestamp,
                        dml_created_by,
                        trx_timestamp
                    )
                    VALUES(
                        (SELECT id FROM Inserted),
                        null,
                        (SELECT * FROM Inserted FOR JSON PATH, WITHOUT_ARRAY_WRAPPER),
                        'INSERT',
                        CURRENT_TIMESTAMP,
                        @loggedUser,
                        @transactionTimestamp
                    );
                END;
                """,
                table)
            );
            executeStatement(String.format("""
                CREATE TRIGGER tr_update_%1$s_audit_log ON %1$s FOR UPDATE AS
                BEGIN
                    DECLARE @loggedUser varchar(255)
                    SELECT @loggedUser = cast(SESSION_CONTEXT(N'loggedUser') as varchar(255))
                    
                    DECLARE @transactionTimestamp datetime = SYSUTCDATETIME()
                    
                    DECLARE @oldRecord nvarchar(1000)
                    DECLARE @newRecord nvarchar(1000)
                    
                    SET @oldRecord = (SELECT * FROM Deleted FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)
                    SET @newRecord = (SELECT * FROM Inserted FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)
                    
                    IF @oldRecord != @newRecord
                        INSERT INTO %1$s_audit_log (
                            id,
                            old_row_data,
                            new_row_data,
                            dml_type,
                            dml_timestamp,
                            dml_created_by,
                            trx_timestamp
                        )
                        VALUES(
                            (SELECT id FROM Inserted),
                            @oldRecord,
                            @newRecord,
                            'UPDATE',
                            CURRENT_TIMESTAMP,
                            @loggedUser,
                            @transactionTimestamp
                        );
                END;
                """,
                table)
            );

            executeStatement(String.format("""
                CREATE TRIGGER tr_delete_%1$s_audit_log ON %1$s FOR DELETE AS
                    BEGIN
                        DECLARE @loggedUser varchar(255)
                        SELECT @loggedUser = cast(SESSION_CONTEXT(N'loggedUser') as varchar(255))
                        
                        DECLARE @transactionTimestamp datetime = SYSUTCDATETIME()
                        
                        INSERT INTO %1$s_audit_log (
                            id,
                            old_row_data,
                            new_row_data,
                            dml_type,
                            dml_timestamp,
                            dml_created_by,
                            trx_timestamp
                        )
                        VALUES(
                            (SELECT id FROM Deleted),
                            (SELECT * FROM Deleted FOR JSON PATH, WITHOUT_ARRAY_WRAPPER),
                            null,
                            'DELETE',
                            CURRENT_TIMESTAMP,
                            @loggedUser,
                            @transactionTimestamp
                        );
                    END;
                    """,
                table)
            );
        }

        initData();
    }

    private void initData() {
        LoggedUser.logIn("Vlad Mihalcea");

        LocalDateTime now = LocalDateTime.now();
        int tagCount = 10;
        int postCount = 1000;
        int postCommentCountPerPost = 10;

        doInJOOQ(sql -> {
            setCurrentLoggedUser(sql);

            long postCommentId = 1;

            for (long tagId = 1; tagId <= tagCount; tagId++) {
                sql
                    .insertInto(TAG).columns(TAG.ID, TAG.NAME)
                    .values(tagId, String.format("Tag %d", tagId))
                    .execute();
            }

            for (long postId = 1; postId <= postCount; postId++) {
                sql
                    .insertInto(POST).columns(POST.ID, POST.TITLE)
                    .values(postId, String.format("High-Performance Java Persistence - Page %d", postId))
                    .execute();

                sql
                    .insertInto(POST_DETAILS).columns(
                    POST_DETAILS.ID,
                    POST_DETAILS.CREATED_ON,
                    POST_DETAILS.CREATED_BY
                )
                    .values(postId, now.plusHours(postId / 10), LoggedUser.get())
                    .execute();

                for (int j = 1; j <= postCommentCountPerPost; j++) {
                    sql
                        .insertInto(POST_COMMENT).columns(
                        POST_COMMENT.ID,
                        POST_COMMENT.REVIEW,
                        POST_COMMENT.POST_ID
                    )
                        .values(postCommentId++, "Cool", postId)
                        .execute();
                }

                for (long tagId = 1; tagId <= tagCount; tagId++) {
                    sql
                        .insertInto(POST_TAG).columns(
                        POST_TAG.POST_ID,
                        POST_TAG.TAG_ID
                    )
                        .values(postId, tagId)
                        .execute();
                }
            }
        });
    }


    @Test
    public void testCleanUpAuditLogTablePost() {
        LocalDateTime _60DaysAgo = LocalDateTime.now().minusDays(60);

        doInJOOQ(sql -> {
            sql.update(POST_AUDIT_LOG).set(POST_AUDIT_LOG.DML_TIMESTAMP, _60DaysAgo).execute();
        });

        doInJOOQ(sql -> {
            CleanUpAuditLogTable cleanUpPostAuditLog = new CleanUpAuditLogTable();
            cleanUpPostAuditLog.setTableName(POST.getName());
            cleanUpPostAuditLog.setBatchSize(500);
            cleanUpPostAuditLog.setBeforeStartTimestamp(LocalDateTime.now().minusDays(30));
            cleanUpPostAuditLog.execute(sql.configuration());

            int deletedRowCount = cleanUpPostAuditLog.getDeletedRowCount();
            assertSame(1000, deletedRowCount);
        });
    }

    @Test
    public void testCleanUpAuditLogTablePostComment() {
        LocalDateTime _60DaysAgo = LocalDateTime.now().minusDays(60);

        doInJOOQ(sql -> {
            sql.update(POST_COMMENT_AUDIT_LOG).set(POST_COMMENT_AUDIT_LOG.DML_TIMESTAMP, _60DaysAgo).execute();
        });

        doInJOOQ(sql -> {
            CleanUpAuditLogTable cleanUpPostCommentAuditLog = new CleanUpAuditLogTable();
            cleanUpPostCommentAuditLog.setTableName(POST_COMMENT.getName());
            cleanUpPostCommentAuditLog.setBatchSize(500);
            cleanUpPostCommentAuditLog.setBeforeStartTimestamp(LocalDateTime.now().minusDays(30));
            cleanUpPostCommentAuditLog.execute(sql.configuration());

            int deletedRowCount = cleanUpPostCommentAuditLog.getDeletedRowCount();
            assertSame(10_000, deletedRowCount);
        });
    }

    @Test
    public void testCleanUpAuditLogTables() {
        LocalDateTime _60DaysAgo = LocalDateTime.now().minusDays(60);

        doInJOOQ(sql -> {
            sql.update(POST_AUDIT_LOG).set(POST_AUDIT_LOG.DML_TIMESTAMP, _60DaysAgo).execute();
            sql.update(POST_COMMENT_AUDIT_LOG).set(POST_COMMENT_AUDIT_LOG.DML_TIMESTAMP, _60DaysAgo).execute();
            sql.update(POST_DETAILS_AUDIT_LOG).set(POST_DETAILS_AUDIT_LOG.DML_TIMESTAMP, _60DaysAgo).execute();
        });

        doInJOOQ(sql -> {
            CleanUpAuditLogTables cleanUpPostAuditLogTables = new CleanUpAuditLogTables();
            cleanUpPostAuditLogTables.setBeforeStartTimestamp(LocalDateTime.now().minusDays(30));
            cleanUpPostAuditLogTables.execute(sql.configuration());
            String jsonReport = cleanUpPostAuditLogTables.getJsonReport();
            LOGGER.info("Clean-up report: {}", jsonReport);
        });
    }

    private void setCurrentLoggedUser(DSLContext sql) {
        sql.execute(
            String.format(
                "EXEC sys.sp_set_session_context @key = N'loggedUser', @value = N'%s', @read_only = 1", LoggedUser.get()
            )
        );
    }

    public static class LoggedUser {

        private static final ThreadLocal<String> userHolder = new ThreadLocal<>();

        public static void logIn(String user) {
            userHolder.set(user);
        }

        public static void logOut() {
            userHolder.remove();
        }

        public static String get() {
            return userHolder.get();
        }
    }
}
