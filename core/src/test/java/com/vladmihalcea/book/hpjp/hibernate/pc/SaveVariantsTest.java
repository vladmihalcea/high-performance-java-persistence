package com.vladmihalcea.book.hpjp.hibernate.pc;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.Assert.assertFalse;

public class SaveVariantsTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Book.class
        };
    }

    @Test
    public void testPersist() {
        doInJPA(entityManager -> {
            Book book = new Book()
            .setIsbn("978-9730228236")
            .setTitle("High-Performance Java Persistence")
            .setAuthor("Vlad Mihalcea");

            entityManager.persist(book);

            LOGGER.info("Persisting the Book entity with the id: {}", book.getId());
        });
    }

    @Test
    public void testSave() {
        doInJPA(entityManager -> {
            Book book = new Book()
            .setIsbn("978-9730228236")
            .setTitle("High-Performance Java Persistence")
            .setAuthor("Vlad Mihalcea");

            Session session = entityManager.unwrap(Session.class);

            Long id = (Long) session.save(book);

            LOGGER.info("Saving the Book entity with the id: {}", id);
        });
    }

    @Test
    public void testUpdate() {
        Book _book = doInJPA(entityManager -> {
            Book book = new Book()
            .setIsbn("978-9730228236")
            .setTitle("High-Performance Java Persistence")
            .setAuthor("Vlad Mihalcea");

            entityManager.persist(book);

            return book;
        });

        LOGGER.info("Modifying the Book entity");

        _book.setTitle("High-Performance Java Persistence, 2nd edition");

        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);

            session.update(_book);

            LOGGER.info("Updating the Book entity");
        });
    }

    @Test
    public void testUpdateWithoutModification() {
        Book _book = doInJPA(entityManager -> {
            Book book = new Book()
            .setIsbn("978-9730228236")
            .setTitle("High-Performance Java Persistence")
            .setAuthor("Vlad Mihalcea");

            entityManager.persist(book);

            return book;
        });

        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);

            session.update(_book);

            LOGGER.info("Updating the Book entity");
        });
    }

    @Test(expected = NonUniqueObjectException.class)
    public void testUpdateFailAlreadyManaged() {
        Book _book = doInJPA(entityManager -> {
            Book book = new Book()
            .setIsbn("978-9730228236")
            .setTitle("High-Performance Java Persistence")
            .setAuthor("Vlad Mihalcea");

            entityManager.persist(book);

            return book;
        });

        _book.setTitle("High-Performance Java Persistence, 2nd edition");

        doInJPA(entityManager -> {
            Book book = entityManager.find(Book.class, _book.getId());

            Session session = entityManager.unwrap(Session.class);

            session.update(_book);
        });
    }

    @Test
    public void testSaveUpdate() {
        Book _book = doInJPA(entityManager -> {
            Book book = new Book()
            .setIsbn("978-9730228236")
            .setTitle("High-Performance Java Persistence")
            .setAuthor("Vlad Mihalcea");

            Session session = entityManager.unwrap(Session.class);
            session.saveOrUpdate(book);

            return book;
        });

        _book.setTitle("High-Performance Java Persistence, 2nd edition");

        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            session.saveOrUpdate(_book);
        });
    }

    @Test
    public void testSaveOrUpdateFailAlreadyManaged() {
        Book _book = doInJPA(entityManager -> {
            Book book = new Book()
            .setIsbn("978-9730228236")
            .setTitle("High-Performance Java Persistence")
            .setAuthor("Vlad Mihalcea");

            Session session = entityManager.unwrap(Session.class);
            session.saveOrUpdate(book);

            return book;
        });

        _book.setTitle("High-Performance Java Persistence, 2nd edition");

        try {
            doInJPA(entityManager -> {
                Book book = entityManager.find(Book.class, _book.getId());

                Session session = entityManager.unwrap(Session.class);
                session.saveOrUpdate(_book);
            });
        } catch (NonUniqueObjectException e) {
            LOGGER.error(
                    "The Persistence Context cannot hold " +
                    "two representations of the same entity",
                    e
            );
        }
    }

    @Test
    public void testMerge() {
        Book _book = doInJPA(entityManager -> {
            Book book = new Book()
            .setIsbn("978-9730228236")
            .setTitle("High-Performance Java Persistence")
            .setAuthor("Vlad Mihalcea");

            entityManager.persist(book);

            return book;
        });

        LOGGER.info("Modifying the Book entity");

        _book.setTitle("High-Performance Java Persistence, 2nd edition");

        doInJPA(entityManager -> {
            Book book = entityManager.merge(_book);

            LOGGER.info("Merging the Book entity");

            assertFalse(book == _book);
        });
    }

    @Test
    public void testMergeAlreadyManaged() {
        Book _book = doInJPA(entityManager -> {
            Book book = new Book()
            .setIsbn("978-9730228236")
            .setTitle("High-Performance Java Persistence")
            .setAuthor("Vlad Mihalcea");

            entityManager.persist(book);

            return book;
        });

        _book.setTitle("High-Performance Java Persistence, 2nd edition");

        doInJPA(entityManager -> {
            Book book = entityManager.find(Book.class, _book.getId());

            entityManager.merge(_book);
        });
    }

    @Entity(name = "Book")
    @Table(name = "book")
    public static class Book {

        @Id
        @GeneratedValue
        private Long id;

        private String isbn;

        private String title;

        private String author;

        public Long getId() {
            return id;
        }

        public Book setId(Long id) {
            this.id = id;
            return this;
        }

        public String getIsbn() {
            return isbn;
        }

        public Book setIsbn(String isbn) {
            this.isbn = isbn;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Book setTitle(String title) {
            this.title = title;
            return this;
        }

        public String getAuthor() {
            return author;
        }

        public Book setAuthor(String author) {
            this.author = author;
            return this;
        }
    }
}
