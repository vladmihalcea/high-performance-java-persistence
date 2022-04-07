package com.vladmihalcea.book.hpjp.hibernate.fetching.detector;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.stat.internal.StatisticsInitiator;
import org.junit.Test;

import jakarta.persistence.*;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class EagerFetchingDetectorTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class,
            PostCommentDetails.class,
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(
            AvailableSettings.GENERATE_STATISTICS,
            Boolean.TRUE.toString()
        );
        properties.put(
            StatisticsInitiator.STATS_BUILDER,
            SessionStatistics.Factory.INSTANCE
        );
    }

    @Override
    protected Integrator integrator() {
        return AssociationFetchingEventListenerIntegrator.INSTANCE;
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");

            PostComment comment1 = new PostComment();
            comment1.setId(1L);
            comment1.setReview("Good");
            comment1.setPost(post);

            PostCommentDetails details1 = new PostCommentDetails();
            details1.setComment(comment1);
            details1.setVotes(10);

            PostComment comment2 = new PostComment();
            comment2.setId(2L);
            comment2.setReview("Excellent");
            comment2.setPost(post);

            PostCommentDetails details2 = new PostCommentDetails();
            details2.setComment(comment2);
            details2.setVotes(10);

            entityManager.persist(post);
            entityManager.persist(comment1);
            entityManager.persist(comment2);
            entityManager.persist(details1);
            entityManager.persist(details2);
        });
    }

    @Test
    public void testFindPostComment() {
        doInJPA(entityManager -> {
            AssociationFetch.Context context = AssociationFetch.Context.get(entityManager);
            assertTrue(context.getAssociationFetches().isEmpty());

            PostComment comment = entityManager.find(PostComment.class, 1L);

            List<AssociationFetch> associationFetches = context.getAssociationFetches();
            assertEquals(1, associationFetches.size());
            assertEquals(1, context.getJoinedAssociationFetches().size());
            assertEquals(0, context.getSecondaryAssociationFetches().size());

            AssociationFetch associationFetch = associationFetches.get(0);
            assertSame(comment.getPost(), associationFetch.getEntity());
        });
    }

    @Test
    public void testFindPostCommentDetails() {
        doInJPA(entityManager -> {
            AssociationFetch.Context context = AssociationFetch.Context.get(entityManager);
            assertTrue(context.getAssociationFetches().isEmpty());

            PostCommentDetails commentDetails = entityManager.find(PostCommentDetails.class, 1L);

            assertEquals(1, context.getJoinedAssociationFetches().size());
            assertEquals(1, context.getSecondaryAssociationFetches().size());
            Map<Class, List<Object>> associationFetchMap = context.getAssociationFetchEntityMap();
            assertEquals(2, associationFetchMap.size());

            List<Object> postCommentAssociationFetches = associationFetchMap.get(PostComment.class);
            assertEquals(1, postCommentAssociationFetches.size());
            assertSame(commentDetails.getComment(), postCommentAssociationFetches.get(0));

            List<Object> postAssociationFetches = associationFetchMap.get(Post.class);
            assertEquals(1, postAssociationFetches.size());
            assertSame(commentDetails.getComment().getPost(), postAssociationFetches.get(0));
        });
    }

    @Test
    public void testJPQLPostCommentDetails() {
        doInJPA(entityManager -> {
            AssociationFetch.Context context = AssociationFetch.Context.get(entityManager);
            assertTrue(context.getAssociationFetches().isEmpty());

            List<PostCommentDetails> commentDetailsList = entityManager.createQuery("""
                select pcd
                from PostCommentDetails pcd
                order by pcd.id
                """,
                PostCommentDetails.class)
            .getResultList();

            assertEquals(3, context.getAssociationFetches().size());
            assertEquals(2, context.getSecondaryAssociationFetches().size());
            assertEquals(1, context.getJoinedAssociationFetches().size());

            Map<Class, List<Object>> associationFetchMap = context.getAssociationFetchEntityMap();
            assertEquals(2, associationFetchMap.size());

            for (PostCommentDetails commentDetails : commentDetailsList) {
                assertTrue(associationFetchMap.get(PostComment.class).contains(commentDetails.getComment()));
                assertTrue(associationFetchMap.get(Post.class).contains(commentDetails.getComment().getPost()));
            }
        });
    }

    @Test
    public void testJPQLPostCommentDetailsJoinFetchEagerAssociations() {
        doInJPA(entityManager -> {
            AssociationFetch.Context context = AssociationFetch.Context.get(entityManager);
            assertTrue(context.getAssociationFetches().isEmpty());

            List<PostCommentDetails> commentDetailsList = entityManager.createQuery("""
                select pcd
                from PostCommentDetails pcd
                join fetch pcd.comment pc
                join fetch pc.post
                order by pcd.id
                """,
                PostCommentDetails.class)
            .getResultList();

            assertEquals(3, context.getJoinedAssociationFetches().size());
            assertTrue(context.getSecondaryAssociationFetches().isEmpty());
        });
    }

    @Test
    public void testStatisticsSecondaryQueries() {
        doInJPA(entityManager -> {
            assertEquals(0, SessionStatistics.getEntityFetchCount(PostCommentDetails.class));
            assertEquals(0, SessionStatistics.getEntityFetchCount(PostComment.class));
            assertEquals(0, SessionStatistics.getEntityFetchCount(Post.class));

            List<PostCommentDetails> commentDetailsList = entityManager.createQuery("""
                select pcd
                from PostCommentDetails pcd
                order by pcd.id
                """,
                PostCommentDetails.class)
            .getResultList();

            assertEquals(2, commentDetailsList.size());

            assertEquals(0, SessionStatistics.getEntityFetchCount(PostCommentDetails.class));
            assertEquals(2, SessionStatistics.getEntityFetchCount(PostComment.class));
            assertEquals(0, SessionStatistics.getEntityFetchCount(Post.class));
        });
    }

    @Test
    public void testStatisticsJoinFetch() {
        doInJPA(entityManager -> {
            assertEquals(0, SessionStatistics.getEntityFetchCount(PostCommentDetails.class));
            assertEquals(0, SessionStatistics.getEntityFetchCount(PostComment.class));
            assertEquals(0, SessionStatistics.getEntityFetchCount(Post.class));

            List<PostCommentDetails> commentDetailsList = entityManager.createQuery("""
                select pcd
                from PostCommentDetails pcd
                join fetch pcd.comment pc
                join fetch pc.post
                order by pcd.id
                """,
                PostCommentDetails.class)
            .getResultList();

            assertEquals(2, commentDetailsList.size());

            assertEquals(0, SessionStatistics.getEntityFetchCount(PostCommentDetails.class));
            assertEquals(0, SessionStatistics.getEntityFetchCount(PostComment.class));
            assertEquals(0, SessionStatistics.getEntityFetchCount(Post.class));
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

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
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne
        private Post post;

        private String review;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
        }
    }

    @Entity(name = "PostCommentDetails")
    @Table(name = "post_comment_details")
    public static class PostCommentDetails {

        @Id
        private Long id;

        @OneToOne
        @MapsId
        @OnDelete(action = OnDeleteAction.CASCADE)
        private PostComment comment;

        private int votes;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public PostComment getComment() {
            return comment;
        }

        public void setComment(PostComment comment) {
            this.comment = comment;
        }

        public int getVotes() {
            return votes;
        }

        public void setVotes(int votes) {
            this.votes = votes;
        }
    }
}
