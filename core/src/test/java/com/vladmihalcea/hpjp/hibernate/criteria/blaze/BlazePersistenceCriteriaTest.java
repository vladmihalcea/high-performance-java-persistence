package com.vladmihalcea.hpjp.hibernate.criteria.blaze;

import com.blazebit.persistence.CTE;
import com.blazebit.persistence.Criteria;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.JoinType;
import com.blazebit.persistence.spi.CriteriaBuilderConfiguration;
import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import io.hypersistence.utils.hibernate.query.ListResultTransformer;
import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class BlazePersistenceCriteriaTest extends AbstractTest {

    private CriteriaBuilderFactory cbf;

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostDetails.class,
            PostComment.class,
            Tag.class,
            PostCommentCountCTE.class,
            LatestPostCommentCTE.class,
            PostCommentMaxIdCTE.class
        };
    }

    @Override
    protected EntityManagerFactory newEntityManagerFactory() {
        EntityManagerFactory entityManagerFactory = super.newEntityManagerFactory();
        CriteriaBuilderConfiguration config = Criteria.getDefault();
        cbf = config.createCriteriaBuilderFactory(entityManagerFactory);
        return entityManagerFactory;
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            Post post = new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence")
                .addComment(
                    new PostComment()
                        .setId(1L)
                        .setReview("Best book on JPA and Hibernate!")
                )
                .addComment(
                    new PostComment()
                        .setId(2L)
                        .setReview("A great reference book.")
                )
                .addComment(
                    new PostComment()
                        .setId(3L)
                        .setReview("A must-read for every Java developer!")
                );

            entityManager.persist(post);

            entityManager.persist(
                new PostDetails()
                    .setPost(post)
                    .setCreatedBy("Vlad Mihalcea")
            );

            Tag java = new Tag().setName("Java");
            Tag hibernate = new Tag().setName("Hibernate");

            entityManager.persist(java);
            entityManager.persist(hibernate);

            post.getTags().add(java);
            post.getTags().add(hibernate);
        });
    }

    @Test
    public void testLateralJoinBlaze() {
        doInJPA(entityManager -> {
            List<Tuple> tuples = entityManager.createNativeQuery("""
                    SELECT
                      p.id AS "p.id",
                      p.title AS "p.title",
                      pc3.latest_comment_id AS "pc.id",
                      pc3.latest_comment_review AS "pc.review"
                    FROM
                      post p,
                      LATERAL (
                      	SELECT
                      	  pc2.post_comment_id AS latest_comment_id,
                      	  pc2.post_comment_review AS latest_comment_review
                      	FROM (
                      	  SELECT
                      	  	pc1.id AS post_comment_id,
                      	  	pc1.review AS post_comment_review,
                      	  	pc1.post_id AS post_comment_post_id,
                      	  	MAX(pc1.id) OVER (PARTITION BY pc1.post_id) AS max_post_comment_id
                      	  FROM post_comment pc1
                      	) pc2
                        WHERE 
                          pc2.post_comment_id = pc2.max_post_comment_id AND 
                          pc2.post_comment_post_id = p.id
                    ) pc3
			    """, Tuple.class)
                .unwrap(NativeQuery.class)
                .setResultTransformer(new ListResultTransformer() {
                    @Override
                    public Object transformTuple(Object[] tuple, String[] aliases) {
                        return new PostComment()
                            .setId(((Number) tuple[2]).longValue())
                            .setReview((String) tuple[3])
                            .setPost(
                                new Post()
                                    .setId(((Number) tuple[0]).longValue())
                                    .setTitle((String) tuple[1])
                            );
                    }
                })
                .getResultList();

            assertEquals(1, tuples.size());
        });
    }

    @Test
    public void testDerivedTableJoinBlaze() {
        doInJPA(entityManager -> {
            List<Tuple> tuples = entityManager.createNativeQuery("""
                SELECT
                   p.id AS post_id,
                   p.title AS post_title,
                   pc2.review AS comment_review
                FROM (
                   SELECT
                      pc1.id AS id,
                      pc1.review AS review,
                      pc1.post_id AS post_id,
                      MAX(pc1.id) OVER (PARTITION BY pc1.post_id) AS max_id
                   FROM post_comment pc1
                ) pc2
                JOIN post p ON p.id = pc2.post_id
                WHERE 
                   pc2.id = pc2.max_id   
			    """, Tuple.class)
                .getResultList();

            assertEquals(1, tuples.size());
            Tuple tuple = tuples.get(0);
            assertEquals(1L, longValue(tuple.get("post_id")));
            assertEquals("High-Performance Java Persistence", tuple.get("post_title"));
            assertEquals("A must-read for every Java developer!", tuple.get("comment_review"));
        });

        doInJPA(entityManager -> {
            List<Tuple> tuples = cbf
                .create(entityManager, Tuple.class)
                .fromSubquery(PostCommentMaxIdCTE.class, "pc2")
                    .from(PostComment.class, "pc1")
                    .bind("id").select("pc1.id")
                    .bind("review").select("pc1.review")
                    .bind("postId").select("pc1.post.id")
                    .bind("maxId").select("MAX(pc1.id) OVER (PARTITION BY pc1.post.id)")
                .end()
                .joinOn(Post.class, "p", JoinType.INNER).onExpression("p.id = pc2.postId").end()
                .where("pc2.id").eqExpression("pc2.maxId")
                .select("p.id", "post_id")
                .select("p.title", "post_title")
                .select("pc2.review", "comment_review")
                .getResultList();

            assertEquals(1, tuples.size());
            Tuple tuple = tuples.get(0);
            assertEquals(1L, longValue(tuple.get("post_id")));
            assertEquals("High-Performance Java Persistence", tuple.get("post_title"));
            assertEquals("A must-read for every Java developer!", tuple.get("comment_review"));
        });
    }

    @Test
    public void testGroupBy() {
        doInJPA(entityManager -> {
            List<Tuple> tuples = entityManager
                .createNativeQuery("""
                 select
                     p.title as post_title,
                     count(pc.id) as comment_count
                 from post p
                 left join post_comment pc on pc.post_id = p.id
                 join post_details pd on p.id = pd.id
                 where pd.created_by = :createdBy
                 group by p.title
			    """, Tuple.class)
                .setParameter("createdBy", "Vlad Mihalcea")
                .getResultList();

            assertEquals(1, tuples.size());
        });

        doInJPA(entityManager -> {
            List<Tuple> tuples = cbf
                .create(entityManager, Tuple.class)
                .from(Post.class, "p")
                .leftJoinOn(PostComment.class, "pc").onExpression("pc.post = p").end()
                .joinOn(PostDetails.class, "pd", JoinType.INNER).onExpression("pd.post = p").end()
                .where("pd.createdBy").eqExpression(":createdBy")
                .groupBy("p.title")
                .select("p.title", "post_title")
                .select("count(pc.id)", "comment_count")
                .setParameter("createdBy", "Vlad Mihalcea")
                .getResultList();

            assertEquals(1, tuples.size());
        });
    }

    @Test
    public void testJoinGroupBy() {
        doInJPA(entityManager -> {
            List<Tuple> tuples = entityManager
                .createNativeQuery("""
                 select 
                    p1.title,
                    p_c.comment_count
                 from post p1
                 join (
                    select
                         p.title as post_title,
                         count(pc.id) as comment_count
                     from post p
                     left join post_comment pc on pc.post_id = p.id
                     join post_details pd on p.id = pd.id
                     where pd.created_by = :createdBy
                     group by p.title
                 ) p_c on p1.title = p_c.post_title
			    """, Tuple.class)
                .setParameter("createdBy", "Vlad Mihalcea")
                .getResultList();

            assertEquals(1, tuples.size());
        });

        doInJPA(entityManager -> {
            List<Tuple> tuples = cbf
                .create(entityManager, Tuple.class)
                .from(Post.class, "p1")
                .leftJoinOnSubquery(PostCommentCountCTE.class, "p_c")
                    .from(Post.class, "p")
                    .bind("id").select("p.id")
                    .bind("postTitle").select("p.title")
                    .bind("commentCount").select("count(pc.id)")
                    .leftJoinOn(PostComment.class, "pc").onExpression("pc.post = p").end()
                    .joinOn(PostDetails.class, "pd", JoinType.INNER).onExpression("pd.post = p").end()
                    .where("pd.createdBy").eqExpression(":createdBy")
                    .groupBy("p.title", "p.id")
                    .end()
                .onExpression("p_c.id = p1.id")
                .end()
                .select("p1.title", "post_title")
                .select("p_c.commentCount", "comment_count")
                .setParameter("createdBy", "Vlad Mihalcea")
                .getResultList();

            assertEquals(1, tuples.size());
        });
    }

    @Test
    public void testCriteriaAPIAlternative() {
        final int maxCount = 50;
        final String titlePattern = "High-Performance Java Persistence";

        doInJPA(entityManager -> {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<Post> criteria = builder.createQuery(Post.class);
            Root<Post> post = criteria.from(Post.class);
            ParameterExpression<String> parameterExpression = builder.parameter(String.class);
            List<Post> posts = entityManager.createQuery(
                    criteria
                        .where(builder.like(post.get(Post_.TITLE), parameterExpression))
                        .orderBy(builder.asc(post.get(Post_.ID)))
                )
                .setParameter(parameterExpression, titlePattern)
                .setMaxResults(maxCount)
                .getResultList();

            assertEquals(1, posts.size());
        });

        doInJPA(entityManager -> {
            List<Post> tuples = cbf.create(entityManager, Post.class)
                .from(Post.class, "p")
                .where(Post_.TITLE).like().expression(":titlePattern").noEscape()
            .orderBy(Post_.ID, true)
            .setParameter("titlePattern", titlePattern)
            .setMaxResults(maxCount)
            .getResultList();

            assertEquals(1, tuples.size());
        });
    }

    @Test
    public void testHibernateCriteriaAPI() {
        final int maxCount = 50;
        final String titlePattern = "High-Performance Java Persistence";

        doInJPA(entityManager -> {
            HibernateCriteriaBuilder builder = entityManager
                .unwrap(Session.class)
                .getCriteriaBuilder();

            CriteriaQuery<Post> criteria = builder.createQuery(Post.class);
            Root<Post> post = criteria.from(Post.class);
            ParameterExpression<String> parameterExpression = builder.parameter(String.class);
            List<Post> posts = entityManager.createQuery(
                    criteria
                        .where(builder.ilike(post.get(Post_.TITLE), parameterExpression))
                        .orderBy(builder.asc(post.get(Post_.ID)))
                )
                .setParameter(parameterExpression, titlePattern)
                .setMaxResults(maxCount)
                .getResultList();

            assertEquals(1, posts.size());
        });
    }

    @CTE
    @Entity
    public static class PostCommentMaxIdCTE {
        @Id
        private Long id;
        private String review;
        private Long postId;
        private Long maxId;
    }

    @CTE
    @Entity
    public static class LatestPostCommentCTE {
        @Id
        private Long id;
        private String review;
    }

    @CTE
    @Entity
    public static class PostCommentCountCTE {
        @Id
        private Long id;
        private String postTitle;
        private Long commentCount;
    }

    @Entity(name = "PostDetails")
    @Table(name = "post_details")
    public static class PostDetails {

        @Id
        private Long id;

        @Column(name = "created_on")
        private Date createdOn;

        @Column(name = "created_by")
        private String createdBy;

        public PostDetails() {
            createdOn = new Date();
        }

        @OneToOne(fetch = FetchType.LAZY)
        @MapsId
        @JoinColumn(name = "id")
        private Post post;

        public Long getId() {
            return id;
        }

        public PostDetails setId(Long id) {
            this.id = id;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostDetails setPost(Post post) {
            this.post = post;
            return this;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public PostDetails setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
            return this;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public PostDetails setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }
    }

    @Entity(name = "Tag")
    @Table(name = "tag")
    public static class Tag {

        @Id
        @GeneratedValue
        private Long id;

        private String name;

        public String getName() {
            return name;
        }

        public Tag setName(String name) {
            this.name = name;
            return this;
        }
    }
}
