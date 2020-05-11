package com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.hibernate;

import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.Post;
import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.PostComment;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.Transformers;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.*;

import static com.vladmihalcea.book.hpjp.util.AbstractTest.stringValue;
import static com.vladmihalcea.book.hpjp.util.AbstractTest.longValue;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("unchecked")
public class HibernateDTOProjectionTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            Post post1 = new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence")
                .setCreatedBy("Vlad Mihalcea")
                .setCreatedOn(
                    LocalDateTime.of(2016, 11, 2, 12, 0, 0)
                )
                .setUpdatedBy("Vlad Mihalcea")
                .setUpdatedOn(
                    LocalDateTime.now()
                );

            entityManager.persist(post1);

            entityManager.persist(
                new PostComment()
                    .setId(1L)
                    .setPost(post1)
                    .setReview("Best book on JPA and Hibernate!")
            );

            entityManager.persist(
                new PostComment()
                    .setId(2L)
                    .setPost(post1)
                    .setReview("A must-read for every Java developer!")
            );

            Post post2 = new Post()
                .setId(2L)
                .setTitle("Hypersistence Optimizer")
                .setCreatedBy("Vlad Mihalcea")
                .setCreatedOn(
                    LocalDateTime.of(2019, 3, 19, 12, 0, 0)
                )
                .setUpdatedBy("Vlad Mihalcea")
                .setUpdatedOn(
                    LocalDateTime.now()
                );

            entityManager.persist(post2);

            entityManager.persist(
                new PostComment()
                    .setId(3L)
                    .setPost(post2)
                    .setReview("It's like pair programming with Vlad!")
            );
        });
    }

    @Test
    public void testJPQLResultTransformer() {
        doInJPA( entityManager -> {
            List<PostDTO> postDTOs = entityManager.createQuery("""
                select
                   p.id as id,
                   p.title as title
                from Post p
                order by p.id
                """)
            .unwrap(org.hibernate.query.Query.class)
            .setResultTransformer(Transformers.aliasToBean(PostDTO.class))
            .getResultList();

            assertEquals(2, postDTOs.size());

            PostDTO postDTO = postDTOs.get(0);
            assertEquals(1L, postDTO.getId().longValue());
            assertEquals("High-Performance Java Persistence", postDTO.getTitle());
        } );
    }

    @Test
    public void testNativeQueryResultTransformer() {
        doInJPA( entityManager -> {
            List<PostDTO> postDTOs = entityManager.createNativeQuery("""
                SELECT
                   p.id AS "id",
                   p.title AS "title"
                FROM post p
                ORDER BY p.id
                """)
            .unwrap(org.hibernate.query.NativeQuery.class)
            .setResultTransformer(Transformers.aliasToBean(PostDTO.class))
            .getResultList();

            assertEquals(2, postDTOs.size());

            PostDTO postDTO = postDTOs.get(0);
            assertEquals(1L, postDTO.getId().longValue());
            assertEquals("High-Performance Java Persistence", postDTO.getTitle());
        } );
    }

    @Test
    public void testParentChildResultTransformer() {
        doInJPA( entityManager -> {
            List<PostDTO> postDTOs = entityManager.createNativeQuery("""
                SELECT
                   p.id AS "p.id", p.title AS "p.title",
                   pc.id AS "pc.id", pc.review AS "pc.review"
                FROM post p
                JOIN post_comment pc ON p.id = pc.post_id
                ORDER BY pc.id
                """)
            .unwrap(org.hibernate.query.NativeQuery.class)
            .setResultTransformer(new PostDTOResultTransformer())
            .getResultList();

            assertEquals(2, postDTOs.size());
            assertEquals(2, postDTOs.get(0).getComments().size());
            assertEquals(1, postDTOs.get(1).getComments().size());

            PostDTO post1DTO = postDTOs.get(0);

            assertEquals(1L, post1DTO.getId().longValue());
            assertEquals(2, post1DTO.getComments().size());
            assertEquals(1L, post1DTO.getComments().get(0).getId().longValue());
            assertEquals(2L, post1DTO.getComments().get(1).getId().longValue());

            PostDTO post2DTO = postDTOs.get(1);

            assertEquals(2L, post2DTO.getId().longValue());
            assertEquals(1, post2DTO.getComments().size());
            assertEquals(3L, post2DTO.getComments().get(0).getId().longValue());
        } );
    }

    public static class PostDTO {

        public static final String ID_ALIAS = "p.id";
        public static final String TITLE_ALIAS = "p.title";

        private Long id;

        private String title;

        private List<PostCommentDTO> comments = new ArrayList<>();

        public PostDTO() {
        }

        public PostDTO(Long id, String title) {
            this.id = id;
            this.title = title;
        }

        public PostDTO(Object[] tuples, Map<String, Integer> aliasToIndexMap) {
            this.id = longValue(tuples[aliasToIndexMap.get(ID_ALIAS)]);
            this.title = stringValue(tuples[aliasToIndexMap.get(TITLE_ALIAS)]);
        }

        public Long getId() {
            return id;
        }

        public void setId(Number id) {
            this.id = id.longValue();
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public List<PostCommentDTO> getComments() {
            return comments;
        }
    }

    public static class PostCommentDTO {
        public static final String ID_ALIAS = "pc.id";
        public static final String REVIEW_ALIAS = "pc.review";

        private Long id;

        private String review;

        public PostCommentDTO(Long id, String review) {
            this.id = id;
            this.review = review;
        }

        public PostCommentDTO(Object[] tuples, Map<String, Integer> aliasToIndexMap) {
            this.id = longValue(tuples[aliasToIndexMap.get(ID_ALIAS)]);
            this.review = stringValue(tuples[aliasToIndexMap.get(REVIEW_ALIAS)]);
        }

        public Long getId() {
            return id;
        }

        public void setId(Number id) {
            this.id = id.longValue();
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
        }
    }

    public static class PostDTOResultTransformer implements ResultTransformer {

        private Map<Long, PostDTO> postDTOMap = new LinkedHashMap<>();

        @Override
        public Object transformTuple(Object[] tuple, String[] aliases) {
            Map<String, Integer> aliasToIndexMap = aliasToIndexMap(aliases);
            Long postId = longValue(tuple[aliasToIndexMap.get(PostDTO.ID_ALIAS)]);

            PostDTO postDTO = postDTOMap.computeIfAbsent(postId, id -> new PostDTO(tuple, aliasToIndexMap));
            postDTO.getComments().add(new PostCommentDTO(tuple, aliasToIndexMap));

            return postDTO;
        }

        @Override
        public List transformList(List collection) {
            return new ArrayList<>(postDTOMap.values());
        }
    }

    public static Map<String, Integer> aliasToIndexMap(String[] aliases) {
        Map<String, Integer> aliasToIndexMap = new LinkedHashMap<>();
        for (int i = 0; i < aliases.length; i++) {
            aliasToIndexMap.put(aliases[i], i);
        }
        return aliasToIndexMap;
    }
}
