package com.vladmihalcea.book.hpjp.hibernate.sp;

import com.vladmihalcea.book.hpjp.util.AbstractOracleXEIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.persistence.*;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Vlad Mihalcea
 */
@RunWith(Parameterized.class)
public class OracleDeleteGlobalTableStoredProcedureTest extends AbstractOracleXEIntegrationTest {

    private final int infoEntryCount;
    private final int errorEntryCount;
    private final int warnEntryCount;

    private final int totalEntryCount;

    private Date timestamp = Timestamp.valueOf(LocalDateTime.now().minusDays(60));
    private long millisStep;

    private int batchSize = 50;

    public OracleDeleteGlobalTableStoredProcedureTest(int multiplier) {
        infoEntryCount = 100 * multiplier;
        errorEntryCount = 50 * multiplier;
        warnEntryCount = 100 * multiplier;

        totalEntryCount = ( infoEntryCount + errorEntryCount + warnEntryCount ) * 2;
        millisStep = ( new Date().getTime() - timestamp.getTime() ) / totalEntryCount;
    }

    @Parameterized.Parameters
    public static Collection<Integer[]> parameters() {
        List<Integer[]> multipliers = new ArrayList<>();
 /*       multipliers.add(new Integer[] {1});
        multipliers.add(new Integer[] {10});
        multipliers.add(new Integer[] {50});
        multipliers.add(new Integer[] {100});*/
        multipliers.add(new Integer[] {500});
/*        multipliers.add(new Integer[] {1000});
        multipliers.add(new Integer[] {2000});*/
        return multipliers;
    }

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
                    "drop table deletable_rowid"
                );
                statement.executeUpdate(
                    "create global temporary table deletable_rowid(rid urowid) on commit preserve rows "
                );
            }
        });

        doInJDBC(connection -> {
            try(Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                    "CREATE OR REPLACE PROCEDURE delete_log_entries ( " +
                    "    logLevel IN VARCHAR2,  " +
                    "    daysOld IN NUMBER,  " +
                    "    batchSize IN NUMBER,  " +
                    "    deletedCount OUT NUMBER      " +
                    ") AS  " +
                    "    v_row deletable_rowid%rowtype;  " +
                    "BEGIN      " +
                    "     insert into deletable_rowid SELECT rowid FROM log_entry WHERE log_level = 'INFO' AND created_on < (SELECT sysdate - 30 FROM dual); " +
                    "     commit; " +
                    "      " +
                    "     deletedCount:=0; " +
                    "      " +
                    "     for v_row in (select * from deletable_rowid x) " +
                    "     loop " +
                    "        deletedCount:=deletedCount+1; " +
                    "        delete from log_entry where rowid=v_row.rid; " +
                    "        if mod(deletedCount,batchSize)=0 then " +
                    "          commit; " +
                    "        end if; " +
                    "     end loop; " +
                    "     commit; " +
                    "END; "
                );
            }
        });

        EntityManager entityManager = entityManagerFactory().createEntityManager();
        EntityTransaction txn = entityManager.getTransaction();
        txn.begin();

        try {

            int oldEntryThreshold = totalEntryCount / 2;

            long logTimestamp = timestamp.getTime();

            for (int i = 0; i < totalEntryCount; i++) {
                if(i % batchSize == 0 && i > 0) {
                    txn.commit();
                    txn.begin();
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
        } finally {
            txn.commit();
            entityManager.close();
        }
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
            int deleteCount = entityManager.createQuery(
                "DELETE FROM LogEntry " +
                "WHERE level = :level AND createdOn < :timestamp")
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
