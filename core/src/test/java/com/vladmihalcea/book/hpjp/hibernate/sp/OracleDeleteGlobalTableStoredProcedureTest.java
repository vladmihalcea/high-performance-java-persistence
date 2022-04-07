package com.vladmihalcea.book.hpjp.hibernate.sp;

import com.vladmihalcea.book.hpjp.util.AbstractOracleIntegrationTest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import jakarta.persistence.*;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Vlad Mihalcea
 */
public class OracleDeleteGlobalTableStoredProcedureTest extends AbstractOracleIntegrationTest {

    private int infoEntryCount;
    private int errorEntryCount;
    private int warnEntryCount;

    private int multiplier = 1;

    private int totalEntryCount;

    private Date timestamp = Timestamp.valueOf(LocalDateTime.now().minusDays(60));
    private long millisStep;

    private int batchSize = 50;

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            LogEntry.class
        };
    }

    public void afterInit() {
        executeStatement("DROP TABLE deletable_rowid");
        executeStatement("CREATE GLOBAL TEMPORARY TABLE deletable_rowid(rid urowid) ON COMMIT PRESERVE ROWS");
        executeStatement("""
            CREATE OR REPLACE PROCEDURE delete_log_entries (
                logLevel IN VARCHAR2,
                daysOld IN NUMBER,
                batchSize IN NUMBER,
                deletedCount OUT NUMBER
            ) AS
                v_row deletable_rowid%rowtype;
            BEGIN   
                 INSERT INTO deletable_rowid 
                 SELECT rowid FROM log_entry 
                 WHERE 
                    log_level = 'INFO' AND 
                    created_on < (SELECT sysdate - 30 FROM dual);
                 COMMIT;
                
                 deletedCount:=0;
                
                 FOR v_row IN (SELECT * FROM deletable_rowid x)
                 LOOP
                    deletedCount:=deletedCount+1;
                    DELETE FROM log_entry WHERE rowid = v_row.rid;
                    IF mod(deletedCount, batchSize)=0 THEN
                      COMMIT;
                    END IF;
                 END LOOP;
                 COMMIT;
            END;
            """);

        infoEntryCount = 100 * multiplier;
        errorEntryCount = 50 * multiplier;
        warnEntryCount = 100 * multiplier;

        totalEntryCount = ( infoEntryCount + errorEntryCount + warnEntryCount ) * 2;
        millisStep = ( new Date().getTime() - timestamp.getTime() ) / totalEntryCount;

        doInJPA(entityManager -> {
            int oldEntryThreshold = totalEntryCount / 2;

            long logTimestamp = timestamp.getTime();

            for (int i = 0; i < totalEntryCount; i++) {
                if(i % batchSize == 0 && i > 0) {
                    entityManager.getTransaction().commit();
                    entityManager.getTransaction().begin();
                    entityManager.clear();
                }

                LogEntry log = new LogEntry();
                int index = i % oldEntryThreshold;

                if(index < infoEntryCount) {
                    log.setLevel(LogLevel.INFO);
                } else if (index < infoEntryCount + errorEntryCount) {
                    log.setLevel(LogLevel.ERROR);
                } else {
                    log.setLevel(LogLevel.WARN);
                }
                log.setMessage(log.getLevel().name());
                logTimestamp += millisStep;
                log.setCreatedOn(new Date(logTimestamp));
                entityManager.persist(log);
            }
        });
    }

    @Test
    public void testStoredProcedureOutParameter() {
        doInJPA(entityManager -> {
            long startNanos = System.nanoTime();
            StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("delete_log_entries")
                .registerStoredProcedureParameter(1, String.class, ParameterMode.IN)
                .registerStoredProcedureParameter(2, Integer.class, ParameterMode.IN)
                .registerStoredProcedureParameter(3, Integer.class, ParameterMode.IN)
                .registerStoredProcedureParameter(4, Integer.class, ParameterMode.OUT)
                .setParameter(1, LogLevel.INFO.name())
                .setParameter(2, 30)
                .setParameter(3, 1000);
            query.execute();

            Integer deleteCount = (Integer) query.getOutputParameterValue(4);
            long endNanos = System.nanoTime();
            LOGGER.info("Delete {} entries out of {} took {} ms", deleteCount, totalEntryCount, TimeUnit.NANOSECONDS.toMillis(endNanos - startNanos));
        });
    }

    @Test
    public void testBulkDelete() {
        doInJPA(entityManager -> {
            long startNanos = System.nanoTime();
            int deleteCount = entityManager.createQuery("""
                    DELETE FROM LogEntry
                    WHERE level = :level
                    AND createdOn < :timestamp
                    """)
                .setParameter("level", LogLevel.INFO)
                .setParameter("timestamp", Timestamp.valueOf(LocalDateTime.now().minusDays(30)))
                .executeUpdate();
            long endNanos = System.nanoTime();
            LOGGER.info("Delete {} entries out of {} took {} ms", deleteCount, totalEntryCount, TimeUnit.NANOSECONDS.toMillis(endNanos - startNanos));
        });
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.jdbc.batch_size", "50");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        properties.put("hibernate.jdbc.batch_versioned_data", "true");
        return properties;
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
