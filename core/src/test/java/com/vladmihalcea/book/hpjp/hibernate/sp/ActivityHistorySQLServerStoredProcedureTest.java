package com.vladmihalcea.book.hpjp.hibernate.sp;

import com.vladmihalcea.book.hpjp.util.AbstractSQLServerIntegrationTest;
import org.hibernate.Session;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.EntityManager;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class ActivityHistorySQLServerStoredProcedureTest extends AbstractSQLServerIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[]{};
    }

    @Before
    public void init() {
        super.init();
        if (recreateTables) {
            executeStatement("DROP table ACT_HI_PROCINST");
            executeStatement("DROP table ACT_HI_ACTINST");
            executeStatement("DROP table ACT_HI_TASKINST");
            executeStatement("DROP table ACT_GE_BYTEARRAY");
            executeStatement("DROP table ACT_HI_VARINST");
            executeStatement("DROP table ACT_HI_DETAIL");
            executeStatement("DROP table ACT_HI_COMMENT");
            executeStatement("DROP table ACT_HI_ATTACHMENT");
            executeStatement("DROP table ACT_HI_IDENTITYLINK");

            executeStatement("""
                create table ACT_HI_PROCINST (
                    ID_ nvarchar(64) not null,
                    PROC_INST_ID_ nvarchar(64) not null,
                    BUSINESS_KEY_ nvarchar(255),
                    PROC_DEF_ID_ nvarchar(64) not null,
                    START_TIME_ datetime not null,
                    END_TIME_ datetime,
                    DURATION_ numeric(19,0),
                    START_USER_ID_ nvarchar(255),
                    START_ACT_ID_ nvarchar(255),
                    END_ACT_ID_ nvarchar(255),
                    SUPER_PROCESS_INSTANCE_ID_ nvarchar(64),
                    DELETE_REASON_ nvarchar(4000),
                    TENANT_ID_ nvarchar(255) default '',
                    NAME_ nvarchar(255),
                    primary key (ID_),
                    unique (PROC_INST_ID_)
                )
                """);
            executeStatement("""
                create table ACT_HI_ACTINST (
                    ID_ nvarchar(64) not null,
                    PROC_DEF_ID_ nvarchar(64) not null,
                    PROC_INST_ID_ nvarchar(64) not null,
                    EXECUTION_ID_ nvarchar(64) not null,
                    ACT_ID_ nvarchar(255) not null,
                    TASK_ID_ nvarchar(64),
                    CALL_PROC_INST_ID_ nvarchar(64),
                    ACT_NAME_ nvarchar(255),
                    ACT_TYPE_ nvarchar(255) not null,
                    ASSIGNEE_ nvarchar(255),
                    START_TIME_ datetime not null,
                    END_TIME_ datetime,
                    DURATION_ numeric(19,0),
                    TENANT_ID_ nvarchar(255) default '',
                    primary key (ID_)
                )
                """);
            executeStatement("""                         
                create table ACT_HI_TASKINST (
                    ID_ nvarchar(64) not null,
                    PROC_DEF_ID_ nvarchar(64),
                    TASK_DEF_KEY_ nvarchar(255),
                    PROC_INST_ID_ nvarchar(64),
                    EXECUTION_ID_ nvarchar(64),
                    NAME_ nvarchar(255),
                    PARENT_TASK_ID_ nvarchar(64),
                    DESCRIPTION_ nvarchar(4000),
                    OWNER_ nvarchar(255),
                    ASSIGNEE_ nvarchar(255),
                    START_TIME_ datetime not null,
                    CLAIM_TIME_ datetime,
                    END_TIME_ datetime,
                    DURATION_ numeric(19,0),
                    DELETE_REASON_ nvarchar(4000),
                    PRIORITY_ int,
                    DUE_DATE_ datetime,
                    FORM_KEY_ nvarchar(255),
                    CATEGORY_ nvarchar(255),
                    TENANT_ID_ nvarchar(255) default '',
                    primary key (ID_)
                )
                """);
            executeStatement("""                             
                create table ACT_HI_VARINST (
                    ID_ nvarchar(64) not null,
                    PROC_INST_ID_ nvarchar(64),
                    EXECUTION_ID_ nvarchar(64),
                    TASK_ID_ nvarchar(64),
                    NAME_ nvarchar(255) not null,
                    VAR_TYPE_ nvarchar(100),
                    REV_ int,
                    BYTEARRAY_ID_ nvarchar(64),
                    DOUBLE_ double precision,
                    LONG_ numeric(19,0),
                    TEXT_ nvarchar(4000),
                    TEXT2_ nvarchar(4000),
                    CREATE_TIME_ datetime,
                    LAST_UPDATED_TIME_ datetime,
                    primary key (ID_)
                )
                """);
            executeStatement("""                          
              create table ACT_HI_DETAIL (
                  ID_ nvarchar(64) not null,
                  TYPE_ nvarchar(255) not null,
                  PROC_INST_ID_ nvarchar(64),
                  EXECUTION_ID_ nvarchar(64),
                  TASK_ID_ nvarchar(64),
                  ACT_INST_ID_ nvarchar(64),
                  NAME_ nvarchar(255) not null,
                  VAR_TYPE_ nvarchar(255),
                  REV_ int,
                  TIME_ datetime not null,
                  BYTEARRAY_ID_ nvarchar(64),
                  DOUBLE_ double precision,
                  LONG_ numeric(19,0),
                  TEXT_ nvarchar(4000),
                  TEXT2_ nvarchar(4000),
                  primary key (ID_)
              )
              """);
            executeStatement("""                            
                create table ACT_HI_COMMENT (
                    ID_ nvarchar(64) not null,
                    TYPE_ nvarchar(255),
                    TIME_ datetime not null,
                    USER_ID_ nvarchar(255),
                    TASK_ID_ nvarchar(64),
                    PROC_INST_ID_ nvarchar(64),
                    ACTION_ nvarchar(255),
                    MESSAGE_ nvarchar(4000),
                    FULL_MSG_ varbinary(max),
                    primary key (ID_)
                )
                """);
            executeStatement("""                           
               create table ACT_HI_ATTACHMENT (
                   ID_ nvarchar(64) not null,
                   REV_ integer,
                   USER_ID_ nvarchar(255),
                   NAME_ nvarchar(255),
                   DESCRIPTION_ nvarchar(4000),
                   TYPE_ nvarchar(255),
                   TASK_ID_ nvarchar(64),
                   PROC_INST_ID_ nvarchar(64),
                   URL_ nvarchar(4000),
                   CONTENT_ID_ nvarchar(64),
                   TIME_ datetime,
                   primary key (ID_)
               )
               """);
            executeStatement("""                            
                create table ACT_HI_IDENTITYLINK (
                    ID_ nvarchar(64),
                    GROUP_ID_ nvarchar(255),
                    TYPE_ nvarchar(255),
                    USER_ID_ nvarchar(255),
                    TASK_ID_ nvarchar(64),
                    PROC_INST_ID_ nvarchar(64),
                    primary key (ID_)
                )
                """);
            executeStatement("""                            
                create table ACT_GE_BYTEARRAY (
                    ID_ nvarchar(64),
                    REV_ int,
                    NAME_ nvarchar(255),
                    DEPLOYMENT_ID_ nvarchar(64),
                    BYTES_  varbinary(max),
                    GENERATED_ tinyint,
                    primary key (ID_)
                );
                """);

            insertData();

            executeStatement("create index ACT_IDX_HI_PRO_INST_END on ACT_HI_PROCINST(END_TIME_)");
            executeStatement("create index ACT_IDX_HI_PRO_I_BUSKEY on ACT_HI_PROCINST(BUSINESS_KEY_)");
            executeStatement("create index ACT_IDX_HI_ACT_INST_START on ACT_HI_ACTINST(START_TIME_)");
            executeStatement("create index ACT_IDX_HI_ACT_INST_END on ACT_HI_ACTINST(END_TIME_)");
            executeStatement("create index ACT_IDX_HI_DETAIL_PROC_INST on ACT_HI_DETAIL(PROC_INST_ID_)");
            executeStatement("create index ACT_IDX_HI_DETAIL_ACT_INST on ACT_HI_DETAIL(ACT_INST_ID_)");
            executeStatement("create index ACT_IDX_HI_DETAIL_TIME on ACT_HI_DETAIL(TIME_)");
            executeStatement("create index ACT_IDX_HI_DETAIL_NAME on ACT_HI_DETAIL(NAME_)");
            executeStatement("create index ACT_IDX_HI_DETAIL_TASK_ID on ACT_HI_DETAIL(TASK_ID_)");
            executeStatement("create index ACT_IDX_HI_PROCVAR_PROC_INST on ACT_HI_VARINST(PROC_INST_ID_)");
            executeStatement("create index ACT_IDX_HI_PROCVAR_NAME_TYPE on ACT_HI_VARINST(NAME_, VAR_TYPE_)");
            executeStatement("create index ACT_IDX_HI_PROCVAR_TASK_ID on ACT_HI_VARINST(TASK_ID_)");
            executeStatement("create index ACT_IDX_HI_ACT_INST_PROCINST on ACT_HI_ACTINST(PROC_INST_ID_, ACT_ID_)");
            executeStatement("create index ACT_IDX_HI_ACT_INST_EXEC on ACT_HI_ACTINST(EXECUTION_ID_, ACT_ID_)");
            executeStatement("create index ACT_IDX_HI_IDENT_LNK_USER on ACT_HI_IDENTITYLINK(USER_ID_)");
            executeStatement("create index ACT_IDX_HI_IDENT_LNK_TASK on ACT_HI_IDENTITYLINK(TASK_ID_)");
            executeStatement("create index ACT_IDX_HI_IDENT_LNK_PROCINST on ACT_HI_IDENTITYLINK(PROC_INST_ID_)");
            executeStatement("create index ACT_IDX_HI_TASK_INST_PROCINST on ACT_HI_TASKINST(PROC_INST_ID_)");
        }

        executeStatement("DROP PROCEDURE usp_DeleteActivityHistory");

        executeStatement("""
            CREATE PROCEDURE usp_DeleteActivityHistory(
                @BeforeStartTimestamp DATETIME,
                @BatchSize INT,
                @DeletedRowCount INT OUTPUT
            )
            AS
            BEGIN                         
                DROP TABLE IF EXISTS #ROOT_PROC_INST_ID_TABLE;
                CREATE TABLE #ROOT_PROC_INST_ID_TABLE (PROC_INST_ID_ NVARCHAR(64));
                
                DROP TABLE IF EXISTS #PROC_INST_ID_TABLE;
                CREATE TABLE #PROC_INST_ID_TABLE (PROC_INST_ID_ NVARCHAR(64));

                DROP TABLE IF EXISTS #TASK_INST_ID_TABLE;
                CREATE TABLE #TASK_INST_ID_TABLE (ID_ NVARCHAR(64));
                                                               
                INSERT INTO #ROOT_PROC_INST_ID_TABLE
                SELECT TOP (@BatchSize) PROC_INST_ID_
                FROM ACT_HI_PROCINST
                WHERE
                    END_TIME_ <= @BeforeStartTimestamp
                    AND END_TIME_ IS NOT NULL
                    AND SUPER_PROCESS_INSTANCE_ID_ IS NULL;
                    
                SET @DeletedRowCount=0;
                DECLARE @DeletedBatchRowCount INT;
                                
                WHILE (SELECT COUNT(*) FROM #ROOT_PROC_INST_ID_TABLE) > 0
                BEGIN
                    TRUNCATE TABLE #PROC_INST_ID_TABLE;
                    TRUNCATE TABLE #TASK_INST_ID_TABLE;
                    
                    SET @DeletedBatchRowCount=0;
                                    
                    WITH ACT_HI_PROCINST_HIERARCHY(PROC_INST_ID_)
                    AS (
                        SELECT PROC_INST_ID_
                        FROM #ROOT_PROC_INST_ID_TABLE
                        UNION ALL
                        SELECT ACT_HI_PROCINST.PROC_INST_ID_
                        FROM ACT_HI_PROCINST
                        INNER JOIN ACT_HI_PROCINST_HIERARCHY ON ACT_HI_PROCINST_HIERARCHY.PROC_INST_ID_ = ACT_HI_PROCINST.SUPER_PROCESS_INSTANCE_ID_
                    )
                    INSERT INTO #PROC_INST_ID_TABLE
                    SELECT PROC_INST_ID_
                    FROM ACT_HI_PROCINST_HIERARCHY;
                    
                    BEGIN TRY
                        BEGIN TRANSACTION;
                        
                        DELETE FROM ACT_GE_BYTEARRAY
                        WHERE ID_ IN (
                            SELECT BYTEARRAY_ID_ FROM ACT_HI_DETAIL
                            WHERE PROC_INST_ID_ IN (SELECT PROC_INST_ID_ FROM #PROC_INST_ID_TABLE)
                        );
                        
                        SET @DeletedBatchRowCount+=@@ROWCOUNT;
                        
                        DELETE FROM ACT_HI_DETAIL
                        WHERE PROC_INST_ID_ IN (SELECT PROC_INST_ID_ FROM #PROC_INST_ID_TABLE);
                                   
                        SET @DeletedBatchRowCount+=@@ROWCOUNT;
                        
                        DELETE FROM ACT_GE_BYTEARRAY
                        WHERE ID_ IN (
                            SELECT BYTEARRAY_ID_ FROM ACT_HI_VARINST
                            WHERE PROC_INST_ID_ IN (SELECT PROC_INST_ID_ FROM #PROC_INST_ID_TABLE)
                        );
                        
                        SET @DeletedBatchRowCount+=@@ROWCOUNT;
                        
                        DELETE FROM ACT_HI_VARINST
                        WHERE PROC_INST_ID_ IN (SELECT PROC_INST_ID_ FROM #PROC_INST_ID_TABLE);
                                                                      
                        SET @DeletedBatchRowCount+=@@ROWCOUNT;
                        
                        DELETE FROM ACT_HI_ACTINST
                        WHERE PROC_INST_ID_ IN (SELECT PROC_INST_ID_ FROM #PROC_INST_ID_TABLE);
                                   
                        SET @DeletedBatchRowCount+=@@ROWCOUNT;
                        
                        -- Delete ACT_HI_TASKINST rows recursive along with their associated: 
                        -- ACT_HI_DETAIL, ACT_HI_VARINST, ACT_HI_COMMENT, ACT_HI_ATTACHMENT, ACT_HI_IDENTITYLINK
                        BEGIN
                            WITH ACT_HI_TASKINST_HIERARCHY(ID_)
                            AS (
                                SELECT ID_
                                FROM ACT_HI_TASKINST
                                WHERE PROC_INST_ID_ IN (SELECT PROC_INST_ID_ FROM #PROC_INST_ID_TABLE)
                                UNION ALL
                                SELECT ACT_HI_TASKINST.ID_
                                FROM ACT_HI_TASKINST
                                INNER JOIN ACT_HI_TASKINST_HIERARCHY ON ACT_HI_TASKINST_HIERARCHY.ID_ = ACT_HI_TASKINST.PARENT_TASK_ID_
                            )
                            INSERT INTO #TASK_INST_ID_TABLE
                            SELECT ID_
                            FROM ACT_HI_TASKINST_HIERARCHY;
                            
                            DELETE FROM ACT_GE_BYTEARRAY
                            WHERE ID_ IN (
                                SELECT BYTEARRAY_ID_ FROM ACT_HI_DETAIL
                                WHERE TASK_ID_ IN (SELECT ID_ FROM #TASK_INST_ID_TABLE)
                            );
                                       
                            SET @DeletedBatchRowCount+=@@ROWCOUNT;
                            
                            DELETE FROM ACT_HI_DETAIL
                            WHERE TASK_ID_ IN (SELECT ID_ FROM #TASK_INST_ID_TABLE);
                                       
                            SET @DeletedBatchRowCount+=@@ROWCOUNT;
                            
                            DELETE FROM ACT_GE_BYTEARRAY
                            WHERE ID_ IN (
                                SELECT BYTEARRAY_ID_ FROM ACT_HI_VARINST
                                WHERE TASK_ID_ IN (SELECT ID_ FROM #TASK_INST_ID_TABLE)
                            );
                                       
                            SET @DeletedBatchRowCount+=@@ROWCOUNT;
                            
                            DELETE FROM ACT_HI_VARINST
                            WHERE TASK_ID_ IN (SELECT ID_ FROM #TASK_INST_ID_TABLE);
                                       
                            SET @DeletedBatchRowCount+=@@ROWCOUNT;
                            
                            DELETE FROM ACT_HI_COMMENT
                            WHERE TASK_ID_ IN (SELECT ID_ FROM #TASK_INST_ID_TABLE);
                                       
                            SET @DeletedBatchRowCount+=@@ROWCOUNT;
                            
                            DELETE FROM ACT_GE_BYTEARRAY
                            WHERE ID_ IN (
                                SELECT CONTENT_ID_ FROM ACT_HI_ATTACHMENT
                                WHERE TASK_ID_ IN (SELECT ID_ FROM #TASK_INST_ID_TABLE)
                            );
                                       
                            SET @DeletedBatchRowCount+=@@ROWCOUNT;
                            
                            DELETE FROM ACT_HI_ATTACHMENT
                            WHERE TASK_ID_ IN (SELECT ID_ FROM #TASK_INST_ID_TABLE);
                                       
                            SET @DeletedBatchRowCount+=@@ROWCOUNT;
                            
                            DELETE FROM ACT_HI_IDENTITYLINK
                            WHERE TASK_ID_ IN (SELECT ID_ FROM #TASK_INST_ID_TABLE);
                                       
                            SET @DeletedBatchRowCount+=@@ROWCOUNT;
                            
                            DELETE FROM ACT_HI_TASKINST
                            WHERE ID_ IN (SELECT ID_ FROM #TASK_INST_ID_TABLE);
                                       
                            SET @DeletedBatchRowCount+=@@ROWCOUNT;
                            
                        END;
                               
                        DELETE FROM ACT_HI_IDENTITYLINK
                        WHERE PROC_INST_ID_ IN (SELECT PROC_INST_ID_ FROM #PROC_INST_ID_TABLE);
                                   
                        SET @DeletedBatchRowCount+=@@ROWCOUNT;
                                   
                        DELETE FROM ACT_HI_COMMENT
                        WHERE PROC_INST_ID_ IN (SELECT PROC_INST_ID_ FROM #PROC_INST_ID_TABLE);
                                   
                        SET @DeletedBatchRowCount+=@@ROWCOUNT;
                                   
                        DELETE FROM ACT_HI_PROCINST
                        WHERE PROC_INST_ID_ IN (SELECT PROC_INST_ID_ FROM #PROC_INST_ID_TABLE);
                                   
                        SET @DeletedBatchRowCount+=@@ROWCOUNT;
                                                                   
                        COMMIT TRANSACTION;
                        SET @DeletedRowCount+=@DeletedBatchRowCount;
                    END TRY
                    BEGIN CATCH
                        IF (XACT_STATE()) = -1
                            -- The current transaction cannot be committed.
                            BEGIN
                                PRINT
                                    N'The transaction cannot be committed. Rolling back transaction.'
                                ROLLBACK TRANSACTION;
                            END;
                        ELSE
                            IF (XACT_STATE()) = 1
                            -- The current transaction can be committed.
                                BEGIN
                                    PRINT
                                        N'Exception was caught, but the trasaction can be committed.'
                                    COMMIT TRANSACTION;   
                                END;
                    END CATCH;
                               
                    TRUNCATE TABLE #ROOT_PROC_INST_ID_TABLE;
                    
                    INSERT INTO #ROOT_PROC_INST_ID_TABLE
                    SELECT TOP (@BatchSize) PROC_INST_ID_
                    FROM ACT_HI_PROCINST
                    WHERE
                        END_TIME_ <= @BeforeStartTimestamp
                        AND END_TIME_ IS NOT NULL
                        AND SUPER_PROCESS_INSTANCE_ID_ IS NULL;
                END
                
                DROP TABLE IF EXISTS #ROOT_PROC_INST_ID_TABLE;                
                DROP TABLE IF EXISTS #PROC_INST_ID_TABLE;
                DROP TABLE IF EXISTS #TASK_INST_ID_TABLE;
            END
            """
        );
    }

    private final boolean recreateTables = true;

    private final int ACT_HI_PROCINST_ROOT_COUNT = 15;
    private final int ACT_HI_ACTINST_PER_PROC_COUNT = 5;
    private final int ACT_HI_DETAIL_PER_PROC_COUNT = 5;
    private final int ACT_HI_TASKINST_PER_PROC_COUNT = 5;
    private final int ACT_HI_VARINST_PER_TASK_COUNT = 5;
    private final int ACT_HI_DETAIL_PER_TASK_COUNT = 5;
    private final int ACT_HI_COMMENT_PER_TASK_COUNT = 5;
    private final int ACT_HI_ATTACHMENT_PER_TASK_COUNT = 5;
    private final int ACT_HI_IDENTITYLINK_PER_TASK_COUNT = 5;

    private int procInstId = 1;
    private int actInstId = 1;
    private int taskInstId = 1;
    private int varInstId = 1;
    private int detailId = 1;
    private int commentId = 1;
    private int attachmentId = 1;
    private int identityLinkId = 1;
    private int byteArrayId = 1;

    private void insertData() {
        doInJPA(entityManager -> {
            int procInstRootCount = 0;
            while (procInstRootCount < ACT_HI_PROCINST_ROOT_COUNT) {
                //Add a new root process
                int rootId = insertProcInst(entityManager, procInstId++, null);
                //Add two child process instances
                int child1Id = insertProcInst(entityManager, procInstId++, rootId);
                int child2Id = insertProcInst(entityManager, procInstId++, rootId);
                //Add two grandchild process instances per child
                insertProcInst(entityManager, procInstId++, child1Id);
                insertProcInst(entityManager, procInstId++, child1Id);
                insertProcInst(entityManager, procInstId++, child2Id);
                insertProcInst(entityManager, procInstId++, child2Id);

                procInstRootCount++;
            }
        });
    }

    private int insertProcInst(EntityManager entityManager, int procId, Integer parentProcId) {
        entityManager.createNativeQuery("""
                INSERT INTO [ACT_HI_PROCINST] (
                    [ID_],
                    [PROC_INST_ID_],
                    [PROC_DEF_ID_],
                    [SUPER_PROCESS_INSTANCE_ID_],
                    [START_TIME_],
                    [END_TIME_]
                )
                VALUES (
                    :id,
                    :proc_inst_id_,
                    'Proc Def',
                    :super_process_instance_id_,
                    :start_time_,
                    :end_time_                    
                )
                """)
            .setParameter("id", String.valueOf(procId))
            .setParameter("proc_inst_id_", String.valueOf(procId))
            .setParameter("super_process_instance_id_", parentProcId != null ? String.valueOf(parentProcId) : null)
            .setParameter("start_time_", Timestamp.valueOf(LocalDate.of(2020, 11, 25).atStartOfDay().plusHours(procId)))
            .setParameter("end_time_", Timestamp.valueOf(LocalDate.of(2020, 12, 25).atStartOfDay().plusHours(procId)))
            .executeUpdate();

        entityManager.unwrap(Session.class).doWork(connection -> {
            insertActivities(connection, procId);
            insertDetails(connection, procId);
        });


        for (int j = 1; j <= ACT_HI_TASKINST_PER_PROC_COUNT; j++) {
            //Add a new root task
            int rootTaskId  = insertTaskInst(entityManager, procId, null);
            //Add two child task instances
            int child1TaskId = insertTaskInst(entityManager, procId, rootTaskId);
            int child2TaskId = insertTaskInst(entityManager, procId, rootTaskId);
            //Add two grandchild task instances per child
            insertTaskInst(entityManager, procId, child1TaskId);
            insertTaskInst(entityManager, procId, child1TaskId);
            insertTaskInst(entityManager, procId, child2TaskId);
            insertTaskInst(entityManager, procId, child2TaskId);
        }

        insertIdentityLinks(entityManager, procId);
        insertComments(entityManager, procId);

        return procId;
    }

    private void insertActivities(Connection connection, int procId) throws SQLException {
        try(PreparedStatement preparedStatement = connection.prepareStatement("""
            INSERT INTO [ACT_HI_ACTINST] (
                [ID_],
                [PROC_INST_ID_],
                [PROC_DEF_ID_],
                [EXECUTION_ID_],
                [ACT_ID_],
                [ACT_TYPE_],
                [START_TIME_])
            VALUES (
                ?,
                ?,
                1,
                'Exec Id',
                'Act Type', 
                'Act Id', 
                ?
            )
            """
        )) {
            for (int j = 1; j <= ACT_HI_ACTINST_PER_PROC_COUNT; j++) {
                int actId = actInstId++;
                int index = 1;
                preparedStatement.setString(index++, String.valueOf(actId));
                preparedStatement.setString(index++, String.valueOf(procId));
                preparedStatement.setTimestamp(index++, Timestamp.valueOf(LocalDate.of(2020, 11, 25).atStartOfDay().plusHours(procId).plusMinutes(j)));

                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
    }

    private int insertTaskInst(EntityManager entityManager, int procId, Integer parentTaskId) {
        int taskId = taskInstId++;

        entityManager.createNativeQuery("""
            INSERT INTO [ACT_HI_TASKINST] (
                [ID_],
                [PROC_INST_ID_],
                [PARENT_TASK_ID_],
                [START_TIME_])
            VALUES (
                :id,
                :proc_inst_id_,
                :parent_task_id_, 
                :start_time_
            )
            """)
        .setParameter("id", String.valueOf(taskId))
        .setParameter("proc_inst_id_", parentTaskId != null ? null : String.valueOf(procId))
        .setParameter("parent_task_id_", parentTaskId != null ? String.valueOf(parentTaskId) : null)
        .setParameter("start_time_", Timestamp.valueOf(LocalDate.of(2020, 11, 25).atStartOfDay().plusHours(procId).plusMinutes(taskId)))
        .executeUpdate();

        entityManager.unwrap(Session.class).doWork(connection -> {
            insertVarInsts(connection, procId, taskId, parentTaskId != null);
            insertDetails(connection, procId, taskId, parentTaskId != null);
            insertComments(connection, procId, taskId, parentTaskId != null);
            insertAttachments(connection, procId, taskId, parentTaskId != null);
            insertIdentityLinks(connection, procId, taskId, parentTaskId != null);
        });

        return taskId;
    }

    private void insertVarInsts(Connection connection, int procId, int taskId, boolean subTask) throws SQLException {
        try(PreparedStatement varInstPreparedStatement = connection.prepareStatement("""
            INSERT INTO [ACT_HI_VARINST] (
                [ID_],
                [PROC_INST_ID_],
                [TASK_ID_],
                [NAME_],
                [BYTEARRAY_ID_],
                [CREATE_TIME_])
            VALUES (
                ?,
                ?,
                ?, 
                ?, 
                ?, 
                ?
            )
            """);
            PreparedStatement byteArrayPreparedStatement = connection.prepareStatement("""
                INSERT INTO [ACT_GE_BYTEARRAY] (
                    [ID_],
                    [NAME_])
                VALUES (
                    ?,
                    ?
                )
                """
            )
        ) {
            for (int i = 1; i <= ACT_HI_VARINST_PER_TASK_COUNT; i++) {
                int id = varInstId++;
                byteArrayId++;
                int index = 1;
                varInstPreparedStatement.setString(index++, String.valueOf(id));
                varInstPreparedStatement.setString(index++, subTask ? null : String.valueOf(procId));
                varInstPreparedStatement.setString(index++, String.valueOf(taskId));
                varInstPreparedStatement.setString(index++, String.format("Task Var: %d", id));
                varInstPreparedStatement.setString(index++, String.valueOf(byteArrayId));
                varInstPreparedStatement.setTimestamp(index++, Timestamp.valueOf(LocalDate.of(2020, 11, 25).atStartOfDay().plusHours(procId).plusMinutes(taskId).plusSeconds(id)));

                varInstPreparedStatement.addBatch();

                index = 1;
                byteArrayPreparedStatement.setString(index++, String.valueOf(byteArrayId));
                byteArrayPreparedStatement.setString(index++, String.format("Task Var Byte Array: %d", id));

                byteArrayPreparedStatement.addBatch();
            }
            varInstPreparedStatement.executeBatch();
            byteArrayPreparedStatement.executeBatch();
        }
    }

    private void insertDetails(Connection connection, int procId) throws SQLException {
        try(PreparedStatement detailPreparedStatement = connection.prepareStatement("""
            INSERT INTO [ACT_HI_DETAIL] (
                [ID_],
                [TYPE_],
                [PROC_INST_ID_],
                [NAME_],
                [BYTEARRAY_ID_],
                [TIME_]
            )
            VALUES (
                ?,
                'Type',
                ?,
                ?,
                ?,
                ?
            )
            """);
            PreparedStatement byteArrayPreparedStatement = connection.prepareStatement("""
                INSERT INTO [ACT_GE_BYTEARRAY] (
                    [ID_],
                    [NAME_])
                VALUES (
                    ?,
                    ?
                )
                """
            )
        ) {
            for (int i = 1; i <= ACT_HI_DETAIL_PER_PROC_COUNT; i++) {
                int id = detailId++;
                byteArrayId++;
                int index = 1;

                detailPreparedStatement.setString(index++, String.valueOf(id));
                detailPreparedStatement.setString(index++, String.valueOf(procId));
                detailPreparedStatement.setString(index++, String.format("Proc Detail: %d", id));
                detailPreparedStatement.setString(index++, String.valueOf(byteArrayId));
                detailPreparedStatement.setTimestamp(index++, Timestamp.valueOf(LocalDate.of(2020, 11, 25).atStartOfDay().plusHours(procId)));

                detailPreparedStatement.addBatch();

                index = 1;
                byteArrayPreparedStatement.setString(index++, String.valueOf(byteArrayId));
                byteArrayPreparedStatement.setString(index++, String.format("Proc Detail Byte Array: %d", id));

                byteArrayPreparedStatement.addBatch();
            }
            detailPreparedStatement.executeBatch();
            byteArrayPreparedStatement.executeBatch();
        }
    }

    private void insertDetails(Connection connection, int procId, int taskId, boolean subTask) throws SQLException {
        try(PreparedStatement detailPreparedStatement = connection.prepareStatement("""
            INSERT INTO [ACT_HI_DETAIL] (
                [ID_],
                [TYPE_],
                [PROC_INST_ID_],
                [TASK_ID_],
                [NAME_],
                [BYTEARRAY_ID_],
                [TIME_]
            )
            VALUES (
                ?,
                'Type',
                ?,
                ?,
                ?,
                ?,
                ?
            )
            """);
            PreparedStatement byteArrayPreparedStatement = connection.prepareStatement("""
                INSERT INTO [ACT_GE_BYTEARRAY] (
                    [ID_],
                    [NAME_])
                VALUES (
                    ?,
                    ?
                )
                """
            )
        ) {
            for (int i = 1; i <= ACT_HI_DETAIL_PER_TASK_COUNT; i++) {
                int id = detailId++;
                byteArrayId++;
                int index = 1;

                detailPreparedStatement.setString(index++, String.valueOf(id));
                detailPreparedStatement.setString(index++, subTask ? null : String.valueOf(procId));
                detailPreparedStatement.setString(index++, String.valueOf(taskId));
                detailPreparedStatement.setString(index++, String.format("Task Detail: %d", id));
                detailPreparedStatement.setString(index++, String.valueOf(byteArrayId));
                detailPreparedStatement.setTimestamp(index++, Timestamp.valueOf(LocalDate.of(2020, 11, 25).atStartOfDay().plusHours(procId).plusMinutes(taskId).plusSeconds(id)));

                detailPreparedStatement.addBatch();

                index = 1;
                byteArrayPreparedStatement.setString(index++, String.valueOf(byteArrayId));
                byteArrayPreparedStatement.setString(index++, String.format("Task Detail Byte Array: %d", id));

                byteArrayPreparedStatement.addBatch();
            }
            detailPreparedStatement.executeBatch();
            byteArrayPreparedStatement.executeBatch();
        }
    }

    private void insertComments(Connection connection, int procId, int taskId, boolean subTask) throws SQLException {
        try(PreparedStatement preparedStatement = connection.prepareStatement("""
            INSERT INTO [ACT_HI_COMMENT] (
                [ID_],
                [PROC_INST_ID_],
                [TASK_ID_],
                [TIME_])
            VALUES (
                ?,
                ?,
                ?,
                ?
            )
            """
        )) {
            for (int i = 1; i <= ACT_HI_COMMENT_PER_TASK_COUNT; i++) {
                int id = commentId++;
                int index = 1;
                preparedStatement.setString(index++, String.valueOf(id));
                preparedStatement.setString(index++, subTask ? null : String.valueOf(procId));
                preparedStatement.setString(index++, String.valueOf(taskId));
                preparedStatement.setTimestamp(index++, Timestamp.valueOf(LocalDate.of(2020, 11, 25).atStartOfDay().plusHours(procId).plusMinutes(taskId).plusSeconds(id)));

                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
    }

    private void insertComments(EntityManager entityManager, int procId) {
        int id = commentId++;
        entityManager.createNativeQuery("""
            INSERT INTO [ACT_HI_COMMENT] (
                [ID_],
                [PROC_INST_ID_],
                [TIME_]
            )
            VALUES (
                :id,
                :proc_inst_id_,
                :time
            )
        """)
        .setParameter("id", String.valueOf(id))
        .setParameter("proc_inst_id_", String.valueOf(procId))
        .setParameter("time", Timestamp.valueOf(LocalDate.of(2020, 11, 25).atStartOfDay().plusHours(procId).plusMinutes(id)))
        .executeUpdate();
    }

    private void insertAttachments(Connection connection, int procId, int taskId, boolean subTask) throws SQLException {
        try(PreparedStatement attachmentPreparedStatement = connection.prepareStatement("""
            INSERT INTO [ACT_HI_ATTACHMENT] (
                [ID_],
                [PROC_INST_ID_],
                [TASK_ID_],
                [CONTENT_ID_],
                [TIME_]
            )
            VALUES (
                ?,
                ?,
                ?,
                ?,
                ?
            )
            """);
            PreparedStatement byteArrayPreparedStatement = connection.prepareStatement("""
                INSERT INTO [ACT_GE_BYTEARRAY] (
                    [ID_],
                    [NAME_])
                VALUES (
                    ?,
                    ?
                )
                """
            )
        ) {
            for (int i = 1; i <= ACT_HI_ATTACHMENT_PER_TASK_COUNT; i++) {
                int id = attachmentId++;
                byteArrayId++;
                int index = 1;
                attachmentPreparedStatement.setString(index++, String.valueOf(id));
                attachmentPreparedStatement.setString(index++, subTask ? null : String.valueOf(procId));
                attachmentPreparedStatement.setString(index++, String.valueOf(taskId));
                attachmentPreparedStatement.setString(index++, String.valueOf(byteArrayId));
                attachmentPreparedStatement.setTimestamp(index++, Timestamp.valueOf(LocalDate.of(2020, 11, 25).atStartOfDay().plusHours(procId).plusMinutes(taskId).plusSeconds(id)));

                attachmentPreparedStatement.addBatch();

                index = 1;
                byteArrayPreparedStatement.setString(index++, String.valueOf(byteArrayId));
                byteArrayPreparedStatement.setString(index++, String.format("Var: %d", id));

                byteArrayPreparedStatement.addBatch();
            }
            attachmentPreparedStatement.executeBatch();
            byteArrayPreparedStatement.executeBatch();
        }
    }

    private void insertIdentityLinks(Connection connection, int procId, int taskId, boolean subTask) throws SQLException {
        try(PreparedStatement preparedStatement = connection.prepareStatement("""
            INSERT INTO [ACT_HI_IDENTITYLINK] (
                [ID_],
                [PROC_INST_ID_],
                [TASK_ID_]
            )
            VALUES (
                ?,
                ?,
                ?
            )
            """
        )) {
            for (int i = 1; i <= ACT_HI_IDENTITYLINK_PER_TASK_COUNT; i++) {
                int id = identityLinkId++;
                int index = 1;
                preparedStatement.setString(index++, String.valueOf(id));
                preparedStatement.setString(index++, subTask ? null : String.valueOf(procId));
                preparedStatement.setString(index++, String.valueOf(taskId));

                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
    }

    private void insertIdentityLinks(EntityManager entityManager, int procId) {
        int id = identityLinkId++;

        entityManager.createNativeQuery("""
            INSERT INTO [ACT_HI_IDENTITYLINK] (
                [ID_],
                [PROC_INST_ID_]
            )
            VALUES (
                :id,
                :proc_inst_id_
            )
        """)
        .setParameter("id", String.valueOf(id))
        .setParameter("proc_inst_id_", String.valueOf(procId))
        .executeUpdate();
    }

    @Test
    public void testDeleteActivityHistory() {
        try(Connection connection = dataSourceProvider().dataSource().getConnection()) {
            deleteActivityHistoryBeforeDate(
                connection,
                Timestamp.valueOf(LocalDateTime.now()),
                10
            );
        } catch (SQLException e) {
            LOGGER.error("Error getting database connection", e);
        }

        doInJPA(entityManager -> {
            assertEquals(0, ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM ACT_HI_PROCINST").getSingleResult()).intValue());
            assertEquals(0, ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM ACT_HI_ACTINST").getSingleResult()).intValue());
            assertEquals(0, ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM ACT_HI_TASKINST").getSingleResult()).intValue());
            assertEquals(0, ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM ACT_GE_BYTEARRAY").getSingleResult()).intValue());
            assertEquals(0, ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM ACT_HI_VARINST").getSingleResult()).intValue());
            assertEquals(0, ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM ACT_HI_DETAIL").getSingleResult()).intValue());
            assertEquals(0, ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM ACT_HI_COMMENT").getSingleResult()).intValue());
            assertEquals(0, ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM ACT_HI_ATTACHMENT").getSingleResult()).intValue());
            assertEquals(0, ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM ACT_HI_IDENTITYLINK").getSingleResult()).intValue());
        });
    }

    private int deleteActivityHistoryBeforeDate(Connection connection, Timestamp olderThanTimestamp, int batchSize) {
        long startNanos = System.nanoTime();
        try (CallableStatement sp = connection.prepareCall("{ call usp_DeleteActivityHistory(?, ?, ?) }")) {
            sp.setTimestamp(1, olderThanTimestamp);
            sp.setInt(2, batchSize);
            sp.registerOutParameter("DeletedRowCount", Types.INTEGER);
            sp.execute();
            int rowCount = sp.getInt("DeletedRowCount");
            LOGGER.info(
                "Deleted {} records in {} milliseconds", 
                rowCount,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)
            );
            return rowCount;
        } catch (SQLException e) {
            LOGGER.error("The usp_DeleteActivityHistory execution failed", e);
            return 0;
        }
    }
}
