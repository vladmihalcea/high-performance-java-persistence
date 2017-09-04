package com.vladmihalcea.book.hpjp.hibernate.identifier.composite;

import java.io.Serializable;
import java.sql.Statement;
import java.util.Objects;
import java.util.Properties;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Version;

import org.hibernate.Session;
import org.hibernate.annotations.SQLInsert;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractSQLServerIntegrationTest;

import static org.junit.Assert.assertEquals;

public class CompositeIdIdentityGeneratedTest extends AbstractSQLServerIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Book.class
        };
    }

    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.hbm2ddl.auto", "none");
    }

    @Test
    public void test() {
        LOGGER.debug("test");

        doInJPA(entityManager -> {
            Session session = entityManager.unwrap( Session.class );

            session.doWork( connection -> {
				try(Statement statement = connection.createStatement()) {
				    statement.executeUpdate( "drop table BOOK_EMBEDDED" );
                }
                catch (Exception ignore) {}

                try(Statement statement = connection.createStatement()) {
                    statement.executeUpdate( "create table BOOK_EMBEDDED (group_no int not null, row_id bigint IDENTITY(1,1) not null, BOOK_NAME varchar(255), version int, primary key (group_no, row_id))" );
                }
                catch (Exception ignore) {}
			} );


            EmbeddedKey key = new EmbeddedKey();
            key.setGroupNo(1);

            Book book = new Book();
            book.setBookName( "High-Performance Java Persistence");

            book.setKey(key);

            entityManager.persist(book);
        });
        doInJPA(entityManager -> {
            EmbeddedKey key = new EmbeddedKey();

            key.setGroupNo(1);
            key.setRowId(1L);

            Book book = entityManager.find(Book.class, key);
            assertEquals( "High-Performance Java Persistence", book.getBookName() );
        });

    }

    @Entity(name = "BOOK_EMBEDDED")
    @SQLInsert( sql = "insert into BOOK_EMBEDDED (BOOK_NAME, group_no, version) values (?, ?, ?)")
    public static class Book implements Serializable {

        @EmbeddedId
        private EmbeddedKey key;

        @Column(name = "BOOK_NAME")
        private String bookName;

        @Version
        @Column(insertable = false)
        private Integer version;

        public EmbeddedKey getKey() {
            return key;
        }

        public void setKey(EmbeddedKey key) {
            this.key = key;
        }

        public String getBookName() {
            return bookName;
        }

        public void setBookName(String bookName) {
            this.bookName = bookName;
        }
    }

    @Embeddable
    public static class EmbeddedKey implements Serializable {

        @Column(name = "row_id", insertable = false, updatable = false)
        private Long rowId;

        @Column(name = "group_no")
        private int groupNo;

        public Long getRowId() {
            return rowId;
        }

        public void setRowId(Long rowId) {
            this.rowId = rowId;
        }

        public int getGroupNo() {
            return groupNo;
        }

        public void setGroupNo(int groupNo) {
            this.groupNo = groupNo;
        }

        @Override
        public boolean equals(Object o) {
            if ( this == o ) {
                return true;
            }
            if ( o == null || getClass() != o.getClass() ) {
                return false;
            }
            EmbeddedKey that = (EmbeddedKey) o;
            return groupNo == that.groupNo &&
                    Objects.equals( rowId, that.rowId );
        }

        @Override
        public int hashCode() {
            return Objects.hash( rowId, groupNo );
        }
    }

}
