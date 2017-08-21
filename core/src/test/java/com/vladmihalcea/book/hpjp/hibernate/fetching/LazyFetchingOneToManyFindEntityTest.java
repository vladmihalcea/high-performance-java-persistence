package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.hibernate.identifier.Identifiable;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.junit.Test;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.hibernate.transform.BasicTransformerAdapter;
import org.hibernate.transform.DistinctResultTransformer;
import org.hibernate.transform.ResultTransformer;

import static org.hibernate.internal.CoreLogging.messageLogger;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class LazyFetchingOneToManyFindEntityTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                PostComment.class,
        };
    }


    @Override
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            int post_count = 10;

            for ( int i = 0; i < post_count; i++ ) {
                Post post = new Post();
                post.setTitle(
                    String.format( "High-Performance Java Persistence, part %d", i)
                );

                PostComment comment1 = new PostComment();
                comment1.setReview("Excellent!");
                PostComment comment2 = new PostComment();
                comment2.setReview("Good!");
                post.addComment(comment1);
                post.addComment(comment2);

                entityManager.persist(post);
            }
        });
    }

    @Test
    public void testFetchAndPaginate() {
        doInJPA(entityManager -> {
            String titlePattern = "High-Performance%";
            int maxResults = 3;
            List<Post> posts = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "left join fetch p.comments " +
                "where p.title like :title " +
                "order by p.id", Post.class)
            .setParameter("title", titlePattern)
            .setMaxResults(maxResults)
            .getResultList();
            assertEquals(maxResults, posts.size());
            assertEquals(2, posts.get(0).comments.size());
        });
    }

    @Test
    public void testFetchAndPaginateUsingDenseRank() {
        doInJPA(entityManager -> {
            String titlePattern = "High-Performance%";
            int maxResults = 3;
            List<Post> posts = entityManager.createNativeQuery(
                "select p_pc_r.* " +
                "from (   " +
                "    select *, dense_rank() OVER (ORDER BY post_id) rank " +
                "    from (   " +
                "        select p.*, pc.* " +
                "        from post p  " +
                "        left join post_comment pc on p.id = pc.post_id  " +
                "        where p.title like :title " +
                "        order by p.id " +
                "    ) p_pc " +
                ") p_pc_r " +
                "where p_pc_r.rank <= :rank", Post.class)
            .setParameter("title", titlePattern)
            .setParameter("rank", maxResults)
            .unwrap( NativeQuery.class )
            .setResultTransformer( DistinctPostResultTransformer.INSTANCE )
            .getResultList();
            assertEquals(maxResults, posts.size());
            //assertEquals(2, posts.get(0).comments.size());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post implements Identifiable<Long> {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
        private List<PostComment> comments = new ArrayList<>();

        public Post() {
        }

        public Post(Long id) {
            this.id = id;
        }

        public Post(String title) {
            this.title = title;
        }

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

        public void addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        @GeneratedValue
        private Long id;

        @ManyToOne
        private Post post;

        private String review;

        public PostComment() {
        }

        public PostComment(String review) {
            this.review = review;
        }

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

    public static class DistinctPostResultTransformer extends BasicTransformerAdapter {

        private static final DistinctPostResultTransformer INSTANCE  = new DistinctPostResultTransformer();

        @Override
        public List transformList(List list) {
            Map<Serializable, Identifiable> identifiableMap = new LinkedHashMap<>( list.size() );
            for ( Object entityArray : list ) {
                if ( Object[].class.isAssignableFrom( entityArray.getClass() ) ) {
                    Object entity = ((Object[]) entityArray)[0];
                    Identifiable identifiable = (Identifiable) entity;
                    if ( !identifiableMap.containsKey( identifiable.getId() ) ) {
                        identifiableMap.put( identifiable.getId(), identifiable );
                    }
                }
            }
            return new ArrayList<>( identifiableMap.values() );
        }


    }
}
