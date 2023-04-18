package com.vladmihalcea.book.hpjp.jooq.mssql.crud;

import com.vladmihalcea.book.hpjp.jooq.mssql.schema.crud.high_performance_java_persistence.dbo.routines.CleanUpAuditLogTable;
import org.jooq.DSLContext;
import org.junit.Test;

import java.time.LocalDateTime;

import static com.vladmihalcea.book.hpjp.jooq.mssql.schema.crud.high_performance_java_persistence.dbo.Tables.*;
import static org.junit.Assert.assertEquals;

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
        executeStatement("""
            IF EXISTS (SELECT * FROM sys.triggers WHERE name='tr_insert_post_audit_log' and type = 'TR')
            DROP TRIGGER tr_insert_post_audit_log;
            """
        );

        executeStatement("""
            IF EXISTS (SELECT * FROM sys.triggers WHERE name='tr_update_post_audit_log' and type = 'TR')
            DROP TRIGGER tr_update_post_audit_log;
            """
        );

        executeStatement("""
            IF EXISTS (SELECT * FROM sys.triggers WHERE name='tr_delete_post_audit_log' and type = 'TR')
            DROP TRIGGER tr_delete_post_audit_log;
            """
        );

        executeStatement("""
            CREATE TRIGGER TR_Post_Insert_AuditLog ON post FOR INSERT AS
            BEGIN
            	DECLARE @loggedUser varchar(255)
            	SELECT @loggedUser = cast(SESSION_CONTEXT(N'loggedUser') as varchar(255))
            	
            	DECLARE @transactionTimestamp datetime = SYSUTCDATETIME()
            	
            	INSERT INTO post_audit_log (
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
            """
        );

        executeStatement("""
            CREATE TRIGGER TR_Post_Update_AuditLog ON post FOR UPDATE AS
            BEGIN
                DECLARE @loggedUser varchar(255)
                SELECT @loggedUser = cast(SESSION_CONTEXT(N'loggedUser') as varchar(255))
                
                DECLARE @transactionTimestamp datetime = SYSUTCDATETIME()
                
                DECLARE @oldRecord nvarchar(1000)
                DECLARE @newRecord nvarchar(1000)
                
                SET @oldRecord = (SELECT * FROM Deleted FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)
                SET @newRecord = (SELECT * FROM Inserted FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)
                
                IF @oldRecord != @newRecord
                    INSERT INTO post_audit_log (
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
            """);

        executeStatement("""
        CREATE TRIGGER TR_Post_Delete_AuditLog ON post FOR DELETE AS
            BEGIN
                DECLARE @loggedUser varchar(255)
                SELECT @loggedUser = cast(SESSION_CONTEXT(N'loggedUser') as varchar(255))
                
                DECLARE @transactionTimestamp datetime = SYSUTCDATETIME()
                
                INSERT INTO post_audit_log (
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
            """);
    }

    @Test
    public void testAuditLog() {
        LoggedUser.logIn("Vlad Mihalcea");
        
        LocalDateTime now = LocalDateTime.now();
        int postCount = 1000;
        int postCommentCountPerPost = 10;

        doInJOOQ(sql -> {
            setCurrentLoggedUser(sql);
            
            long postCommentId = 1;

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
            }
        });

        doInJOOQ(sql -> {
            assertEquals(postCount, sql.fetchCount(POST_AUDIT_LOG));

            CleanUpAuditLogTable cleanUpPostAuditLog = new CleanUpAuditLogTable();
            cleanUpPostAuditLog.setTableName(POST.getName());
            cleanUpPostAuditLog.setBatchSize(10);
            cleanUpPostAuditLog.setBeforeStartTimestamp(LocalDateTime.now());
            cleanUpPostAuditLog.execute(sql.configuration());
            int deletedRowCount = cleanUpPostAuditLog.getDeletedRowCount();
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
