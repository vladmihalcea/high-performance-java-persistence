package com.vladmihalcea.book.hpjp.hibernate.sp;

import com.vladmihalcea.book.hpjp.util.AbstractOracleXEIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * <code>OracleDeleteStoredProcedureTest</code> - Oracle Delete StoredProcedure Test
 *
 * @author Vlad Mihalcea
 */
public class OracleDeleteStoredProcedureTest extends AbstractOracleXEIntegrationTest {

    private int logEntryCount = 100;
    private int batchSize = 50;
    private int infoCount = 0;

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            LogEntry.class
        };
    }

    @Before
    public void init() {
        super.init();
        doInJDBC(connection -> {
            try(Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                    "CREATE OR REPLACE PROCEDURE delete_log_entries ( " +
                    "    logLevel IN VARCHAR2, " +
                    "    daysOld IN NUMBER, " +
                    "    batchSize IN NUMBER, " +
                    "    deletedCount OUT NUMBER     " +
                    ") AS  " +
                    "    TYPE ARRAY_NUMBER IS TABLE OF NUMBER; " +
                    "    ids ARRAY_NUMBER;     " +
                    "    CURSOR select_cursor IS " +
                    "        SELECT id  " +
                    "        FROM log_entry  " +
                    "        WHERE log_level = logLevel AND created_on < (SELECT sysdate - daysOld FROM dual);     " +
                    "BEGIN " +
                    "    deletedCount := 0; " +
                    "    OPEN select_cursor; " +
                    "    LOOP " +
                    "        FETCH select_cursor BULK COLLECT INTO ids LIMIT batchSize; " +
                    "        FORALL i IN 1 .. ids.COUNT " +
                    "        DELETE FROM log_entry WHERE id = ids(i); " +
                    "        deletedCount := deletedCount + sql%rowcount; " +
                    "        COMMIT; " +
                    "        EXIT WHEN select_cursor%NOTFOUND; " +
                    "    END LOOP; " +
                    "CLOSE select_cursor; " +
                    "EXCEPTION " +
                    "    WHEN NO_DATA_FOUND THEN NULL; " +
                    "    WHEN OTHERS THEN RAISE; " +
                    "END delete_log_entries;"
                );
            }
        });
        Date timestamp = Timestamp.valueOf(LocalDateTime.now().minusDays(31));

        EntityManager entityManager = entityManagerFactory().createEntityManager();
        EntityTransaction txn = entityManager.getTransaction();
        txn.begin();

        try {
            for (int i = 0; i < logEntryCount; i++) {
                if(i % batchSize == 0 && i > 0) {
                    txn.commit();
                    txn.begin();
                    entityManager.clear();
                }
                double entropy = Math.random();

                LogEntry log = new LogEntry();
                log.setLevel(entropy < 0.1 ? LogLevel.ERROR : entropy < 0.2 ? LogLevel.WARN : LogLevel.INFO);
                log.setMessage(log.getLevel().name());
                log.setCreatedOn(timestamp);
                entityManager.persist(log);

                if(log.getLevel() == LogLevel.INFO) {
                    infoCount++;
                }
            }
        } finally {
            txn.commit();
            entityManager.close();
        }

    }

    @Test
    public void testStoredProcedureOutParameter() {
        doInJPA(entityManager -> {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("delete_log_entries");
            query.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(2, Integer.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(3, Integer.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(4, Integer.class, ParameterMode.OUT);

            query.setParameter(1, LogLevel.INFO.name());
            query.setParameter(2, 30);
            query.setParameter(3, 50);

            query.execute();
            Integer count = (Integer) query.getOutputParameterValue(4);
            assertEquals(Integer.valueOf(infoCount), count);
        });
    }

    public enum LogLevel {
        INFO,
        WARN,
        ERROR
    }

    @Entity(name = "LogEntry")
    @Table(name = "log_entry")
    public static class LogEntry {

        @Id
        @GeneratedValue
        private Long id;

        @Column(name = "log_level")
        @Enumerated(EnumType.STRING)
        private LogLevel level;

        @Column(name = "log_message")
        private String message;

        @Temporal(TemporalType.TIMESTAMP)
        @Column(name = "created_on")
        private Date createdOn;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public LogLevel getLevel() {
            return level;
        }

        public void setLevel(LogLevel level) {
            this.level = level;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
        }
    }
}
