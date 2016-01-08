package com.vladmihalcea.guide.mapping;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.junit.Test;

import javax.persistence.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * <code>ColumnTransformerTest</code> -
 *
 * @author Vlad Mihalcea
 */
public class ColumnTransformerTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                DateEvent.class
        };
    }

    @Override
    public void init() {
        try(Connection c = newDataSource().getConnection()) {
            try(Statement st = c.createStatement()) {
                st.executeUpdate("drop sequence subscription_code_1_seq");
                st.executeUpdate("create sequence subscription_code_1_seq start 1 increment 7");
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        }
        super.init();
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            DateEvent dateEvent = new DateEvent();
            entityManager.persist(dateEvent);
        });
        doInJPA(entityManager -> {
            DateEvent dateEvent = entityManager.createQuery("from DateEvent", DateEvent.class).getSingleResult();
            assertNotNull(dateEvent.code);
        });
    }

    @Entity(name = "DateEvent")
    public static class DateEvent  {

        @Id
        @SequenceGenerator(
                name="subscription_id_seq",
                sequenceName="subscription_id_seq",
                allocationSize=7
        )
        @GeneratedValue(
                strategy=GenerationType.SEQUENCE,
                generator="subscription_id_seq"
        )
        @Column(unique=true, nullable=false)
        private Integer id;

        @Column(
                name="code",
                nullable=false,
                unique=true,
                insertable = false,
                updatable = false,
                columnDefinition = "BIGINT DEFAULT nextval('subscription_code_1_seq')"
        )
        @Generated(GenerationTime.INSERT)
        private Integer code;
    }
}
