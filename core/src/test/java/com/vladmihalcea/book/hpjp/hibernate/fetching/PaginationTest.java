package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.hibernate.identifier.Identifiable;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.BasicTransformerAdapter;
import org.junit.Test;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PaginationTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                PostComment.class,
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            LocalDateTime timestamp = LocalDateTime.of(
                2018, 10, 9, 12, 0, 0, 0
            );

            int commentsSize = 5;

            LongStream.range(1, 50).forEach(postId -> {
                Post post = new Post();
                post.setId(postId);
                post.setTitle(String.format("Post nr. %d", postId));
                post.setCreatedOn(
                     Timestamp.valueOf(timestamp.plusMinutes(postId))
                );

                LongStream.range(1, commentsSize + 1).forEach(commentOffset -> {
                    PostComment comment = new PostComment();

                    long commentId = ((postId - 1) * commentsSize) + commentOffset;
                    comment.setId(commentId);
                    comment.setReview(
                        String.format("Comment nr. %d", comment.getId())
                    );
                    comment.setCreatedOn(
                        Timestamp.valueOf(timestamp.plusMinutes(commentId))
                    );

                    post.addComment(comment);

                });
                entityManager.persist(post);
            });
        });
    }

    @Test
    public void testLimit() {
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "order by p.createdOn ")
            .setMaxResults(10)
            .getResultList();

            assertEquals(10, posts.size());
            assertEquals("Post nr. 1", posts.get(0).getTitle());
            assertEquals("Post nr. 10", posts.get(9).getTitle());
        });
    }

    @Test
    public void testOffset() {
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "order by p.createdOn ")
            .setFirstResult(10)
            .setMaxResults(10)
            .getResultList();

            assertEquals(10, posts.size());
            assertEquals("Post nr. 11", posts.get(0).getTitle());
            assertEquals("Post nr. 20", posts.get(9).getTitle());
        });
    }

    @Test
    public void testOffsetNative() {
        doInJPA(entityManager -> {
            List<Tuple> posts = entityManager.createNativeQuery(
                "select p.id as id, p.title as title " +
                "from post p " +
                "order by p.created_on", Tuple.class)
            .setFirstResult(10)
            .setMaxResults(10)
            .getResultList();

            assertEquals(10, posts.size());
            assertEquals("Post nr. 11", posts.get(0).get("title"));
            assertEquals("Post nr. 20", posts.get(9).get("title"));
        });
    }

    @Test
    public void testDTO() {
        doInJPA(entityManager -> {
            List<PostCommentSummary> summaries = entityManager.createQuery(
                "select new " +
                "   com.vladmihalcea.book.hpjp.hibernate.fetching.PostCommentSummary( " +
                "       p.id, p.title, c.review " +
                "   ) " +
                "from PostComment c " +
                "join c.post p " +
                "order by c.createdOn")
            .setMaxResults(10)
            .getResultList();

            assertEquals(10, summaries.size());
            assertEquals("Post nr. 1", summaries.get(0).getTitle());
            assertEquals("Comment nr. 1", summaries.get(0).getReview());

            assertEquals("Post nr. 2", summaries.get(9).getTitle());
            assertEquals("Comment nr. 10", summaries.get(9).getReview());
        });
    }

    @Test
    public void testFetchAndPaginate() {
        doInJPA(entityManager -> {

            List<Post> posts = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "left join fetch p.comments " +
                "order by p.createdOn", Post.class)
            .setMaxResults(10)
            .getResultList();

            assertEquals(10, posts.size());
        });
    }

    @Test
    public void testFetchAndPaginateUsingDenseRank() {
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createNativeQuery(
                "select * " +
                "from (   " +
                "    select *, dense_rank() OVER (ORDER BY post_id) rank " +
                "    from (   " +
                "        select p.*, pc.* " +
                "        from post p  " +
                "        left join post_comment pc on p.id = pc.post_id  " +
                "        order by p.created_on " +
                "    ) p_pc " +
                ") p_pc_r " +
                "where p_pc_r.rank <= :rank", Post.class)
            .setParameter("rank", 10)
            .unwrap(NativeQuery.class)
            .addEntity("p", Post.class)
            .addEntity("pc", PostComment.class)
            .setResultTransformer(DistinctPostResultTransformer.INSTANCE)
            .getResultList();

            assertEquals(10, posts.size());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post implements Identifiable<Long> {

        @Id
        private Long id;

        private String title;

        @Column(name = "created_on")
        private Timestamp createdOn;

        @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
        private List<PostComment> comments = new ArrayList<>();

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

        public Date getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(Timestamp createdOn) {
            this.createdOn = createdOn;
        }

        public List<PostComment> getComments() {
            return comments;
        }

        public void setComments(List<PostComment> comments) {
            this.comments = comments;
        }

        public void addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment implements Identifiable<Long> {

        @Id
        private Long id;

        @ManyToOne
        private Post post;

        private String review;

        @Column(name = "created_on")
        private Timestamp createdOn;

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

        public Date getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(Timestamp createdOn) {
            this.createdOn = createdOn;
        }
    }

    public static class DistinctPostResultTransformer extends BasicTransformerAdapter {

        private static final DistinctPostResultTransformer INSTANCE  = new DistinctPostResultTransformer();

        @Override
        public List transformList(List list) {
            Map<Serializable, Identifiable> identifiableMap = new LinkedHashMap<>( list.size() );
            for ( Object entityArray : list ) {
                if ( Object[].class.isAssignableFrom( entityArray.getClass() ) ) {
                    Post post = null;
                    PostComment comment = null;

                    Object[] tuples = (Object[]) entityArray;

                    for ( Object tuple : tuples ) {
                        if(tuple instanceof Post) {
                            post = (Post) tuple;
                        }
                        else if(tuple instanceof PostComment) {
                            comment = (PostComment) tuple;
                        }
                        else {
                            throw new UnsupportedOperationException(
                                    "Tuple " + tuple.getClass() + " is not supported!"
                            );
                        }
                    }
                    Objects.requireNonNull(post);
                    Objects.requireNonNull(comment);

                    if ( !identifiableMap.containsKey( post.getId() ) ) {
                        identifiableMap.put( post.getId(), post );
                        post.setComments( new ArrayList<>() );
                    }
                    post.addComment( comment );
                }
            }
            return new ArrayList<>( identifiableMap.values() );
        }
    }
}
