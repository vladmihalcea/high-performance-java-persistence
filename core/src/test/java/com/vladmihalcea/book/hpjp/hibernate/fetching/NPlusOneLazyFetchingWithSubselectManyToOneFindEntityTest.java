package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class NPlusOneLazyFetchingWithSubselectManyToOneFindEntityTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class,
            Tag.class
        };
    }

    @Test
    public void testNPlusOne() {

        String review = "Excellent!";

        doInJPA(entityManager -> {

            Tag tag1 = new Tag();
            tag1.id = 1L;
            tag1.name = "Java";
            entityManager.persist(tag1);

            Tag tag2 = new Tag();
            tag2.id = 2L;
            tag2.name = "MySQL";
            entityManager.persist(tag2);
            
            for (long i = 1; i < 4; i++) {
                Post post = new Post();
                post.setId(i);
                post.setTitle(String.format("Post nr. %d", i));
                entityManager.persist(post);
                post.getTags().add(tag1);
                post.getTags().add(tag2);

                PostComment comment = new PostComment();
                comment.setId(i);
                comment.setPost(post);
                post.getComments().add(comment);
                comment.setReview(review);
                entityManager.persist(comment);
            }
        });

        doInJPA(entityManager -> {
            LOGGER.info("N+1 query problem when using Subselect");
            List<PostComment> comments = entityManager.createQuery(
                "select pc " +
                "from PostComment pc " +
                "where pc.review = :review", PostComment.class)
            .setParameter("review", review)
            .getResultList();
            LOGGER.info("Loaded {} comments", comments.size());
            for(PostComment comment : comments) {
                LOGGER.info("The post title is '{}'", comment.getPost().getTitle());
                assertEquals(1, comment.getPost().getComments().size());
            }
        });

        doInJPA(entityManager -> {
            LOGGER.info("Collection loading when using Subselect");
            List<Post> posts = entityManager.createQuery(
                    "select p " +
                    "from Post p ", Post.class)
            .getResultList();

            LOGGER.info("Loaded {} comments", posts.size());
            for(Post post : posts) {
                LOGGER.info("The post comment count is '{}'", post.getComments().size());
                LOGGER.info("The post tag count is '{}'", post.getTags().size());
            }
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @OneToMany(mappedBy = "post")
        @Fetch(FetchMode.SUBSELECT)
        private List<PostComment> comments = new ArrayList<>();

        @ManyToMany
        @Fetch(FetchMode.SUBSELECT)
        private List<Tag> tags = new ArrayList<>();

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

        public List<PostComment> getComments() {
            return comments;
        }

        public List<Tag> getTags() {
            return tags;
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    @NamedEntityGraph(name = "PostComment.post", attributeNodes = {})
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
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

    @Entity(name = "Tag")
    @Table(name = "tag")
    public static class Tag {

        @Id
        private Long id;

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
