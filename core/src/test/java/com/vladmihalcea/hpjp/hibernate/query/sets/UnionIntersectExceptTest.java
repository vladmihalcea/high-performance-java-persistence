package com.vladmihalcea.hpjp.hibernate.query.sets;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.NaturalId;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class UnionIntersectExceptTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Tag.class,
            Category.class,
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    private List<String> categories = List.of(
        "Java",
        "JPA",
        "jOOQ",
        "Spring"
    );

    private List<String> tags = List.of(
        "Hibernate",
        "JDBC",
        "JPA",
        "jOOQ",
        "Spring"
    );

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            for (String category : categories) {
                entityManager.persist(new Category().setName(category));
            }
            for (String tag : tags) {
                entityManager.persist(new Tag().setName(tag));
            }
        });
    }

    @Test
    public void testUnionAll() {
        List<String> _topics = doInJPA(entityManager -> {
            List<String> topics = entityManager.createQuery("""
                select c.name as name
                from Category c
                union all
                select t.name as name
                from Tag t
                """, String.class)
            .getResultList();

            assertEquals(9, topics.size());

            return topics;
        });

        List<String> topics = Stream
            .concat(categories.stream(), tags.stream())
            .toList();

        Collections.sort(_topics, String::compareToIgnoreCase);
        topics = new ArrayList<>(topics);
        Collections.sort(topics, String::compareToIgnoreCase);

        assertEquals(_topics, topics);
    }

    @Test
    public void testUnion() {
        List<String> _topics = doInJPA(entityManager -> {
            List<String> topics = entityManager.createQuery("""
                select c.name as name
                from Category c
                union
                select t.name as name
                from Tag t
                """, String.class)
            .getResultList();

            assertEquals(6, topics.size());

            return topics;
        });

        List<String> topics = Stream
            .concat(categories.stream(), tags.stream())
            .distinct()
            .toList();

        Collections.sort(_topics, String::compareToIgnoreCase);
        topics = new ArrayList<>(topics);
        Collections.sort(topics, String::compareToIgnoreCase);

        assertEquals(_topics, topics);
    }

    @Test
    public void testIntersect() {
        List<String> _topics = doInJPA(entityManager -> {
            List<String> topics = entityManager.createQuery("""
                select c.name as name
                from Category c
                intersect
                select t.name as name
                from Tag t
                """, String.class)
            .getResultList();

            assertEquals(3, topics.size());

            return topics;
        });

        List<String> topics = categories
            .stream()
            .filter(tags::contains)
            .distinct()
            .toList();

        Collections.sort(_topics, String::compareToIgnoreCase);
        topics = new ArrayList<>(topics);
        Collections.sort(topics, String::compareToIgnoreCase);

        assertEquals(_topics, topics);
    }

    @Test
    public void testExcept() {
        List<String> _topics = doInJPA(entityManager -> {
            List<String> topics = entityManager.createQuery("""
                select c.name as name
                from Category c
                except
                select t.name as name
                from Tag t
                """, String.class)
                .getResultList();

            assertEquals(1, topics.size());

            return topics;
        });

        List<String> topics = categories
            .stream()
            .filter(Predicate.not(tags::contains))
            .distinct()
            .toList();

        Collections.sort(_topics, String::compareToIgnoreCase);
        topics = new ArrayList<>(topics);
        Collections.sort(topics, String::compareToIgnoreCase);

        assertEquals(_topics, topics);
    }

    @Entity(name = "Tag")
    @Table(name = "tag")
    public static class Tag {

        @Id
        @GeneratedValue
        private Long id;

        @NaturalId
        private String name;

        public Long getId() {
            return id;
        }

        public Tag setId(Long id) {
            this.id = id;
            return this;
        }

        public String getName() {
            return name;
        }

        public Tag setName(String name) {
            this.name = name;
            return this;
        }
    }

    @Entity(name = "Category")
    @Table(name = "category")
    public static class Category {

        @Id
        @GeneratedValue
        private Long id;

        @NaturalId
        private String name;

        public Long getId() {
            return id;
        }

        public Category setId(Long id) {
            this.id = id;
            return this;
        }

        public String getName() {
            return name;
        }

        public Category setName(String name) {
            this.name = name;
            return this;
        }
    }
}
