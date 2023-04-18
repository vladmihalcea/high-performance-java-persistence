IF EXISTS (SELECT * FROM sysobjects WHERE name='post_tag' and xtype='U')
DROP TABLE post_tag;

IF EXISTS (SELECT * FROM sysobjects WHERE name='tag' and xtype='U')
DROP TABLE tag;

IF EXISTS (SELECT * FROM sysobjects WHERE name='post_details' and xtype='U')
DROP TABLE post_details;

IF EXISTS (SELECT * FROM sysobjects WHERE name='post_comment' and xtype='U')
DROP TABLE post_comment;

IF EXISTS (SELECT * FROM sysobjects WHERE name='post' and xtype='U')
DROP TABLE post;

CREATE TABLE post (id bigint not null, title varchar(255), primary key (id));
CREATE TABLE post_comment (id bigint not null, review varchar(255), post_id bigint, primary key (id));
CREATE TABLE post_details (id bigint not null, created_by varchar(255), created_on datetime2, updated_by varchar(255), updated_on datetime2, primary key (id));
CREATE TABLE tag (id bigint not null, name varchar(255), primary key (id));
CREATE TABLE post_tag (post_id bigint not null, tag_id bigint not null);

ALTER TABLE post_comment ADD CONSTRAINT post_comment_post_id FOREIGN KEY (post_id) REFERENCES post;
ALTER TABLE post_details ADD CONSTRAINT post_details_id FOREIGN KEY (id) REFERENCES post;
ALTER TABLE post_tag ADD CONSTRAINT post_tag_tag_id FOREIGN KEY (tag_id) REFERENCES Tag;
ALTER TABLE post_tag ADD CONSTRAINT post_tag_post_id FOREIGN KEY (post_id) REFERENCES post;

IF EXISTS (SELECT * FROM sysobjects WHERE name='hibernate_sequence' and xtype='SO')
DROP SEQUENCE hibernate_sequence;

CREATE SEQUENCE hibernate_sequence START WITH 1 INCREMENT BY 1;

IF EXISTS (SELECT * FROM sysobjects WHERE name='post_audit_log' and xtype='U')
DROP TABLE post_audit_log;

IF EXISTS (SELECT * FROM sysobjects WHERE name='post_details_audit_log' and xtype='U')
DROP TABLE post_details_audit_log;

IF EXISTS (SELECT * FROM sysobjects WHERE name='post_comment_audit_log' and xtype='U')
DROP TABLE post_comment_audit_log;

IF EXISTS (SELECT * FROM sysobjects WHERE name='post_tag_audit_log' and xtype='U')
DROP TABLE post_tag_audit_log;

IF EXISTS (SELECT * FROM sysobjects WHERE name='tag_audit_log' and xtype='U')
DROP TABLE tag_audit_log;

CREATE TABLE post_audit_log (
    id bigint NOT NULL,
    old_row_data nvarchar(1000) CHECK(ISJSON(old_row_data) = 1),
    new_row_data nvarchar(1000) CHECK(ISJSON(new_row_data) = 1),
    dml_type varchar(10) NOT NULL CHECK (dml_type IN ('INSERT', 'UPDATE', 'DELETE')),
    dml_timestamp datetime NOT NULL,
    dml_created_by varchar(255) NOT NULL,
    trx_timestamp datetime NOT NULL,
    PRIMARY KEY (id, dml_type, dml_timestamp)
);

CREATE TABLE post_details_audit_log (
    id bigint NOT NULL,
    old_row_data nvarchar(1000) CHECK(ISJSON(old_row_data) = 1),
    new_row_data nvarchar(1000) CHECK(ISJSON(new_row_data) = 1),
    dml_type varchar(10) NOT NULL CHECK (dml_type IN ('INSERT', 'UPDATE', 'DELETE')),
    dml_timestamp datetime NOT NULL,
    dml_created_by varchar(255) NOT NULL,
    trx_timestamp datetime NOT NULL,
    PRIMARY KEY (id, dml_type, dml_timestamp)
);

CREATE TABLE post_comment_audit_log (
    id bigint NOT NULL,
    old_row_data nvarchar(1000) CHECK(ISJSON(old_row_data) = 1),
    new_row_data nvarchar(1000) CHECK(ISJSON(new_row_data) = 1),
    dml_type varchar(10) NOT NULL CHECK (dml_type IN ('INSERT', 'UPDATE', 'DELETE')),
    dml_timestamp datetime NOT NULL,
    dml_created_by varchar(255) NOT NULL,
    trx_timestamp datetime NOT NULL,
    PRIMARY KEY (id, dml_type, dml_timestamp)
);

CREATE TABLE post_tag_audit_log (
   id bigint NOT NULL,
   old_row_data nvarchar(1000) CHECK(ISJSON(old_row_data) = 1),
   new_row_data nvarchar(1000) CHECK(ISJSON(new_row_data) = 1),
   dml_type varchar(10) NOT NULL CHECK (dml_type IN ('INSERT', 'UPDATE', 'DELETE')),
   dml_timestamp datetime NOT NULL,
   dml_created_by varchar(255) NOT NULL,
   trx_timestamp datetime NOT NULL,
   PRIMARY KEY (id, dml_type, dml_timestamp)
);

CREATE TABLE tag_audit_log (
    id bigint NOT NULL,
    old_row_data nvarchar(1000) CHECK(ISJSON(old_row_data) = 1),
    new_row_data nvarchar(1000) CHECK(ISJSON(new_row_data) = 1),
    dml_type varchar(10) NOT NULL CHECK (dml_type IN ('INSERT', 'UPDATE', 'DELETE')),
    dml_timestamp datetime NOT NULL,
    dml_created_by varchar(255) NOT NULL,
    trx_timestamp datetime NOT NULL,
    PRIMARY KEY (id, dml_type, dml_timestamp)
);

