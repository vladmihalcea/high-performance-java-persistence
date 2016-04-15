package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.vladmihalcea.book.hpjp.util.AbstractTest;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/**
 * AbstractEntityOptimisticLockingCollectionTest - Abstract Test to check optimistic locking collection versioning
 *
 * @author Vlad Mihalcea
 */
public abstract class AbstractEntityOptimisticLockingCollectionTest<P extends AbstractEntityOptimisticLockingCollectionTest.IPost<C>, C extends AbstractEntityOptimisticLockingCollectionTest.IComment<P>> extends AbstractTest {

    public interface IPost<C> {
        Long getId();

        void setId(Long id);

        public String getName();

        public void setName(String name);

        public List<C> getComments();

        public int getVersion();

        public void addComment(C comment);
    }

    public interface IComment<P> {

        String getReview();

        void setReview(String review);
    }

    private final Class<P> postClass;

    private final Class<C> commentClass;

    protected AbstractEntityOptimisticLockingCollectionTest(Class<P> postClass, Class<C> commentClass) {
        this.postClass = postClass;
        this.commentClass = commentClass;
    }

    protected void simulateConcurrentTransactions(final boolean shouldIncrementParentVersion) {
        doInJPA(entityManager -> {
            try {
                P post = postClass.newInstance();
                post.setId(1L);
                post.setName("Hibernate training");
                entityManager.persist(post);
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        });

        doInJPA(entityManager -> {
            final P post = (P) entityManager.find(postClass, 1L);
            try {
                executeSync(() -> {
                    doInJPA(_entityManager -> {
                        try {
                            P otherThreadPost = (P) _entityManager.find(postClass, 1L);
                            int loadTimeVersion = otherThreadPost.getVersion();
                            assertNotSame(post, otherThreadPost);
                            assertEquals(0L, otherThreadPost.getVersion());
                            C comment = commentClass.newInstance();
                            comment.setReview("Good post!");
                            otherThreadPost.addComment(comment);
                            _entityManager.flush();
                            if (shouldIncrementParentVersion) {
                                assertEquals(otherThreadPost.getVersion(), loadTimeVersion + 1);
                            } else {
                                assertEquals(otherThreadPost.getVersion(), loadTimeVersion);
                            }
                        } catch (Exception e) {
                            throw new IllegalArgumentException(e);
                        }
                    });
                });
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
            post.setName("Hibernate Master Class");
            entityManager.flush();
        });
    }
}
