package com.vladmihalcea.hpjp.jdbc.caching;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.SQLServerDataSourceProvider;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@RunWith(Parameterized.class)
public class SQLServerSelectMethodCursorTest extends AbstractTest {

    public static final String SELECT_POST = "SELECT id, title FROM post";

    private String selectMethod;

    public SQLServerSelectMethodCursorTest(String selectMethod) {
        this.selectMethod = selectMethod;
    }

    @Parameterized.Parameters
    public static Collection<String[]> rdbmsDataSourceProvider() {
        List<String[]> providers = new ArrayList<>();
        providers.add(new String[]{"direct"});
        providers.add(new String[]{"cursor"});
        return providers;
    }

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new SQLServerDataSourceProvider() {
            @Override
            public DataSource newDataSource() {
                SQLServerDataSource dataSource = (SQLServerDataSource) super.newDataSource();
                dataSource.setSelectMethod(selectMethod);
                return dataSource;
            }
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            for (long i = 1; i <= 100; i++) {
                entityManager.persist(
                    new Post()
                        .setId(i)
                        .setTitle(String.format("High-Performance Java Persistence, part %d", i))
                );
            }
        });
        doInJDBC(connection -> {
            try(PreparedStatement statement = connection.prepareStatement(SELECT_POST);
                ResultSet resultSet = statement.executeQuery()) {

                assertEquals(128, statement.getFetchSize());

                short sessionId = selectColumn(connection, "select @@SPID", Short.class);
                executeSync(() -> {
                    doInJDBC(_connection -> {
                        List<Map<String, Object>> cursors = selectColumnMap(_connection, String.format("""
                            select
                                s.session_id, s.host_name, s.program_name,
                                s.client_interface_name, s.login_name,
                                c.cursor_id, c.properties, c.creation_time, c.is_open, con.text,
                                l.resource_type, d.name, l.request_type,
                                l.request_Status, l.request_reference_count,
                                l.request_lifetime, l.request_owner_type
                            from sys.dm_exec_cursors(0) c
                            left outer join (
                                select *
                                from sys.dm_exec_connections c
                                cross apply sys.dm_exec_sql_text(c.most_recent_sql_handle) mr
                            ) con on c.session_id = con.session_id
                            left outer join sys.dm_exec_sessions s on s.session_id = c.session_id
                            left outer join sys.dm_tran_locks l on l.request_session_id = c.session_id
                            left outer join sys.databases d on d.database_id = l.resource_database_id
                            where c.session_id = %d
                        """, sessionId));

                        if("cursor".equals(selectMethod)) {
                            assertEquals(1, cursors.size());

                            Map<String, Object> columnValues = cursors.get(0);
                            LOGGER.info(
                                "Current open cursor id: {} for statement: {}",
                                columnValues.get("cursor_id"),
                                columnValues.get("text")
                            );
                        } else {
                            assertEquals(0, cursors.size());
                        }

                        List<Map<String, Object>> cursorMemory = selectColumnMap(_connection, """
                            SELECT *
                            FROM sys.dm_os_performance_counters
                            WHERE
                                counter_name = 'Cursor memory usage' and
                                instance_name = 'API Cursor'
                            """);

                        assertEquals(1, cursorMemory.size());

                        Map<String, Object> columnValues = cursorMemory.get(0);
                        long memorySize = (long) columnValues.get("cntr_value");
                        LOGGER.info(
                            "Current memory value: {}",
                            memorySize
                        );
                    });
                });
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        public Long getId() {
            return id;
        }

        public Post setId(Long id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Post setTitle(String title) {
            this.title = title;
            return this;
        }
    }
}
