package com.vladmihalcea.book.hpjp.hibernate.identifier.composite;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;

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
            book.setPublisherId( 1 );
            book.setTitle( "High-Performance Java Persistence");

            entityManager.persist(book);

            return book;
        });

        doInJPA(entityManager -> {
            PK key = new PK( _book.getRegistrationNumber(), 1);

            Book book = entityManager.find(Book.class, key);
            assertEquals( "High-Performance Java Persistence", book.getTitle() );
        });
    }

    @Entity(name = "Book")
    @Table(name = "book")
    @IdClass( PK.class )
    public static class Book {

        @Id
        @Column(name = "registration_number")
        @GeneratedValue
        private Long registrationNumber;

        @Id
        @Column(name = "publisher_id")
        private Integer publisherId;

        private String title;

        public Long getRegistrationNumber() {
            return registrationNumber;
        }

        public void setRegistrationNumber(Long registrationNumber) {
            this.registrationNumber = registrationNumber;
        }

        public int getPublisherId() {
            return publisherId;
        }

        public void setPublisherId(int publisherId) {
            this.publisherId = publisherId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    public static class PK implements Serializable {

        private Long registrationNumber;

        private Integer publisherId;

        public PK(Long registrationNumber, Integer publisherId) {
            this.registrationNumber = registrationNumber;
            this.publisherId = publisherId;
        }

        private PK() {
        }

        public Long getRegistrationNumber() {
            return registrationNumber;
        }

        public void setRegistrationNumber(Long registrationNumber) {
            this.registrationNumber = registrationNumber;
        }

        public Integer getPublisherId() {
            return publisherId;
        }

        public void setPublisherId(Integer publisherId) {
            this.publisherId = publisherId;
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
            return Objects.equals( registrationNumber, pk.registrationNumber ) &&
                    Objects.equals( publisherId, pk.publisherId );
        }

        @Override
        public int hashCode() {
            return Objects.hash( registrationNumber, publisherId );
        }
    }

}
