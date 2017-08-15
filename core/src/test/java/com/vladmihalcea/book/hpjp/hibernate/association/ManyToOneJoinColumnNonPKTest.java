package com.vladmihalcea.book.hpjp.hibernate.association;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.NaturalId;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractTest;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class ManyToOneJoinColumnNonPKTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Book.class,
            Publication.class,
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Book book = new Book();
            book.setTitle( "High-Performance Java Persistence" );
            book.setAuthor( "Vlad Mihalcea" );
            book.setIsbn( "978-9730228236" );
            entityManager.persist(book);

            Publication amazonUs = new Publication();
            amazonUs.setPublisher( "amazon.com" );
            amazonUs.setBook( book );
            amazonUs.setPriceCents( 4599 );
            amazonUs.setCurrency( "$" );
            entityManager.persist( amazonUs );

            Publication amazonUk = new Publication();
            amazonUk.setPublisher( "amazon.co.uk" );
            amazonUk.setBook( book );
            amazonUk.setPriceCents( 3545 );
            amazonUk.setCurrency( "&" );
            entityManager.persist( amazonUk );
        });
        doInJPA(entityManager -> {
            Publication publication = entityManager.createQuery(
                "select p " +
                "from Publication p " +
                "join fetch p.book b " +
                "where " +
                "   b.isbn = :isbn and " +
                "   p.currency = :currency", Publication.class)
            .setParameter( "isbn", "978-9730228236" )
            .setParameter( "currency", "&" )
            .getSingleResult();

            assertEquals(
                "amazon.co.uk",
                publication.getPublisher()
            );

            assertEquals(
                "High-Performance Java Persistence",
                publication.getBook().getTitle()
            );
        });
    }

    @Entity(name = "Book")
    @Table(name = "book")
    public static class Book implements Serializable {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        private String author;

        @NaturalId
        private String isbn;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getIsbn() {
            return isbn;
        }

        public void setIsbn(String isbn) {
            this.isbn = isbn;
        }
    }

    @Entity(name = "Publication")
    @Table(name = "publication")
    public static class Publication {

        @Id
        @GeneratedValue
        private Long id;

        private String publisher;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "isbn", referencedColumnName = "isbn")
        private Book book;

        @Column(name = "price_in_cents", nullable = false)
        private Integer priceCents;

        private String currency;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getPublisher() {
            return publisher;
        }

        public void setPublisher(String publisher) {
            this.publisher = publisher;
        }

        public Integer getPriceCents() {
            return priceCents;
        }

        public void setPriceCents(Integer priceCents) {
            this.priceCents = priceCents;
        }

        public Book getBook() {
            return book;
        }

        public void setBook(Book book) {
            this.book = book;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }
}
