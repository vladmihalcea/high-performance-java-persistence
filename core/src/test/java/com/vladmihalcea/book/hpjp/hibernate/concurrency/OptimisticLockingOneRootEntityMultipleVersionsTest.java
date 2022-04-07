package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.transaction.VoidCallable;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.matchers.ThrowableCauseMatcher;
import org.junit.rules.ExpectedException;

import jakarta.persistence.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;


/**
 * EntityOptimisticLockingHighUpdateRateSingleEntityTest - Test to check optimistic checking on a single entity being updated by many threads
 *
 * @author Vlad Mihalcea
 */
public class OptimisticLockingOneRootEntityMultipleVersionsTest extends AbstractTest {

    private ExecutorService executorService = Executors.newFixedThreadPool(10);
    private Post originalPost;

    @Before
    public void addPost() {
        originalPost = doInJPA(entityManager -> {
            Post post = new Post();
            PostViews views = new PostViews();
            views.setPost(post);
            post.views = views;
            PostLikes likes = new PostLikes();
            likes.setPost(post);
            post.likes = likes;

            post.setId(1L);
            post.setTitle("JDBC");
            entityManager.persist(post);
            return post;
        });
    }


    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    static interface DoWithPost {
        void with(Post post);
    }

    public static class ModifyViews implements DoWithPost {
        private final Long views;

        public ModifyViews(Long views) {
            this.views = views;
        }

        public void with(Post post) {
            post.setViews(views);
        }
    }

    public static class ModifyTitle implements DoWithPost {
        private final String title;

        public ModifyTitle(String title) {
            this.title = title;
        }


        public void with(Post post) {
            post.setTitle(title);
        }
    }

    public static class IncrementLikes implements DoWithPost {

        public void with(Post post) {
            post.incrementLikes();
        }
    }

    public class TransactionTemplate implements VoidCallable {
        private final DoWithPost doWithPost;
        private CyclicBarrier barrier;

        public TransactionTemplate(DoWithPost doWithPost, CyclicBarrier barrier) {
            this.doWithPost = doWithPost;
            this.barrier = barrier;
        }

        public void execute() {
            doInJPA(entityManager -> {
                try {
                    Post post = entityManager.find(Post.class, 1L);
                    barrier.await();
                    doWithPost.with(post);
                    return null;
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
    }


    public Post getPostById(final long postId) {
        return doInJPA(entityManager -> (Post) entityManager.find(Post.class, postId));
    }

    @Test
    public void canConcurrentlyModifyEachOfSubEntities() throws InterruptedException, ExecutionException {
        executeOperations(
                new IncrementLikes(),
                new ModifyTitle("JPA"),
                new ModifyViews(15L));

        Post modifiedPost = getPostById(originalPost.getId());

        assertThat(modifiedPost.getTitle(), equalTo("JPA"));
        assertThat(modifiedPost.getViews(), equalTo(15L));
        assertThat(modifiedPost.getLikes(), equalTo(originalPost.getLikes() + 1));
    }


    @Test
    public void optimisticLockingViolationForConcurrentViewsModifications() throws InterruptedException, ExecutionException {
        expectedException.expectCause(new ThrowableCauseMatcher<>(IsInstanceOf.<Throwable>instanceOf(OptimisticLockException.class)));

        executeOperations(
                new IncrementLikes(),
                new ModifyViews(100L),
                new ModifyTitle("JPA"),
                new ModifyViews(1000L));
    }

    @Test
    public void optimisticLockingViolationForConcurrentPostModifications() throws InterruptedException, ExecutionException {
        expectedException.expectCause(new ThrowableCauseMatcher<>(IsInstanceOf.<Throwable>instanceOf(OptimisticLockException.class)));

        executeOperations(
                new IncrementLikes(),
                new ModifyTitle("Hibernate"),
                new ModifyTitle("JPA"),
                new ModifyViews(1L));
    }


    @Test
    public void optimisticLockingViolationForConcurrentLikeModifications() throws InterruptedException, ExecutionException {
        expectedException.expectCause(new ThrowableCauseMatcher<>(IsInstanceOf.<Throwable>instanceOf(OptimisticLockException.class)));

        executeOperations(
                new IncrementLikes(),
                new IncrementLikes(),
                new ModifyTitle("JPA"),
                new ModifyViews(2L));

    }

    private void executeOperations(DoWithPost... operations) throws InterruptedException, ExecutionException {
        CyclicBarrier cyclicBarrier = new CyclicBarrier(operations.length);
        List<TransactionTemplate> tasks = new LinkedList<>();
        for (DoWithPost operation : operations) {
            tasks.add(new TransactionTemplate(operation, cyclicBarrier));
        }

        List<Future<Void>> futures = executorService.invokeAll(tasks);

        for (Future<Void> future : futures) {
            future.get();
        }
    }

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                PostViews.class,
                PostLikes.class
        };
    }
    
    @Entity(name = "PostViews")
    @Table(name = "post_views")
    public static class PostViews {

        @Id
        private Long id;

        @MapsId
        @OneToOne
        private Post post;

        private long views;

        @Version
        private int version;

        public Long getId() {
            return id;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public long getViews() {
            return views;
        }

        public void setViews(long views) {
            this.views = views;
        }
    }

    @Entity(name = "PostLikes")
    @Table(name = "post_likes")
    public static class PostLikes {

        @Id
        private Long id;

        @MapsId
        @OneToOne
        private Post post;

        private int likes;

        @Version
        private int version;

        public Long getId() {
            return id;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public int getLikes() {
            return likes;
        }

        public int incrementLikes() {
            return ++likes;
        }
    }

    @Entity(name = "Post") @Table(name = "post")
    public static class Post {

        @Id
        private Long id;
        
        private String title;

        @OneToOne(optional = false, mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
        private PostViews views;

        @OneToOne(optional = false, mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
        private PostLikes likes;

        @Version
        private int version;

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

        public long getViews() {
            return views.getViews();
        }

        public void setViews(long views) {
            this.views.setViews(views);
        }

        public int getLikes() {
            return likes.getLikes();
        }

        public int incrementLikes() {
            return likes.incrementLikes();
        }
    }
}
