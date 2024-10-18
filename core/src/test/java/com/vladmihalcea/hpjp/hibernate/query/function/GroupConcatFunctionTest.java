package com.vladmihalcea.hpjp.hibernate.query.function;

import com.vladmihalcea.hpjp.util.AbstractMySQLIntegrationTest;
import jakarta.persistence.criteria.*;
import org.hibernate.query.Query;
import org.hibernate.transform.Transformers;
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
            List<Object[]> postSummaries = entityManager.createNativeQuery("""
                select p.id, p.title, group_concat(t.name)
                from Post p
                left join post_tag pt on p.id = pt.post_id
                left join tag t on t.id = pt.tag_id
                group by p.id, p.title
                """)
            .getResultList();

            assertEquals(1, postSummaries.size());
        });
    }

    @Test
    public void testGroupConcatJPQLQuery() {
        doInJPA(entityManager -> {
            List<PostSummaryDTO> postSummaries = entityManager.createQuery("""
                select
                   p.id as id,
                   p.title as title,
                   group_concat(t.name) as tags
                from Post p
                left join p.tags t
                group by p.id, p.title
                """)
            .unwrap(Query.class)
            .setTupleTransformer(Transformers.aliasToBean(PostSummaryDTO.class))
            .getResultList();

            assertEquals(1, postSummaries.size());
            LOGGER.info("Post tags: {}", postSummaries.get(0).getTags());
        });
    }

    @Test
    public void testGroupConcatCriteriaAPIQuery() {
        doInJPA(entityManager -> {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();

            CriteriaQuery<PostSummaryDTO> cq = cb.createQuery(
                PostSummaryDTO.class
            );

            Root<Post> post = cq.from(Post.class);
            Join tags = post.join("tags", JoinType.LEFT);
            cq.groupBy(post.get("id"), post.get("title"));

            cq.select(
                cb.construct(
                    PostSummaryDTO.class,
                    post.get("id"),
                    post.get("title"),
                    cb.function(
                        "group_concat",
                        String.class,
                        tags.get("name")
                    )
                )
            );

            List<PostSummaryDTO> postSummaries = entityManager.createQuery(cq)
                .getResultList();

            assertEquals(1, postSummaries.size());
            LOGGER.info("Post tags: {}", postSummaries.get(0).getTags());
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
