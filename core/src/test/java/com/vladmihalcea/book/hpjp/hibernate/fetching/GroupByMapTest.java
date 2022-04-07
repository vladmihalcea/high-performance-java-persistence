package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.transform.ResultTransformer;
import org.junit.Test;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class GroupByMapTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }


    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence eBook has been released!")
                    .setCreatedOn(LocalDate.of(2016, 8, 30))
            );

            entityManager.persist(
                new Post()
                    .setId(2L)
                    .setTitle("High-Performance Java Persistence paperback has been released!")
                    .setCreatedOn(LocalDate.of(2016, 10, 12))
            );

            entityManager.persist(
                new Post()
                    .setId(3L)
                    .setTitle("High-Performance Java Persistence Mach 1 video course has been released!")
                    .setCreatedOn(LocalDate.of(2018, 1, 30))
            );

            entityManager.persist(
                new Post()
                    .setId(4L)
                    .setTitle("High-Performance Java Persistence Mach 2 video course has been released!")
                    .setCreatedOn(LocalDate.of(2018, 5, 8))
            );

            entityManager.persist(
                new Post()
                    .setId(5L)
                    .setTitle("Hypersistence Optimizer has been released!")
                    .setCreatedOn(LocalDate.of(2019, 3, 19))
            );
        });
    }

    @Test
    public void testGroupByStreamCollector() {
        doInJPA(entityManager -> {
            Map<Integer, Integer> postCountByYearMap = entityManager
            .createQuery(
                "select " +
                "   YEAR(p.createdOn) as year, " +
                "   count(p) as postCount " +
                "from Post p " +
                "group by " +
                "   YEAR(p.createdOn)", Tuple.class)
            .getResultStream()
            .collect(
                Collectors.toMap(
                    tuple -> ((Number) tuple.get("year")).intValue(),
                    tuple -> ((Number) tuple.get("postCount")).intValue()
                )
            );

            assertEquals(2, postCountByYearMap.get(2016).intValue());
            assertEquals(2, postCountByYearMap.get(2018).intValue());
            assertEquals(1, postCountByYearMap.get(2019).intValue());
        });
    }

    @Test
    public void testGroupByResultTransformer() {
        doInJPA(entityManager -> {
            Map<Number, Number> postCountByYearMap = (Map<Number, Number>) entityManager
            .createQuery(
                "select " +
                "   YEAR(p.createdOn) as year, " +
                "   count(p) as postCount " +
                "from Post p " +
                "group by " +
                "   YEAR(p.createdOn)")
            .unwrap(org.hibernate.query.Query.class)
            .setResultTransformer(
                new MapResultTransformer<Number, Number>()
            )
            .getSingleResult();

            assertEquals(2, postCountByYearMap.get(2016).intValue());
            assertEquals(2, postCountByYearMap.get(2018).intValue());
            assertEquals(1, postCountByYearMap.get(2019).intValue());
        });
    }

    @Test
    public void testGroupByIdStreamCollector() {
        doInJPA(entityManager -> {
            Map<Long, Post> postByIdMap = entityManager
            .createQuery(
                "select p " +
                "from Post p ", Post.class)
            .getResultStream()
            .collect(
                Collectors.toMap(
                    Post::getId,
                    Function.identity()
                )
            );

            assertEquals(
                "High-Performance Java Persistence eBook has been released!",
                postByIdMap.get(1L).getTitle()
            );

            assertEquals(
                "Hypersistence Optimizer has been released!",
                postByIdMap.get(5L).getTitle()
            );
       });
    }

    @Test
    public void testGroupByIdResultTransformer() {
        doInJPA(entityManager -> {
            Map<Long, Post> postByIdMap = (Map<Long, Post>) entityManager
            .createQuery(
                "select p " +
                "from Post p ")
            .unwrap(org.hibernate.query.Query.class)
            .setResultTransformer(
                new ResultTransformer() {

                    Map<Long, Post> result = new HashMap<>();

                    @Override
                    public Object transformTuple(Object[] tuple, String[] aliases) {
                        Post post = (Post) tuple[0];
                        result.put(
                            post.getId(),
                            post
                        );
                        return tuple;
                    }

                    @Override
                    public List transformList(List collection) {
                        return Collections.singletonList(result);
                    }
                }
            )
            .getSingleResult();

            assertEquals(
                "High-Performance Java Persistence eBook has been released!",
                postByIdMap.get(1L).getTitle()
            );

            assertEquals(
                "Hypersistence Optimizer has been released!",
                postByIdMap.get(5L).getTitle()
            );
        });
    }

    @FunctionalInterface
    public interface ListResultTransformer extends ResultTransformer {

        @Override
        default List transformList(List collection) {
            return collection;
        }
    }

    public class MapResultTransformer<K, V> implements ListResultTransformer {

        Map<K, V> result = new HashMap<>();

        @Override
        public Object transformTuple(Object[] tuple, String[] aliases) {
            K key = (K) tuple[0];
            V value = (V) tuple[1];
            result.put(
                key,
                value
            );
            return tuple;
        }

        @Override
        public List transformList(List collection) {
            return Collections.singletonList(result);
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Column(name = "created_on")
        private LocalDate createdOn;

        public Long getId() {
            return id;
        }

        public Post setId(Long id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Post setTitle(String title) {
            this.title = title;
            return this;
        }

        public LocalDate getCreatedOn() {
            return createdOn;
        }

        public Post setCreatedOn(LocalDate createdOn) {
            this.createdOn = createdOn;
            return this;
        }
    }
}
