package com.vladmihalcea.book.hpjp.jooq.mysql.batch;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.Test;

import java.util.Properties;

import static com.vladmihalcea.book.hpjp.jooq.mysql.schema.Tables.POST;


/**
 * @author Vlad Mihalcea
 */
public class BatchTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.setProperty("hibernate.hbm2ddl.import_files", "mysql/initial_schema.sql");
        return properties;
    }

    @Test
    public void testLocalDateEvent() {
        doInJDBC(connection -> {
            DSLContext create = DSL.using(connection, SQLDialect.MYSQL);
            try {
                Result<Record> result = create.select().from(POST).fetch();
            } catch (org.jooq.exception.DataAccessException e) {
                e.printStackTrace();
            }
        });
    }
}
