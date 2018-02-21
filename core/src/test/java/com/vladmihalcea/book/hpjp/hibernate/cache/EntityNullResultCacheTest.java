package com.vladmihalcea.book.hpjp.hibernate.cache;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.transaction.VoidCallable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.QueryHints;
import org.hibernate.cache.internal.StandardQueryCache;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Vlad Mihalcea
 */
public class EntityNullResultCacheTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Book.class
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");
        properties.put("hibernate.generate_statistics", Boolean.TRUE.toString());
        properties.put("hibernate.cache.use_query_cache", Boolean.TRUE.toString());
        return properties;
    }

    @Test
    public void testFindExistingEntity() {

        doInJPA(entityManager -> {
            Book book = new Book();
            book.setIsbn( "978-9730228236" );
            book.setTitle( "High-Performance Java Persistence" );
            book.setAuthor( "Vlad Mihalcea" );

            entityManager.persist(book);
        });

        doInJPA(entityManager -> {
            entityManager.getEntityManagerFactory().getCache().evictAll();
            printCacheRegionStatistics(Book.class.getName());

            Book book = entityManager.find(Book.class, "978-9730228236");
            assertEquals("Vlad Mihalcea", book.getAuthor());

            printCacheRegionStatistics(Book.class.getName());

            executeSync(() -> {
                doInJPA(_entityManager -> {
                    Book _book = _entityManager.find(Book.class, "978-9730228236");

                    assertEquals("High-Performance Java Persistence", _book.getTitle());
                });
            });
        });
    }

    @Test
    public void testFindNonExistingEntity() {

        doInJPA(entityManager -> {
            printCacheRegionStatistics(Book.class.getName());

            Book book = entityManager.find(Book.class, "978-9730456472");
            assertNull(book);

            printCacheRegionStatistics(Book.class.getName());

            executeSync(() -> {
                doInJPA(_entityManager -> {
                    Book _book = _entityManager.find(Book.class, "978-9730456472");

                    assertNull(_book);
                });
            });
        });
    }

    @Test
    public void testFindNonExistingEntityWithQuery() {

        doInJPA(entityManager -> {
            printQueryCacheRegionStatistics();

            try {
                Book book = entityManager.createQuery(
                    "select b " +
                    "from Book b " +
                    "where b.isbn = :isbn", Book.class)
                .setParameter("isbn", "978-9730456472")
                .setHint(QueryHints.CACHEABLE, true)
                .getSingleResult();
            } catch (NoResultException expected) {
            }

            printQueryCacheRegionStatistics();

            executeSync(() -> {
                doInJPA(_entityManager -> {
                    try {
                        Book _book = _entityManager.createQuery(
                            "select b " +
                            "from Book b " +
                            "where b.isbn = :isbn", Book.class)
                        .setParameter("isbn", "978-9730456472")
                        .setHint(QueryHints.CACHEABLE, true)
                        .getSingleResult();
                    } catch (NoResultException expected) {
                    }
                });
            });
        });
    }

    @Entity(name = "Book")
    @Table(name = "book")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class Book {

        @Id
        private String isbn;

        private String title;

        private String author;

        public String getIsbn() {
            return isbn;
        }

        public void setIsbn(String isbn) {
            this.isbn = isbn;
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
    }
}
