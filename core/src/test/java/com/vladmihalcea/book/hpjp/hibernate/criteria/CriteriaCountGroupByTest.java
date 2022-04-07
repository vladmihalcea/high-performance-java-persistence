package com.vladmihalcea.book.hpjp.hibernate.criteria;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class CriteriaCountGroupByTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Book.class
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Book book1 = new Book();
            book1.setId(1L);
            book1.setName("Java");

            Book book2 = new Book();
            book2.setId(2L);
            book2.setName("Java");

            Book book3 = new Book();
            book3.setId(3L);
            book3.setName("Hibernate");

            entityManager.persist(book1);
            entityManager.persist(book2);
            entityManager.persist(book3);
        });

        doInJPA(entityManager -> {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();

            CriteriaQuery<Tuple> cq = cb.createTupleQuery();
            Root<Book> book = cq.from(Book.class);
            cq.groupBy(book.get("name"));
            cq.multiselect(book.get("name"), cb.count(book));

            List<Tuple> tupleResult = entityManager.createQuery(cq).getResultList();
            assertEquals("Java", tupleResult.get(0).get(0));
            assertEquals(2L, tupleResult.get(0).get(1));

            assertEquals("Hibernate", tupleResult.get(1).get(0));
            assertEquals(1L, tupleResult.get(1).get(1));
        });
    }

    @Entity(name = "Book")
    @Table(name = "book")
    public static class Book {

        @Id
        private Long id;

        private String name;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
