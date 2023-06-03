package com.vladmihalcea.book.hpjp.hibernate.flushing;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class JPAAutoFlushTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Post.class,
            PostDetails.class,
            Tag.class
        };
    }
    @Override
    protected Database database() {
        return Database.MYSQL;
    }

    @Override
    protected boolean nativeHibernateSessionFactoryBootstrap() {
        return false;
    }

    @Test
    public void testFlushAutoJPQL() {
        doInJPA(entityManager -> {
            assertEquals(
                    0,
                    ((Number) entityManager.createQuery("""
                        select count(p)
                        from Post p
                        """)
                            .getSingleResult()
                    ).intValue()
            );

            entityManager.persist(
                    new Post()
                            .setTitle("High-Performance Java Persistence")
            );

            int tagCount = ((Number) entityManager.createQuery("""
                select count(t)
                from Tag t
                """)
                    .getSingleResult()).intValue();

            int postCount = 0;


            try{
                postCount = ((Number) entityManager.createQuery("""
                    select count(p)
                    from Post p
                """)
                        .getSingleResult()).intValue();
                // do some logic with post count
                // here is exception happen
                throw new RuntimeException("Error while process transaction");
            }catch (Exception e){
                // the problem as show here that postCount variable is one but in DB after exception it is 0.
                // it's seems like inconsistent or synchronize issue
                // audit transaction and use the postCount variable
                LOGGER.info("postCount variable value {}",postCount);
                LOGGER.info("post count value in DB {}",((Number) entityManager.createQuery("""
                                                            select count(p)
                                                            from Post p
                                                            """)
                        .getSingleResult()).intValue()
                );
            }finally {
                LOGGER.info("postCount is {}",postCount);
                // the problem can happen here also
            }
            // maybe we need to return postCount variable to caller to use it in other logic
            // return postCount

            assertEquals(1, postCount);
        });
    }

    @Test
    public void testFlushAutoJPQLTableSpaceOverlap() {
        doInJPA(entityManager -> {
            assertEquals(
                0,
                ((Number)
                    entityManager.createQuery("""
                        select count(p)
                        from Post p
                        """)
                    .getSingleResult()
                ).intValue()
            );

            entityManager.persist(
                new Post()
                .setTitle("High-Performance Java Persistence")
            );

            List<PostDetails> details = entityManager.createQuery("""
                select pd
                from PostDetails pd
                join fetch pd.post
                """)
            .getResultList();

            int postCount = ((Number) entityManager.createQuery("""
                select count(p)
                from Post p
                """)
            .getSingleResult()).intValue();

            assertEquals(1, postCount);
        });
    }

    @Test
    public void testFlushAutoNativeSQL() {
        doInJPA(entityManager -> {
            assertEquals(
                0,
                ((Number)
                    entityManager.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM Post
                        """)
                    .getSingleResult()
                ).intValue()
            );

            entityManager.persist(
                new Post()
                .setTitle("High-Performance Java Persistence")
            );

            int tagCount = ((Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM tag
                """)
            .getSingleResult()).intValue();

            int postCount = ((Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM post
                """)
            .getSingleResult()).intValue();

            assertEquals(1, postCount);
        });
    }

    @Test
    public void testFlushAutoDoWorkNativeSQL() {
        doInJPA(entityManager -> {
            assertEquals(
                0,
                ((Number)
                    entityManager.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM Post
                        """)
                    .getSingleResult()
                ).intValue()
            );

            entityManager.persist(
                new Post()
                .setTitle("High-Performance Java Persistence")
            );

            int postCount = (entityManager.unwrap(Session.class).doReturningWork(connection ->
                selectColumn(
                    connection,
                    """
                    SELECT COUNT(*)
                    FROM post
                    """,
                    Number.class
                )
            )).intValue();

            //doWork does not trigger a flush.
            assertEquals(0, postCount);
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        @OneToOne(
            mappedBy = "post",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
        )
        private PostDetails details;

        @ManyToMany
        @JoinTable(name = "post_tag",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
        )
        private Set<Tag> tags = new HashSet<>();

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

    @Entity(name = "PostDetails")
    @Table(name = "post_details")
    public static class PostDetails {

        @Id
        private Long id;

        @Column(name = "created_on")
        private Date createdOn;

        @Column(name = "created_by")
        private String createdBy;

        @OneToOne(fetch = FetchType.LAZY)
        @MapsId
        private Post post;

        public PostDetails() {
            createdOn = new Date();
        }
    }

    @Entity(name = "Tag")
    @Table(name = "tag")
    public static class Tag {

        @Id
        @GeneratedValue
        private Long id;

        @NaturalId
        private String name;
    }
}
