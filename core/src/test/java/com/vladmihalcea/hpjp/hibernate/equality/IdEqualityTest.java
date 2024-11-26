package com.vladmihalcea.hpjp.hibernate.equality;

import com.vladmihalcea.hpjp.hibernate.identifier.Identifiable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.groupingBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class IdEqualityTest
        extends AbstractEqualityCheckTest<IdEqualityTest.Post> {

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Post.class
        };
    }

    @Test
    public void testEquality() {
        Post post = new Post();
        post.setTitle("High-PerformanceJava Persistence");

        assertEqualityConsistency(Post.class, post);
    }

    @Test
    public void testCollectionSize() {
        if(!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        int collectionSize = 25_000;

        long createListStartNanos = System.nanoTime();
        List<Post> postList = new ArrayList<>(collectionSize);

        for (int i = 0; i < collectionSize; i++) {
            Post post = new Post().setId((long) i);
            postList.add(i, post);
        }

        long createListEndNanos = System.nanoTime();
        LOGGER.info(
            "Creating a List with [{}] elements took : [{}] μs",
            collectionSize,
            TimeUnit.NANOSECONDS.toMicros(createListEndNanos - createListStartNanos)
        );

        long createSetStartNanos = System.nanoTime();
        Set<Post> postSet = new HashSet<>();

        for (int i = 0; i < collectionSize; i++) {
            Post post = new Post().setId((long) i);
            postSet.add(post);
        }

        long createSetEndNanos = System.nanoTime();
        LOGGER.info(
            "Creating a Set with [{}] elements took : [{}] μs",
            collectionSize,
            TimeUnit.NANOSECONDS.toMicros(createSetEndNanos - createSetStartNanos)
        );

        Random random = new Random();
        Post randomPost = postList.get(random.nextInt(collectionSize));
        long startNanos = System.nanoTime();
        boolean contained = postList.contains(randomPost);
        long endNanos = System.nanoTime();
        assertTrue(contained);
        LOGGER.info(
            "Calling HashSet contains took : [{}] microseconds",
            TimeUnit.NANOSECONDS.toMicros(endNanos - startNanos)
        );
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post implements Identifiable<Long> {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        public Post() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (!(o instanceof Post))
                return false;

            Post other = (Post) o;

            return id != null && id.equals(other.getId());
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

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

        public void setTitle(String title) {
            this.title = title;
        }
    }
}
