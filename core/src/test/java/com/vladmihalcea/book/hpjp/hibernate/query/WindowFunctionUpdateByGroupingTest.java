package com.vladmihalcea.book.hpjp.hibernate.query;

import java.sql.Statement;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class WindowFunctionUpdateByGroupingTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Entry.class,
        };
    }

    @Override
    public void init() {
        super.init();

        doInJPA(entityManager -> {
            Session session = entityManager.unwrap( Session.class );

            session.doWork( connection -> {
                try(Statement statement = connection.createStatement() ) {
                    statement.executeUpdate( "INSERT INTO entries (c1, c2, c3, c4, c5) VALUES (2000, 'a', 1, 'x', 0)" );
                    statement.executeUpdate( "INSERT INTO entries (c1, c2, c3, c4, c5) VALUES (2000, 'a', 1, 'y', 0) " );
                    statement.executeUpdate( "INSERT INTO entries (c1, c2, c3, c4, c5) VALUES (2000, 'a', 1, 'z', 0) " );
                    statement.executeUpdate( "INSERT INTO entries (c1, c2, c3, c4, c5) VALUES (2000, 'a', 2, 'z', 0) " );
                    statement.executeUpdate( "INSERT INTO entries (c1, c2, c3, c4, c5) VALUES (2000, 'a', 2, 'x', 0) " );
                    statement.executeUpdate( "INSERT INTO entries (c1, c2, c3, c4, c5) VALUES (2000, 'b', 1, 'x', 0) " );
                    statement.executeUpdate( "INSERT INTO entries (c1, c2, c3, c4, c5) VALUES (2000, 'b', 1, 'y', 0) " );
                    statement.executeUpdate( "INSERT INTO entries (c1, c2, c3, c4, c5) VALUES (2000, 'b', 1, 'z', 0) " );
                    statement.executeUpdate( "INSERT INTO entries (c1, c2, c3, c4, c5) VALUES (2000, 'b', 2, 'z', 0) " );
                    statement.executeUpdate( "INSERT INTO entries (c1, c2, c3, c4, c5) VALUES (2001, 'a', 1, 'x', 0) " );
                    statement.executeUpdate( "INSERT INTO entries (c1, c2, c3, c4, c5) VALUES (2001, 'a', 1, 'y', 0) " );
                    statement.executeUpdate( "INSERT INTO entries (c1, c2, c3, c4, c5) VALUES (2001, 'a', 1, 'z', 0) " );
                    statement.executeUpdate( "INSERT INTO entries (c1, c2, c3, c4, c5) VALUES (2001, 'a', 2, 'z', 0) " );
                    statement.executeUpdate( "INSERT INTO entries (c1, c2, c3, c4, c5) VALUES (2001, 'a', 2, 'x', 0) " );
                    statement.executeUpdate( "INSERT INTO entries (c1, c2, c3, c4, c5) VALUES (2001, 'a', 2, 'y', 0) " );
                    statement.executeUpdate( "INSERT INTO entries (c1, c2, c3, c4, c5) VALUES (2001, 'a', 2, 'w', 0) " );
                    statement.executeUpdate( "INSERT INTO entries (c1, c2, c3, c4, c5) VALUES (2001, 'a', 3, 'y', 0) " );
                    statement.executeUpdate( "INSERT INTO entries (c1, c2, c3, c4, c5) VALUES (2001, 'a', 3, 'w', 0) " );
                    statement.executeUpdate( "INSERT INTO entries (c1, c2, c3, c4, c5) VALUES (2001, 'b', 1, 'x', 0) " );
                    statement.executeUpdate( "INSERT INTO entries (c1, c2, c3, c4, c5) VALUES (2001, 'b', 1, 'y', 0) " );
                    statement.executeUpdate( "INSERT INTO entries (c1, c2, c3, c4, c5) VALUES (2001, 'b', 2, 'x', 0) " );
                    statement.executeUpdate( "INSERT INTO entries (c1, c2, c3, c4, c5) VALUES (2001, 'b', 2, 'z', 0) " );
                }
            } );
        });
    }

    @Test
    public void testWindowFunction() {
        doInJPA(entityManager -> {
            List values = entityManager.createQuery( "select e from Entry e" ).getResultList();
            assertEquals(22, values.size());

            int updateCount = entityManager.createNativeQuery(
                "update entries set c5 = 1 " +
                "where id in " +
                "( " +
                "    select id " +
                "    from ( " +
                "        select *, MAX (c3) OVER (PARTITION BY c1, c2) as max_c3 " +
                "        from entries " +
                "    ) t " +
                "    where t.c3 = t.max_c3 " +
                ") ")
            .executeUpdate();

            assertEquals( 7, updateCount );
        });
    }

    @Test
    public void testGroupBy() {
        doInJPA(entityManager -> {
            List values = entityManager.createQuery( "select e from Entry e" ).getResultList();
            assertEquals(22, values.size());

            int updateCount = entityManager.createNativeQuery(
                "update entries set c5 = 1 " +
                "where id in " +
                "( " +
                "    select e.id " +
                "    from entries e  " +
                "    inner join ( " +
                "        select c1, c2, max(c3) as max_c3 " +
                "        from entries " +
                "        group by c1, c2 " +
                "    ) t " +
                "    on e.c1 = t.c1 and e.c2 = t.c2 and e.c3 = t.max_c3  " +
                ") " )
            .executeUpdate();

            assertEquals( 7, updateCount );
        });
    }

    @Entity(name = "Entry")
    @Table(name = "entries")
    public static class Entry  {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private Integer c1;

        private String c2;

        private Integer c3;

        private String c4;

        private Integer c5;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Integer getC1() {
            return c1;
        }

        public void setC1(Integer c1) {
            this.c1 = c1;
        }

        public String getC2() {
            return c2;
        }

        public void setC2(String c2) {
            this.c2 = c2;
        }

        public Integer getC3() {
            return c3;
        }

        public void setC3(Integer c3) {
            this.c3 = c3;
        }

        public String getC4() {
            return c4;
        }

        public void setC4(String c4) {
            this.c4 = c4;
        }

        public Integer getC5() {
            return c5;
        }

        public void setC5(Integer c5) {
            this.c5 = c5;
        }
    }
}
