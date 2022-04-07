package com.vladmihalcea.book.hpjp.hibernate.query.function;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class GroupConcatFunctionTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                Tag.class,
        };
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {

            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");

            Tag jdbc = new Tag();
            jdbc.setName("JDBC");
            post.getTags().add(jdbc);

            Tag hibernate = new Tag();
            hibernate.setName("Hibernate");
            post.getTags().add(hibernate);

            Tag jooq = new Tag();
            jooq.setName("jOOQ");
            post.getTags().add(jooq);

            entityManager.persist(post);
        });
    }

    @Test
    public void testGroupConcatNaiveQuery() {
        doInJPA(entityManager -> {
            List<Object[]> postSummaries = entityManager.createNativeQuery(
                    "select p.id, p.title, group_concat(t.name) " +
                            "from Post p " +
                            "left join post_tag pt on p.id = pt.post_id " +
                            "left join tag t on t.id = pt.tag_id " +
                            "group by p.id, p.title")
                    .getResultList();

            assertEquals(1, postSummaries.size());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @ManyToMany(cascade = CascadeType.PERSIST)
        @JoinTable(name = "post_tag",
                joinColumns = @JoinColumn(name = "post_id"),
                inverseJoinColumns = @JoinColumn(name = "tag_id")
        )
        private Set<Tag> tags = new HashSet<>();

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

        public Set<Tag> getTags() {
            return tags;
        }
    }

    @Entity(name = "Tag")
    @Table(name = "tag")
    public static class Tag {

        @Id
        @GeneratedValue
        private Long id;

        private String name;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}
