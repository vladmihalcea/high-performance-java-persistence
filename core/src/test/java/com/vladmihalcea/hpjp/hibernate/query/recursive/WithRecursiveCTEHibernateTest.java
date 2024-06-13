package com.vladmihalcea.hpjp.hibernate.query.recursive;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import io.hypersistence.utils.hibernate.type.util.ClassImportIntegrator;
import jakarta.persistence.*;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.jpa.boot.spi.JpaSettings;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class WithRecursiveCTEHibernateTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(
            JpaSettings.INTEGRATOR_PROVIDER,
            (IntegratorProvider) () -> Collections.singletonList(
                new ClassImportIntegrator(
                    List.of(
                        PostCommentRecord.class
                    )
                )
            )
        );
        //properties.put("hibernate.hbm2ddl.auto", "none");
    }

    /*@Override
    protected void beforeInit() {
        executeStatement("drop table if exists post cascade");
        executeStatement("drop table if exists post_comment cascade");
        executeStatement("drop sequence if exists post_comment_SEQ");
        executeStatement("create sequence post_comment_SEQ start with 1 increment by 1");
        executeStatement("create table post (id bigint not null, title varchar(100), primary key (id))");
        executeStatement("create table post_comment (id bigint not null, post_id bigint, created_on timestamp(6), review varchar(250), score integer not null, parent_id bigint, primary key (id))");
        executeStatement("alter table if exists post_comment add constraint FK_post_comment_parent_id foreign key (parent_id) references post_comment");
        executeStatement("alter table if exists post_comment add constraint FK_post_comment_post_id foreign key (post_id) references post");
    }*/
    
    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            Post post = new Post()
                .setId(1L)
                .setTitle("Post 1");

            entityManager.persist(post);

            entityManager.persist(
                new PostComment()
                    .setPost(post)
                    .setCreatedOn(LocalDateTime.of(2024, 6, 13, 12, 23, 5))
                    .setScore(1)
                    .setReview("Comment 1")
                    .addChild(
                        new PostComment()
                            .setPost(post)
                            .setCreatedOn(LocalDateTime.of(2024, 6, 14, 13, 23, 10))
                            .setScore(2)
                            .setReview("Comment 1.1")
                    )
                    .addChild(
                        new PostComment()
                            .setPost(post)
                            .setCreatedOn(LocalDateTime.of(2024, 6, 14, 15, 45, 15))
                            .setScore(2)
                            .setReview("Comment 1.2")
                            .addChild(
                                new PostComment()
                                    .setPost(post)
                                    .setCreatedOn(LocalDateTime.of(2024, 6, 15, 10, 15, 20))
                                    .setScore(1)
                                    .setReview("Comment 1.2.1")
                            )
                    )
            );
        });
    }

    @Test
    public void testJPQLWithRecursive() {
        doInJPA(entityManager -> {
            List<PostCommentRecord>  postComments = entityManager.createQuery("""
                WITH postCommentChildHierarchy AS (
                  SELECT pc.children pc
                  FROM PostComment pc
                  WHERE pc.id = :commentId
                  UNION ALL
                  SELECT pc.children pc
                  FROM PostComment pc
                  JOIN postCommentChildHierarchy pch ON pc = pch.pc
                  ORDER BY pc.id
                )
                SELECT new PostCommentRecord(
                    pch.pc.id,
                    pch.pc.createdOn,
                    pch.pc.review,
                    pch.pc.score,
                    pch.pc.parent.id
                )
                FROM postCommentChildHierarchy pch
                """, PostCommentRecord.class)
            .setParameter("commentId", 1L)
            .getResultList();

            assertEquals(3, postComments.size());
            assertEquals("Comment 1.1", postComments.get(0).review);
            assertEquals("Comment 1.2", postComments.get(1).review);
            assertEquals("Comment 1.2.1", postComments.get(2).review);
        });
    }


    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        @Column(length = 100)
        private String title;

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
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        @GeneratedValue(generator = "post_comment_seq", strategy = GenerationType.SEQUENCE)
        @SequenceGenerator(name = "post_comment_seq", allocationSize = 1)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "post_id")
        private Post post;
        
        @Column(name = "created_on")
        private LocalDateTime createdOn;

        @Column(length = 250)
        private String review;

        private int score;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "parent_id")
        private PostComment parent;

        @OneToMany(
            mappedBy = "parent",
            cascade = CascadeType.ALL,
            orphanRemoval = true
        )
        private List<PostComment> children = new ArrayList<>();

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

        public LocalDateTime getCreatedOn() {
            return createdOn;
        }

        public PostComment setCreatedOn(LocalDateTime createdOn) {
            this.createdOn = createdOn;
            return this;
        }

        public int getScore() {
            return score;
        }

        public PostComment setScore(int score) {
            this.score = score;
            return this;
        }

        public PostComment getParent() {
            return parent;
        }

        public PostComment setParent(PostComment parent) {
            this.parent = parent;
            return this;
        }

        public List<PostComment> getChildren() {
            return children;
        }

        public void setChildren(List<PostComment> children) {
            this.children = children;
        }

        public PostComment addChild(PostComment child) {
            children.add(child);
            child.setParent(this);
            return this;
        }
    }

    public record PostCommentRecord(
        Long id,
        LocalDateTime createdOn,
        String review,
        int score,
        Long parentId
    ) {
    }
}
