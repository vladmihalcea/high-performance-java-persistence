package com.vladmihalcea.book.hpjp.hibernate.identifier.composite;

import java.io.Serializable;
import java.sql.Statement;
import java.util.Objects;
import java.util.Properties;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
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
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate( "drop table book" );
                }
                catch (Exception ignore) {
                }

                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(
                            "create table book (publisher_id int not null, registration_number bigint IDENTITY not null, title varchar(255), version int, primary key (publisher_id, registration_number))" );
                }
                catch (Exception ignore) {
                }
            } );
        });

        doInJPA(entityManager -> {
            Book book = new Book();
            book.setTitle( "High-Performance Java Persistence");

            EmbeddedKey key = new EmbeddedKey();
            key.setPublisherId(1);
            book.setKey(key);

            entityManager.persist(book);
        });

        doInJPA(entityManager -> {
            EmbeddedKey key = new EmbeddedKey();

            key.setPublisherId(1);
            key.setRegistrationNumber(1L);

            Book book = entityManager.find(Book.class, key);
            assertEquals( "High-Performance Java Persistence", book.getTitle() );
        });

    }

    @Entity(name = "Book")
    @Table(name = "book")
    @SQLInsert( sql = "insert into book (title, publisher_id, version) values (?, ?, ?)")
    public static class Book implements Serializable {

        @EmbeddedId
        private EmbeddedKey key;

        private String title;

        @Version
        @Column(insertable = false)
        private Integer version;

        public EmbeddedKey getKey() {
            return key;
        }

        public void setKey(EmbeddedKey key) {
            this.key = key;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    @Embeddable
    public static class EmbeddedKey implements Serializable {

        @Column(name = "registration_number")
        private Long registrationNumber;

        @Column(name = "publisher_id")
        private Integer publisherId;

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

        @Override
        public boolean equals(Object o) {
            if ( this == o ) {
                return true;
            }
            if ( o == null || getClass() != o.getClass() ) {
                return false;
            }
            EmbeddedKey that = (EmbeddedKey) o;
            return Objects.equals( registrationNumber, that.registrationNumber ) &&
                    Objects.equals( publisherId, that.publisherId );
        }

        @Override
        public int hashCode() {
            return Objects.hash( registrationNumber, publisherId );
        }
    }

}
