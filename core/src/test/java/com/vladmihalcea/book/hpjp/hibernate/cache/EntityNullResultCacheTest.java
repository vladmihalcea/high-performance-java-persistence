package com.vladmihalcea.book.hpjp.hibernate.cache;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.QueryHints;
import org.junit.Test;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
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
        properties.put("hibernate.cache.region.factory_class", "jcache");
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

    @Test
    public void testFindNonExistingEntityWithCriteria() {

        try {
            Book book = getCacheableEntity(Book.class, "isbn", "978-9730456472");
        } catch (NoResultException expected) {
        }

        printQueryCacheRegionStatistics();

        executeSync(() -> {
            try {
                Book _book = getCacheableEntity(Book.class, "isbn", "978-9730456472");
            } catch (NoResultException expected) {
            }
        });
    }

    public <T> T getCacheableEntity(
                Class<T> entityClass,
                String identifierName,
                Object identifierValue) {
        return doInJPA(entityManager -> {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<T> criteria = builder.createQuery(entityClass);
            Root<T> fromClause = criteria.from(entityClass);

            criteria.where(builder.equal(fromClause.get(identifierName), identifierValue));

            return entityManager
                    .createQuery(criteria)
                    .setHint(QueryHints.CACHEABLE, true)
                    .getSingleResult();
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