DROP PROCEDURE IF EXISTS clean_up_audit_log_table;

CREATE PROCEDURE clean_up_audit_log_table(
    @table_name NVARCHAR(100),
    @before_start_timestamp DATETIME,
    @batch_size INT,
    @deleted_row_count INT OUTPUT
)
AS
BEGIN                         
    DROP TABLE IF EXISTS #AUDIT_LOG_ROW_ID_TABLE
    CREATE TABLE #AUDIT_LOG_ROW_ID_TABLE (
        id bigint, 
        dml_type varchar(10), 
        dml_timestamp datetime
    )
    DECLARE
        @audit_log_table_name nvarchar(1000),
        @insert_audit_logs_sql nvarchar(1000)

    SET @audit_log_table_name = @table_name + N'_audit_log '

    SET @insert_audit_logs_sql =
        N'INSERT INTO #AUDIT_LOG_ROW_ID_TABLE ' +
        N'SELECT TOP (@batch_size) id, dml_type, dml_timestamp ' +
        N'FROM ' + @audit_log_table_name +
        N' WHERE dml_timestamp <= @before_start_timestamp'

    EXECUTE sp_executesql @insert_audit_logs_sql,
        N'@batch_size INT, @before_start_timestamp DATETIME',
        @batch_size=@batch_size, @before_start_timestamp=@before_start_timestamp

    SET @deleted_row_count=0
    DECLARE @DeletedBatchRowCount INT

    WHILE (SELECT COUNT(*) FROM #AUDIT_LOG_ROW_ID_TABLE) > 0
    BEGIN       
        SET @DeletedBatchRowCount=0

        BEGIN TRY
            BEGIN TRANSACTION

            DECLARE @delete_audit_logs_sql nvarchar(1000)
            SET @delete_audit_logs_sql =
                N'DELETE FROM ' + @audit_log_table_name +
                N'WHERE EXISTS ( ' +
                N'  SELECT 1 ' +
                N'  FROM #AUDIT_LOG_ROW_ID_TABLE ' +
                N'  WHERE ' +
                N'    ' + @audit_log_table_name + N'.id = #AUDIT_LOG_ROW_ID_TABLE.id AND ' +
                N'    ' + @audit_log_table_name + N'.dml_type = #AUDIT_LOG_ROW_ID_TABLE.dml_type AND ' +
                N'    ' + @audit_log_table_name + N'.dml_timestamp = #AUDIT_LOG_ROW_ID_TABLE.dml_timestamp ' +
                N')'

            EXECUTE sp_executesql @delete_audit_logs_sql
            
            SET @DeletedBatchRowCount+=@@ROWCOUNT
                                                       
            COMMIT TRANSACTION
            SET @deleted_row_count+=@DeletedBatchRowCount
        END TRY
        BEGIN CATCH
            IF (XACT_STATE()) = -1
                -- The current transaction cannot be committed.
                BEGIN
                    PRINT
                        N'The transaction cannot be committed. Rolling back transaction.'
                    ROLLBACK TRANSACTION
                END
            ELSE
                IF (XACT_STATE()) = 1
                -- The current transaction can be committed.
                    BEGIN
                        PRINT
                            N'Exception was caught, but the transaction can be committed.'
                        COMMIT TRANSACTION
                    END
        END CATCH
                   
        TRUNCATE TABLE #AUDIT_LOG_ROW_ID_TABLE

        EXECUTE sp_executesql @insert_audit_logs_sql,
            N'@batch_size INT, @before_start_timestamp DATETIME',
            @batch_size=@batch_size, @before_start_timestamp=@before_start_timestamp
    END
    
    DROP TABLE IF EXISTS #AUDIT_LOG_ROW_ID_TABLE
END;

DROP PROCEDURE IF EXISTS clean_up_audit_log_tables;

CREATE PROCEDURE clean_up_audit_log_tables(
    @before_start_timestamp DATETIME,
    @json_report nvarchar(4000) output
) AS
BEGIN
    DECLARE
        @table_name NVARCHAR(100),
        @batch_size int,
		@deleted_row_count int

    DECLARE @CLEAN_UP_REPORT TABLE (
        id INT,
        table_name NVARCHAR(100),
        deleted_row_count INT DEFAULT 0
    )
    INSERT @CLEAN_UP_REPORT(id, table_name)
    VALUES (1, 'post'),
           (2, 'post_details'),
           (3, 'post_comment'),
           (4, 'tag')

    DECLARE @AUDIT_LOG_TABLE_COUNT INT = (SELECT COUNT(*) FROM @CLEAN_UP_REPORT)
    DECLARE @I INT = 0

    SET @batch_size = 500

    WHILE @I < @AUDIT_LOG_TABLE_COUNT BEGIN
        SELECT @table_name=[table_name]
        FROM @CLEAN_UP_REPORT
        ORDER BY id DESC
        OFFSET @I
        ROWS FETCH NEXT 1 ROWS ONLY

        EXEC clean_up_audit_log_table
             @table_name = @table_name,
             @before_start_timestamp = @before_start_timestamp,
             @batch_size = @batch_size,
             @deleted_row_count = @deleted_row_count OUTPUT

        UPDATE @CLEAN_UP_REPORT
        SET deleted_row_count=@deleted_row_count
        WHERE table_name=@table_name

        SET @I += 1
    END
    SET @json_report = (
        SELECT
            table_name,
            deleted_row_count
        FROM @CLEAN_UP_REPORT
        FOR JSON PATH
    )
END;