package com.vladmihalcea.book.hpjp.hibernate.identifier.composite;

import java.io.Serializable;
import java.sql.Statement;
import java.util.Objects;
import java.util.Properties;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;

import org.hibernate.Session;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractSQLServerIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractTest;

import static org.junit.Assert.assertEquals;

public class CompositeIdGeneratedIdClassTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Book.class
        };
    }

    @Test
    public void test() {
        LOGGER.debug("test");

        Book _book = doInJPA(entityManager -> {

            Book book = new Book();
            book.setGroupNo( 1 );
            book.setBookName( "High-Performance Java Persistence");

            entityManager.persist(book);

            return book;
        });
        doInJPA(entityManager -> {
            PK key = new PK(_book.getRowId(), 1);

            Book book = entityManager.find(Book.class, key);
            assertEquals( "High-Performance Java Persistence", book.getBookName() );
        });

    }

    @Entity(name = "BOOK_EMBEDDED")
    @IdClass( PK.class )
    public static class Book {

        @Id
        @Column(name = "row_id")
        @GeneratedValue
        private Long rowId;

        @Id
        @Column(name = "group_no")
        private int groupNo;

        @Column(name = "BOOK_NAME")
        private String bookName;

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

        public String getBookName() {
            return bookName;
        }

        public void setBookName(String bookName) {
            this.bookName = bookName;
        }
    }

    public static class PK implements Serializable {

        private Long rowId;

        private int groupNo;

        public PK(Long rowId, int groupNo) {
            this.rowId = rowId;
            this.groupNo = groupNo;
        }

        private PK() {
        }

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
            PK pk = (PK) o;
            return groupNo == pk.groupNo &&
                    Objects.equals( rowId, pk.rowId );
        }

        @Override
        public int hashCode() {
            return Objects.hash( rowId, groupNo );
        }
    }

}
