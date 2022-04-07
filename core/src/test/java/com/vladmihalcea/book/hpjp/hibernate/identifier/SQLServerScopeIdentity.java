package com.vladmihalcea.book.hpjp.hibernate.identifier;

import com.vladmihalcea.book.hpjp.util.AbstractSQLServerIntegrationTest;
import org.hibernate.Session;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class SQLServerScopeIdentity extends AbstractSQLServerIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    @Test
    public void testScopeIdentity() {
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            final AtomicLong resultHolder = new AtomicLong();
            session.doWork(connection -> {
                try(PreparedStatement statement = connection.prepareStatement("INSERT INTO post VALUES (?) select scope_identity() ") ) {
                    statement.setString(1, "abc");
                    if ( !statement.execute() ) {
                        while ( !statement.getMoreResults() && statement.getUpdateCount() != -1 ) {
                            // do nothing until we hit the resultset
                        }
                    }
                    try (ResultSet rs = statement.getResultSet()) {
                        if(rs.next()) {
                            resultHolder.set(rs.getLong(1));
                        }
                    }
                }
            });
            assertNotNull(resultHolder.get());
        });
    }

    @Entity(name = "Post")
    public static class Post {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String name;
    }
}
