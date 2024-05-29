package com.vladmihalcea.hpjp.hibernate.fetching.multiple;

import com.blazebit.persistence.Criteria;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.spi.CriteriaBuilderConfiguration;
import com.blazebit.persistence.view.*;
import com.blazebit.persistence.view.spi.EntityViewConfiguration;
import com.vladmihalcea.hpjp.hibernate.forum.Post_;
import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.hpjp.util.exception.ExceptionUtil;
import jakarta.persistence.CascadeType;
import jakarta.persistence.*;
import org.hibernate.loader.MultipleBagFetchException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.blazebit.persistence.view.FetchStrategy.MULTISET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class EagerFetchingMultipleBagTest extends AbstractPostgreSQLIntegrationTest {

    public static final long POST_COUNT = 50;
    public static final long POST_COMMENT_COUNT = 20;
    public static final long TAG_COUNT = 10;

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                PostComment.class,
                Tag.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "50");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {

            List<Tag> tags = new ArrayList<>();

            for (long i = 1; i <= TAG_COUNT; i++) {
                Tag tag = new Tag()
                    .setId(i)
                    .setName(String.format("Tag nr. %d", i));

                entityManager.persist(tag);
                tags.add(tag);
            }

            long commentId = 0;

            for (long postId = 1; postId <= POST_COUNT; postId++) {
                Post post = new Post()
                    .setId(postId)
                    .setTitle(String.format("Post nr. %d", postId));


                for (long i = 0; i < POST_COMMENT_COUNT; i++) {
                    post.addComment(
                        new PostComment()
                            .setId(++commentId)
                            .setReview("Excellent!")
                    );
                }

                for (int i = 0; i < TAG_COUNT; i++) {
                    post.getTags().add(tags.get(i));
                }

                entityManager.persist(post);
            }
        });
    }

    @Test
    public void testOneQueryTwoJoinFetch() {
        try {
            doInJPA(entityManager -> {
                List<Post> posts = entityManager.createQuery("""
                    select p
                    from Post p
                    left join fetch p.comments
                    left join fetch p.tags
                    where p.id between :minId and :maxId
                    """, Post.class)
                .setParameter("minId", 1L)
                .setParameter("maxId", 50L)
                .getResultList();
            });
        } catch (Exception e) {
            assertTrue(
                MultipleBagFetchException.class.isAssignableFrom(
                    ExceptionUtil.rootCause(e).getClass()
                )
            );
            LOGGER.error("Failure", e);
        }
    }

    @Test
    public void testTwoJoinFetchQueries() {
        List<Post> _posts = doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p
                left join fetch p.comments
                where p.id between :minId and :maxId
                """, Post.class)
            .setParameter("minId", 1L)
            .setParameter("maxId", 50L)
            .getResultList();

            posts = entityManager.createQuery("""
                select p
                from Post p
                left join fetch p.tags t
                where p in :posts
                """, Post.class)
            .setParameter("posts", posts)
            .getResultList();

            assertEquals(POST_COUNT, posts.size());

            return posts;
        });

        for(Post post : _posts) {
            assertEquals(POST_COMMENT_COUNT, post.getComments().size());
            assertEquals(TAG_COUNT, post.getTags().size());
        }
    }

    @Test
    public void testTwoJoinFetchQueriesWithoutInClause() {
        List<Post> _posts = doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p
                left join fetch p.comments
                where p.id between :minId and :maxId
                """, Post.class)
            .setParameter("minId", 1L)
            .setParameter("maxId", 50L)
            .getResultList();

            posts = entityManager.createQuery("""
                select p
                from Post p
                left join fetch p.tags t
                where p.id between :minId and :maxId
                """, Post.class)
            .setParameter("minId", 1L)
            .setParameter("maxId", 50L)
            .getResultList();

            assertEquals(POST_COUNT, posts.size());

            return posts;
        });

        for(Post post : _posts) {
            assertEquals(POST_COMMENT_COUNT, post.getComments().size());
            assertEquals(TAG_COUNT, post.getTags().size());
        }
    }

    @Test
    public void testBlaze() {
        CriteriaBuilderConfiguration config = Criteria.getDefault();
        CriteriaBuilderFactory criteriaBuilderFactory = config.createCriteriaBuilderFactory(entityManagerFactory());

        EntityViewConfiguration entityViewConfiguration = EntityViews.createDefaultConfiguration()
            .addEntityView(PostView.class)
            .addEntityView(PostCommentView.class)
            .addEntityView(TagView.class)
            .addEntityView(PostWithCommentsAndTagsView.class);
        
        EntityViewManager entityViewManager = entityViewConfiguration
            .createEntityViewManager(criteriaBuilderFactory);

        List<PostWithCommentsAndTagsView> posts = doInJPA(entityManager -> {
            return entityViewManager.applySetting(
                    EntityViewSetting.create(PostWithCommentsAndTagsView.class),
                    criteriaBuilderFactory.create(entityManager, Post.class)
                )
                .where(Post_.ID)
                .betweenExpression(":minId")
                .andExpression(":maxId")
                .setParameter("minId", 1L)
                .setParameter("maxId", 50L)
                .getResultList();
        });

        for(PostWithCommentsAndTagsView post : posts) {
            assertEquals(POST_COMMENT_COUNT, post.getComments().size());
            assertEquals(TAG_COUNT, post.getTags().size());
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<PostComment> comments = new ArrayList<>();

        @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
        @JoinTable(name = "post_tag",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
        )
        private List<Tag> tags = new ArrayList<>();

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

        public List<PostComment> getComments() {
            return comments;
        }

        public Post addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
            return this;
        }

        public List<Tag> getTags() {
            return tags;
        }

        public void setTags(List<Tag> tags) {
            this.tags = tags;
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        private String review;

        public Long getId() {
            return id;
        }

        public PostComment setId(Long id) {
            this.id = id;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostComment setPost(Post post) {
            this.post = post;
            return this;
        }

        public String getReview() {
            return review;
        }

        public PostComment setReview(String review) {
            this.review = review;
            return this;
        }
    }

    @Entity(name = "Tag")
    @Table(name = "tag")
    public static class Tag {

        @Id
        private Long id;

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

    @EntityView(Post.class)
    public interface PostView {
        @IdMapping
        Long getId();
    
        String getTitle();
    }

    @EntityView(PostComment.class)
    public interface PostCommentView {
        @IdMapping
        Long getId();

        String getReview();
    }

    @EntityView(Tag.class)
    public interface TagView {
        @IdMapping
        Long getId();
    
        String getName();
    }

    @EntityView(Post.class)
    public interface PostWithCommentsAndTagsView extends PostView {

        @Mapping(fetch = MULTISET)
        List<PostCommentView> getComments();

        @Mapping(fetch = MULTISET)
        List<TagView> getTags();
    }
}
